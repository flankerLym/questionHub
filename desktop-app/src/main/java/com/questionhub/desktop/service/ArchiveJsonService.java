package com.questionhub.desktop.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.questionhub.desktop.db.ArchiveRepository;
import com.questionhub.desktop.model.ArchiveData;
import com.questionhub.desktop.model.Folder;
import com.questionhub.desktop.model.QaItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ArchiveJsonService {
    public static final int ARCHIVE_VERSION = 1;
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final ArchiveRepository repo;

    public ArchiveJsonService(ArchiveRepository repo) { this.repo = repo; }

    public void exportTo(Path file) throws Exception {
        ArchiveData data = new ArchiveData(ARCHIVE_VERSION, System.currentTimeMillis(), repo.folders(), repo.allQuestions());
        Files.createDirectories(file.toAbsolutePath().getParent());
        mapper.writeValue(file.toFile(), data);
    }

    public ArchiveData read(Path file) throws IOException {
        if (!Files.exists(file)) throw new IllegalArgumentException("归档文件不存在");
        if (Files.size(file) > MAX_FILE_SIZE) throw new IllegalArgumentException("归档文件不能超过 20MB");
        ArchiveData data;
        try { data = mapper.readValue(file.toFile(), ArchiveData.class); }
        catch (Exception e) { throw new IllegalArgumentException("JSON 文件解析失败", e); }
        validate(data);
        return data;
    }

    public void importAndReplace(Path file) throws Exception {
        ArchiveData data = read(file);
        repo.replaceAll(data.folders(), data.qaItems());
    }

    public static void validate(ArchiveData data) {
        if (data == null) throw new IllegalArgumentException("归档文件格式错误");
        if (data.version() != ARCHIVE_VERSION) throw new IllegalArgumentException("不支持的归档版本：" + data.version());
        if (data.folders() == null || data.qaItems() == null) throw new IllegalArgumentException("归档缺少 folders 或 qaItems 数组");

        Set<String> folderIds = new HashSet<>();
        Set<String> folderNames = new HashSet<>();
        for (Folder f : data.folders()) {
            if (f == null || blank(f.id()) || blank(f.name()) || f.name().trim().length() > 60)
                throw new IllegalArgumentException("存在无效文件夹数据");
            if (!folderIds.add(f.id())) throw new IllegalArgumentException("文件夹 ID 重复");
            if (!folderNames.add(f.name().trim().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("归档中存在同名文件夹");
        }

        Set<String> qaIds = new HashSet<>();
        for (QaItem q : data.qaItems()) {
            if (q == null || blank(q.id()) || blank(q.folderId()) || q.question() == null || q.answer() == null)
                throw new IllegalArgumentException("存在无效问答数据");
            if (blank(q.question()) || q.question().trim().length() > 500) throw new IllegalArgumentException("归档中存在无效问题");
            if (q.answer().length() > 50_000) throw new IllegalArgumentException("归档中存在过长答案");
            if (!qaIds.add(q.id())) throw new IllegalArgumentException("问题 ID 重复");
            if (!folderIds.contains(q.folderId())) throw new IllegalArgumentException("存在问题引用了不存在的文件夹");
        }
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }
}
