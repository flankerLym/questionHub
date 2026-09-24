package com.questionhub.desktop;

import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppPaths {
    private static final String APP_NAME = "QuestionHub";

    private AppPaths() {}

    public static Path configDir() {
        return localAppData().resolve(APP_NAME).resolve("config");
    }

    public static Path configFile() {
        return configDir().resolve("data-dir.txt");
    }

    public static Path defaultDataDir() {
        return localAppData().resolve(APP_NAME).resolve("data");
    }

    public static Path resolveDataDir(Window owner) throws IOException {
        Path config = configFile();
        if (Files.exists(config)) {
            String raw = Files.readString(config, StandardCharsets.UTF_8).trim();
            if (!raw.isEmpty()) {
                Path saved = Path.of(raw).toAbsolutePath().normalize();
                Files.createDirectories(saved);
                return saved;
            }
        }

        Path fallback = defaultDataDir();
        Files.createDirectories(fallback);

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择 QuestionHub 数据存储目录");
        chooser.setInitialDirectory(fallback.toFile());
        var selected = chooser.showDialog(owner);
        Path dataDir = selected == null ? fallback : selected.toPath().toAbsolutePath().normalize();
        saveDataDir(dataDir);
        return dataDir;
    }

    public static void saveDataDir(Path dataDir) throws IOException {
        Files.createDirectories(configDir());
        Files.createDirectories(dataDir);
        Files.writeString(configFile(), dataDir.toAbsolutePath().normalize().toString(), StandardCharsets.UTF_8);
    }

    private static Path localAppData() {
        String v = System.getenv("LOCALAPPDATA");
        if (v != null && !v.isBlank()) return Path.of(v);
        return Path.of(System.getProperty("user.home"), ".questionhub");
    }
}
