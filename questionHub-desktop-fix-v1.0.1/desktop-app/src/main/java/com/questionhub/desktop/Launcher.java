package com.questionhub.desktop;

import javafx.application.Application;

import javax.swing.JOptionPane;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * Native launcher entry point.
 *
 * Do not make this class extend javafx.application.Application. When JavaFX is
 * shipped as classpath jars (as jpackage does here), using an Application
 * subclass directly as the native main class makes the JDK launcher look for
 * JavaFX modules in the runtime image and the process exits before the UI opens.
 */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        Path logFile = initLog();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            System.err.println("Uncaught exception on thread: " + thread.getName());
            error.printStackTrace();
        });

        try {
            System.out.println("\n=== QuestionHub startup " + LocalDateTime.now() + " ===");
            System.out.println("Java: " + System.getProperty("java.version"));
            System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            Application.launch(QuestionHubApp.class, args);
        } catch (Throwable e) {
            e.printStackTrace();
            try {
                JOptionPane.showMessageDialog(
                        null,
                        "QuestionHub 启动失败。\n\n启动日志：\n" + logFile,
                        "QuestionHub",
                        JOptionPane.ERROR_MESSAGE
                );
            } catch (Throwable ignored) {
                // If even the fallback dialog cannot be displayed, the log still remains.
            }
        }
    }

    private static Path initLog() {
        try {
            String local = System.getenv("LOCALAPPDATA");
            Path root = local == null || local.isBlank()
                    ? Path.of(System.getProperty("user.home"), ".questionhub")
                    : Path.of(local, "QuestionHub");
            Path logDir = root.resolve("logs");
            Files.createDirectories(logDir);
            Path log = logDir.resolve("startup.log");
            PrintStream stream = new PrintStream(
                    Files.newOutputStream(log, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                    true,
                    StandardCharsets.UTF_8
            );
            System.setOut(stream);
            System.setErr(stream);
            return log;
        } catch (Exception e) {
            return Path.of(System.getProperty("user.home"), "questionhub-startup.log");
        }
    }
}
