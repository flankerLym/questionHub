package com.questionhub.desktop.ui;

import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class AppIcons {
    private static final Image ICON = load();

    private AppIcons() {}

    public static void apply(Stage stage) {
        if (stage != null && ICON != null) stage.getIcons().setAll(ICON);
    }

    public static void apply(Dialog<?> dialog) {
        dialog.setOnShowing(e -> {
            Window w = dialog.getDialogPane().getScene() == null ? null : dialog.getDialogPane().getScene().getWindow();
            if (w instanceof Stage stage) apply(stage);
        });
    }

    private static Image load() {
        try {
            var in = AppIcons.class.getResourceAsStream("/app-icon.png");
            return in == null ? null : new Image(in);
        } catch (Exception e) {
            return null;
        }
    }
}
