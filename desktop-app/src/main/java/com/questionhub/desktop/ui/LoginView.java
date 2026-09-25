package com.questionhub.desktop.ui;

import com.questionhub.desktop.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class LoginView {
    private final VBox root = new VBox(14);

    public LoginView(AuthService auth, Runnable onSuccess) {
        root.getStyleClass().add("login-root");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        VBox card = new VBox(12);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(430);

        Label mark = new Label("QA"); mark.getStyleClass().add("brand-mark");
        Label eyebrow = new Label("PERSONAL ARCHIVE"); eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("问题归档系统"); title.getStyleClass().add("login-title");
        Label subtitle = new Label("把零散的问题与答案，整理成自己的可检索知识档案。"); subtitle.getStyleClass().add("muted"); subtitle.setWrapText(true);
        Label label = new Label("访问口令"); label.getStyleClass().add("field-label");
        PasswordField password = new PasswordField(); password.setPromptText("请输入访问口令");
        Label error = new Label(); error.getStyleClass().add("error-text");
        Button enter = new Button("进入归档"); enter.getStyleClass().add("primary-button"); enter.setMaxWidth(Double.MAX_VALUE);
        Label note = new Label("数据保存在本机 SQLite，并自动保留最近备份。"); note.getStyleClass().add("small-muted");

        Runnable submit = () -> {
            if (auth.login(password.getText())) { error.setText(""); onSuccess.run(); }
            else { error.setText("访问口令错误"); password.clear(); }
        };
        enter.setOnAction(e -> submit.run()); password.setOnAction(e -> submit.run());
        card.getChildren().addAll(mark, eyebrow, title, subtitle, new Separator(), label, password, error, enter, note);
        root.getChildren().add(card);
    }

    public Parent root() { return root; }
}
