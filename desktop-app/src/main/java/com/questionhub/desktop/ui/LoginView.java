package com.questionhub.desktop.ui;

import com.questionhub.desktop.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class LoginView {
    private final StackPane root = new StackPane();

    public LoginView(AuthService auth, Runnable onSuccess) {
        root.getStyleClass().add("login-root");
        root.setPadding(new Insets(42));

        VBox card = new VBox(14);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(470);
        card.setAlignment(Pos.CENTER_LEFT);

        Label mark = new Label("QH");
        mark.getStyleClass().add("brand-mark");

        Label eyebrow = new Label("PERSONAL STUDY ARCHIVE");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("我的学习问题库");
        title.getStyleClass().add("login-title");

        Label subtitle = new Label("把每天遇到的问题留下来，整理成真正属于自己的长期知识库。");
        subtitle.getStyleClass().add("muted");
        subtitle.setWrapText(true);

        Label value = new Label("长期积累  ·  本地保存  ·  随时复习");
        value.getStyleClass().add("login-value");

        Separator separator = new Separator();

        Label label = new Label("访问口令");
        label.getStyleClass().add("field-label");

        PasswordField password = new PasswordField();
        password.setPromptText("输入口令后进入学习资料库");
        password.getStyleClass().addAll("login-password", "flat-field");

        Label lock = new Label("◆");
        lock.getStyleClass().add("input-leading-icon");
        HBox passwordShell = new HBox(10, lock, password);
        passwordShell.setAlignment(Pos.CENTER_LEFT);
        passwordShell.getStyleClass().add("input-shell");
        HBox.setHgrow(password, Priority.ALWAYS);

        Label error = new Label();
        error.getStyleClass().add("error-text");
        error.setMinHeight(18);

        Button enter = new Button("进入学习资料库");
        enter.getStyleClass().add("primary-button");
        enter.setMaxWidth(Double.MAX_VALUE);

        Label shortcut = new Label("提示：输入口令后按 Enter 也可以进入");
        shortcut.getStyleClass().add("small-muted");

        HBox privacy = new HBox(8);
        privacy.getStyleClass().add("privacy-note");
        privacy.setAlignment(Pos.CENTER_LEFT);
        Label shield = new Label("●");
        shield.getStyleClass().add("privacy-dot");
        Label note = new Label("所有学习资料仅保存在本机，应用启动时会自动保留最近备份。");
        note.getStyleClass().add("small-muted");
        note.setWrapText(true);
        privacy.getChildren().addAll(shield, note);

        Runnable submit = () -> {
            if (auth.login(password.getText())) {
                error.setText("");
                onSuccess.run();
            } else {
                error.setText("口令不正确，请重新输入");
                password.clear();
                password.requestFocus();
            }
        };

        enter.setOnAction(e -> submit.run());
        password.setOnAction(e -> submit.run());

        card.getChildren().addAll(
                mark, eyebrow, title, subtitle, value,
                separator, label, passwordShell, error, enter, shortcut, privacy
        );
        root.getChildren().add(card);
    }

    public Parent root() { return root; }
}
