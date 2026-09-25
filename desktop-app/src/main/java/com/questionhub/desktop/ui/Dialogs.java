package com.questionhub.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.Optional;

public final class Dialogs {
    private Dialogs() {}

    public record QuestionInput(String question, String answer) {}

    public static Optional<String> text(Window owner, String title, String header, String initial) {
        Dialog<String> d = new Dialog<>();
        prepare(d, owner, title);

        ButtonType save = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(save, cancelType());

        Label h = new Label(header);
        h.getStyleClass().add("dialog-title");
        Label tip = new Label("名称尽量简短、明确，方便几个月后仍然一眼看懂。");
        tip.getStyleClass().add("dialog-description");
        tip.setWrapText(true);

        Label field = new Label("分类名称");
        field.getStyleClass().add("field-caption");
        TextField input = new TextField(initial == null ? "" : initial);
        input.setPromptText("例如：Java 并发 / 算法错题 / 英语写作");
        input.getStyleClass().add("flat-field");

        Label leading = new Label("Aa");
        leading.getStyleClass().add("input-leading-icon");
        HBox shell = new HBox(10, leading, input);
        shell.setAlignment(Pos.CENTER_LEFT);
        shell.getStyleClass().add("input-shell");
        HBox.setHgrow(input, Priority.ALWAYS);

        VBox fieldCard = new VBox(7, field, shell);
        fieldCard.getStyleClass().add("form-card");

        VBox content = new VBox(12, h, tip, spacer(2), fieldCard);
        content.setPadding(new Insets(4, 2, 4, 2));
        d.getDialogPane().setContent(content);
        d.getDialogPane().setPrefWidth(540);
        d.setResultConverter(b -> b == save ? input.getText() : null);
        d.setOnShown(e -> {
            input.requestFocus();
            input.selectAll();
        });
        return d.showAndWait();
    }

    public static Optional<QuestionInput> question(Window owner, String title, String question, String answer) {
        Dialog<QuestionInput> d = new Dialog<>();
        prepare(d, owner, title);

        ButtonType save = new ButtonType("保存这条记录", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(save, cancelType());

        Label h = new Label(title);
        h.getStyleClass().add("dialog-title");
        Label tip = new Label("问题写清楚“卡在哪里”，答案可以记录结论、思路、代码和以后容易忘记的点。");
        tip.getStyleClass().add("dialog-description");
        tip.setWrapText(true);

        TextArea q = new TextArea(question == null ? "" : question);
        q.setPromptText("例如：为什么 HashMap 在多线程下不能直接安全使用？");
        q.setPrefRowCount(4);
        q.setWrapText(true);
        q.getStyleClass().add("flat-area");

        Label qCount = new Label();
        qCount.getStyleClass().add("char-count");
        bindCount(q, qCount, 500);

        TextArea a = new TextArea(answer == null ? "" : answer);
        a.setPromptText("写下自己的理解、关键步骤、代码、踩坑记录……可以先留空，之后继续补充。");
        a.setPrefRowCount(13);
        a.setWrapText(true);
        a.getStyleClass().add("flat-area");

        Label aCount = new Label();
        aCount.getStyleClass().add("char-count");
        bindCount(a, aCount, 50_000);

        VBox qBox = editorSection("1", "问题 / 需要记住的知识点", "把问题描述完整，之后复习时不用重新回忆上下文。", q, qCount);
        VBox aBox = editorSection("2", "答案 / 学习笔记", "可以写结论、思路、代码、易错点，也可以先留空。", a, aCount);
        VBox.setVgrow(a, Priority.ALWAYS);
        VBox.setVgrow(aBox, Priority.ALWAYS);

        VBox content = new VBox(13, h, tip, divider(), qBox, aBox);
        content.setPadding(new Insets(2));
        d.getDialogPane().setContent(content);
        d.getDialogPane().setPrefWidth(790);
        d.getDialogPane().setPrefHeight(700);
        d.setResizable(true);
        d.setResultConverter(b -> b == save ? new QuestionInput(q.getText(), a.getText()) : null);
        d.setOnShown(e -> q.requestFocus());
        return d.showAndWait();
    }

    public static boolean confirm(Window owner, String title, String message) {
        Dialog<Boolean> d = new Dialog<>();
        prepare(d, owner, title);

        String okText = title.contains("删除") ? "确认删除" : title.contains("导入") ? "继续导入" : "确认";
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, cancelType());

        Label badge = new Label(title.contains("删除") ? "!" : "?");
        badge.getStyleClass().add(title.contains("删除") ? "dialog-error-badge" : "dialog-info-badge");

        Label h = new Label(title);
        h.getStyleClass().add("dialog-title");
        Label m = new Label(message);
        m.getStyleClass().add("dialog-description");
        m.setWrapText(true);
        m.setMaxWidth(500);

        VBox texts = new VBox(7, h, m);
        HBox content = new HBox(14, badge, texts);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(5, 2, 5, 2));
        HBox.setHgrow(texts, Priority.ALWAYS);
        d.getDialogPane().setContent(content);
        d.getDialogPane().setPrefWidth(580);

        Node okButton = d.getDialogPane().lookupButton(ok);
        okButton.getStyleClass().add(title.contains("删除") ? "danger-button" : "primary-button");
        d.setResultConverter(b -> b == ok);
        return d.showAndWait().orElse(false);
    }

    public static void error(Window owner, Throwable e) {
        String m = e.getMessage();
        if ((m == null || m.isBlank()) && e.getCause() != null) m = e.getCause().getMessage();
        if (m == null || m.isBlank()) m = e.toString();
        message(owner, "操作没有完成", m, true);
    }

    public static void info(Window owner, String title, String text) {
        message(owner, title, text, false);
    }

    private static void message(Window owner, String title, String text, boolean error) {
        Dialog<Void> d = new Dialog<>();
        prepare(d, owner, title);
        d.getDialogPane().getButtonTypes().add(new ButtonType("知道了", ButtonBar.ButtonData.OK_DONE));

        Label badge = new Label(error ? "!" : "✓");
        badge.getStyleClass().add(error ? "dialog-error-badge" : "dialog-success-badge");
        Label h = new Label(title);
        h.getStyleClass().add("dialog-title");
        Label m = new Label(text);
        m.getStyleClass().add("dialog-description");
        m.setWrapText(true);

        VBox texts = new VBox(6, h, m);
        HBox content = new HBox(14, badge, texts);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(4, 2, 4, 2));
        HBox.setHgrow(texts, Priority.ALWAYS);

        d.getDialogPane().setContent(content);
        d.getDialogPane().setPrefWidth(560);
        d.showAndWait();
    }

    private static VBox editorSection(String number, String title, String hint, TextArea area, Label count) {
        Label badge = new Label(number);
        badge.getStyleClass().add("section-number");
        Label t = new Label(title);
        t.getStyleClass().add("field-caption");
        Label h = new Label(hint);
        h.getStyleClass().add("field-hint");
        h.setWrapText(true);
        VBox texts = new VBox(2, t, h);
        HBox head = new HBox(9, badge, texts);
        head.setAlignment(Pos.CENTER_LEFT);

        StackPane editor = new StackPane(area);
        editor.getStyleClass().add("editor-surface");
        StackPane.setMargin(area, Insets.EMPTY);
        VBox.setVgrow(editor, Priority.ALWAYS);

        HBox footer = new HBox(count);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(8, head, editor, footer);
        box.getStyleClass().add("form-card");
        VBox.setVgrow(editor, Priority.ALWAYS);
        return box;
    }

    private static void prepare(Dialog<?> d, Window owner, String title) {
        if (owner != null) d.initOwner(owner);
        d.setTitle(title);
        d.setHeaderText(null);
        d.getDialogPane().getStylesheets().add(Dialogs.class.getResource("/app.css").toExternalForm());
        d.getDialogPane().getStyleClass().add("study-dialog");
        AppIcons.apply(d);
    }

    private static ButtonType cancelType() {
        return new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
    }

    private static Separator divider() {
        Separator s = new Separator();
        s.getStyleClass().add("soft-separator");
        return s;
    }

    private static Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private static void bindCount(TextArea area, Label label, int max) {
        Runnable update = () -> label.setText(area.getText().length() + " / " + max);
        area.textProperty().addListener((o, a, b) -> update.run());
        update.run();
    }
}
