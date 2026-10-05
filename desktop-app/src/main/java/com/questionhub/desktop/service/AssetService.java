package com.questionhub.desktop.service;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AssetService {
    public static final Pattern IMAGE_TOKEN = Pattern.compile(
            "!\\[截图\\]\\(asset:([A-Za-z0-9._-]+)\\)\\{width=(\\d{1,3})\\}");

    private final Path assetsDir;

    public AssetService(Path dataDir) {
        this.assetsDir = dataDir.resolve("assets");
    }

    public Path assetsDir() {
        return assetsDir;
    }

    public String saveImage(Image image) throws IOException {
        if (image == null) throw new IllegalArgumentException("剪贴板中没有图片");
        Files.createDirectories(assetsDir);

        String name = "img-" + UUID.randomUUID() + ".png";
        Path target = assetsDir.resolve(name);

        BufferedImage buffered = SwingFXUtils.fromFXImage(image, null);
        if (buffered == null || !ImageIO.write(buffered, "png", target.toFile())) {
            throw new IOException("截图保存失败");
        }
        return name;
    }

    public Path resolve(String fileName) {
        String safe = safeFileName(fileName);
        return assetsDir.resolve(safe).normalize();
    }

    public static String imageToken(String fileName, int width) {
        int safeWidth = Math.max(20, Math.min(100, width));
        return "![截图](asset:" + safeFileName(fileName) + "){width=" + safeWidth + "}";
    }

    public static Set<String> referencedAssets(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return out;

        Matcher m = IMAGE_TOKEN.matcher(text);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    public static String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("图片文件名无效");
        String name = Path.of(fileName).getFileName().toString();
        if (!name.equals(fileName) || !name.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("图片文件名无效");
        }
        return name;
    }
}
