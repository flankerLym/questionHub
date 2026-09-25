package com.questionhub.desktop.ui;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import java.util.Optional;

public final class Dialogs {
    private Dialogs() {}

    public record QuestionInput(String question, String answer) {}

    public static Optional<String> text(Window owner, String title, String header, String initial) {
        TextInputDialog d = new TextInputDialog(initial == null ? "" : initial);
        d.initOwner(owner); d.setTitle(title); d.setHeaderText(header); d.setContentText("名称：");
        return d.showAndWait();
    }

    public static Optional<QuestionInput> question(Window owner, String title, String question, String answer) {
        Dialog<QuestionInput> d = new Dialog<>();
        d.initOwner(owner); d.setTitle(title); d.setHeaderText(title);
        ButtonType save = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextArea q = new TextArea(question == null ? "" : question);
        q.setPromptText("输入问题..."); q.setPrefRowCount(3); q.setWrapText(true);
        TextArea a = new TextArea(answer == null ? "" : answer);
        a.setPromptText("输入答案，可先留空..."); a.setPrefRowCount(10); a.setWrapText(true);
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10);
        g.add(new Label("问题"),0,0); g.add(q,0,1); g.add(new Label("答案"),0,2); g.add(a,0,3);
        GridPane.setHgrow(q, Priority.ALWAYS); GridPane.setHgrow(a, Priority.ALWAYS);
        d.getDialogPane().setContent(g); d.getDialogPane().setPrefWidth(640);
        d.setResultConverter(b -> b == save ? new QuestionInput(q.getText(), a.getText()) : null);
        return d.showAndWait();
    }

    public static boolean confirm(Window owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, ButtonType.OK);
        a.initOwner(owner); a.setTitle(title); a.setHeaderText(title);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public static void error(Window owner, Throwable e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initOwner(owner); a.setTitle("操作失败"); a.setHeaderText("操作失败");
        String m = e.getMessage();
        if ((m == null || m.isBlank()) && e.getCause() != null) m = e.getCause().getMessage();
        a.setContentText(m == null || m.isBlank() ? e.toString() : m);
        a.showAndWait();
    }

    public static void info(Window owner, String title, String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        a.initOwner(owner); a.setTitle(title); a.setHeaderText(title); a.showAndWait();
    }
}
