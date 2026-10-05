package com.questionhub.desktop.service;

import com.questionhub.desktop.db.ArchiveRepository;
import com.questionhub.desktop.model.ArchiveData;
import com.questionhub.desktop.model.QaItem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ArchivePackageService {
    public static final int PACKAGE_VERSION = 1;

    private static final String MANIFEST = "manifest.properties";
    private static final String ARCHIVE = "archive.json";
    private static final String ASSETS = "assets/";
    private static final long MAX_PACKAGE_SIZE = 2L * 1024 * 1024 * 1024;
    private static final long MAX_UNPACKED_SIZE = 4L * 1024 * 1024 * 1024;

    private final ArchiveRepository repo;
    private final ArchiveJsonService json;
    private final Path dataDir;
    private final AssetService assets;

    public ArchivePackageService(ArchiveRepository repo, Path dataDir) {
        this.repo = repo;
        this.json = new ArchiveJsonService(repo);
        this.dataDir = dataDir;
        this.assets = new AssetService(dataDir);
    }

    public void exportTo(Path target) throws Exception {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);

        Path temp = Files.createTempDirectory("questionhub-export-");
        try {
            Path archiveFile = temp.resolve(ARCHIVE);
            json.exportTo(archiveFile);

            try (OutputStream raw = Files.newOutputStream(target);
                 ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(raw), StandardCharsets.UTF_8)) {

                putText(zip, MANIFEST,
                        "format=QuestionHubPackage\n"
                                + "packageVersion=" + PACKAGE_VERSION + "\n"
                                + "exportedAt=" + Instant.now() + "\n");

                putFile(zip, archiveFile, ARCHIVE);

                Path assetsDir = assets.assetsDir();
                if (Files.isDirectory(assetsDir)) {
                    try (var stream = Files.walk(assetsDir)) {
                        stream.filter(Files::isRegularFile)
                                .sorted()
                                .forEach(path -> {
                                    try {
                                        Path relative = assetsDir.relativize(path);
                                        String entry = ASSETS + relative.toString().replace('\\', '/');
                                        putFile(zip, path, entry);
                                    } catch (IOException e) {
                                        throw new PackageIoException(e);
                                    }
                                });
                    } catch (PackageIoException e) {
                        throw e.io;
                    }
                }
            }
        } finally {
            deleteTree(temp);
        }
    }

    public void importAndReplace(Path packageFile) throws Exception {
        if (!Files.isRegularFile(packageFile)) throw new IllegalArgumentException("数据包不存在");
        if (Files.size(packageFile) > MAX_PACKAGE_SIZE) throw new IllegalArgumentException("数据包过大");

        Path temp = Files.createTempDirectory("questionhub-import-");
        try {
            extract(packageFile, temp);

            Path manifest = temp.resolve(MANIFEST);
            Path archiveFile = temp.resolve(ARCHIVE);
            if (!Files.isRegularFile(manifest) || !Files.isRegularFile(archiveFile)) {
                throw new IllegalArgumentException("不是有效的 QuestionHub 数据包");
            }

            String manifestText = Files.readString(manifest, StandardCharsets.UTF_8);
            if (!manifestText.contains("format=QuestionHubPackage")
                    || !manifestText.contains("packageVersion=" + PACKAGE_VERSION)) {
                throw new IllegalArgumentException("不支持的数据包版本");
            }

            ArchiveData data = json.read(archiveFile);
            Path incomingAssets = temp.resolve("assets");
            validateReferencedAssets(data, incomingAssets);

            // 先复制图片，再替换数据库。即使数据库导入失败，最多只留下未引用的图片，
            // 不会造成当前资料中的图片丢失。
            if (Files.isDirectory(incomingAssets)) {
                Files.createDirectories(assets.assetsDir());
                try (var stream = Files.walk(incomingAssets)) {
                    stream.filter(Files::isRegularFile).forEach(path -> {
                        try {
                            String name = AssetService.safeFileName(path.getFileName().toString());
                            Files.copy(path, assets.assetsDir().resolve(name), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new PackageIoException(e);
                        }
                    });
                } catch (PackageIoException e) {
                    throw e.io;
                }
            }

            repo.replaceAll(data.folders(), data.qaItems());
        } finally {
            deleteTree(temp);
        }
    }

    private void validateReferencedAssets(ArchiveData data, Path incomingAssets) {
        Set<String> referenced = new HashSet<>();
        for (QaItem item : data.qaItems()) {
            referenced.addAll(AssetService.referencedAssets(item.answer()));
        }

        for (String name : referenced) {
            Path file = incomingAssets.resolve(AssetService.safeFileName(name)).normalize();
            if (!file.startsWith(incomingAssets.normalize()) || !Files.isRegularFile(file)) {
                throw new IllegalArgumentException("数据包缺少截图文件：" + name);
            }
        }
    }

    private void extract(Path packageFile, Path temp) throws IOException {
        long total = 0;
        int entries = 0;
        byte[] buffer = new byte[64 * 1024];

        try (InputStream raw = Files.newInputStream(packageFile);
             ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > 20_000) throw new IllegalArgumentException("数据包文件数量异常");

                Path out = temp.resolve(entry.getName()).normalize();
                if (!out.startsWith(temp)) throw new IllegalArgumentException("数据包路径非法");

                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                    continue;
                }

                Files.createDirectories(out.getParent());
                try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(out))) {
                    int n;
                    while ((n = zip.read(buffer)) >= 0) {
                        total += n;
                        if (total > MAX_UNPACKED_SIZE) throw new IllegalArgumentException("数据包解压后过大");
                        os.write(buffer, 0, n);
                    }
                }
            }
        }
    }

    private static void putText(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void putFile(ZipOutputStream zip, Path file, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Files.copy(file, zip);
        zip.closeEntry();
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }

    private static final class PackageIoException extends RuntimeException {
        private final IOException io;
        private PackageIoException(IOException io) {
            super(io);
            this.io = io;
        }
    }
}
