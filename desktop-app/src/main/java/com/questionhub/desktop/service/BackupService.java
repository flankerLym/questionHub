package com.questionhub.desktop.service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public final class BackupService {
    private BackupService() {}

    public static void backupIfNeeded(Path dbFile, Path dataDir) {
        try {
            if (!Files.exists(dbFile) || Files.size(dbFile) == 0) return;
            Path dir = dataDir.resolve("backup");
            Files.createDirectories(dir);
            LocalDate today = LocalDate.now();
            try (var stream = Files.list(dir)) {
                boolean doneToday = stream.filter(Files::isRegularFile).anyMatch(p -> {
                    try {
                        LocalDate d = Instant.ofEpochMilli(Files.getLastModifiedTime(p).toMillis()).atZone(ZoneId.systemDefault()).toLocalDate();
                        return d.equals(today);
                    } catch (IOException e) { return false; }
                });
                if (doneToday) return;
            }
            String name = "questionhub-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now()) + ".db";
            Files.copy(dbFile, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            try (var stream = Files.list(dir)) {
                List<Path> backups = stream.filter(Files::isRegularFile)
                        .sorted(Comparator.comparingLong(BackupService::mtime).reversed()).toList();
                for (int i = 10; i < backups.size(); i++) Files.deleteIfExists(backups.get(i));
            }
        } catch (Exception ignored) {
            // 备份失败不阻断应用启动。
        }
    }

    private static long mtime(Path p) {
        try { return Files.getLastModifiedTime(p).toMillis(); }
        catch (IOException e) { return 0; }
    }
}
