package com.questionhub.desktop.ui;

import com.questionhub.desktop.db.ArchiveRepository;
import com.questionhub.desktop.model.Folder;
import com.questionhub.desktop.model.QaItem;
import com.questionhub.desktop.service.ArchiveJsonService;
import com.questionhub.desktop.util.Ids;
import com.questionhub.desktop.util.Validation;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class MainView {
    private final BorderPane root = new BorderPane();
    private final ArchiveRepository repo;
    private final ArchiveJsonService json;
    private final Path dataDir;
    private final Runnable onLogout;

    private final ObservableList<Folder> folders = FXCollections.observableArrayList();
    private final ObservableList<QaItem> allItems = FXCollections.observableArrayList();
    private final ObservableList<QaItem> visibleItems = FXCollections.observableArrayList();
    private final ListView<Folder> folderList = new ListView<>(folders);
    private final ListView<QaItem> questionList = new ListView<>(visibleItems);
    private final TextField search = new TextField();
    private final Label folderTitle = new Label("问题列表");
    private final Label currentQuestion = new Label("选择一个问题");
    private final TextArea answer = new TextArea();
    private final Button saveAnswer = new Button("保存答案");

    public MainView(ArchiveRepository repo, ArchiveJsonService json, Path dataDir, Runnable onLogout) {
        this.repo = repo; this.json = json; this.dataDir = dataDir; this.onLogout = onLogout;
        build(); reloadFolders();
    }

    public Parent root() { return root; }

    private void build() {
        root.getStyleClass().add("app-root");
        root.setTop(topbar());
        root.setCenter(workspace());
    }

    private Parent topbar() {
        Label mark = new Label("QA"); mark.getStyleClass().add("brand-mark-small");
        VBox brandText = new VBox(1, label("问题归档系统", "brand-title"), label("Local knowledge archive", "small-muted"));
        HBox brand = new HBox(10, mark, brandText); brand.setAlignment(Pos.CENTER_LEFT);

        Button importBtn = softButton("导入"); importBtn.setOnAction(e -> importJson());
        Button exportBtn = softButton("导出"); exportBtn.setOnAction(e -> exportJson());
        Button folderBtn = softButton("数据目录"); folderBtn.setOnAction(e -> openDataDir());
        Button logout = softButton("退出"); logout.setOnAction(e -> onLogout.run());
        HBox actions = new HBox(6, importBtn, exportBtn, folderBtn, logout); actions.setAlignment(Pos.CENTER_RIGHT);

        BorderPane bar = new BorderPane(); bar.getStyleClass().add("topbar"); bar.setLeft(brand); bar.setRight(actions);
        return bar;
    }

    private Parent workspace() {
        SplitPane split = new SplitPane(folderPane(), questionPane(), answerPane());
        split.setDividerPositions(.20, .58);
        split.getStyleClass().add("workspace");
        BorderPane.setMargin(split, new Insets(14));
        return split;
    }

    private Parent folderPane() {
        Label title = label("文件夹", "panel-title");
        Button add = new Button("＋"); add.getStyleClass().add("primary-button"); add.setOnAction(e -> createFolder());
        HBox head = new HBox(8, title, spacer(), add); head.setAlignment(Pos.CENTER_LEFT);
        folderList.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(Folder f, boolean empty) {
                super.updateItem(f, empty); setText(empty || f == null ? null : f.name());
            }
        });
        folderList.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> loadQuestions(b));
        folderList.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.DELETE) deleteFolder(); });
        ContextMenu menu = new ContextMenu();
        MenuItem rename = new MenuItem("重命名"); rename.setOnAction(e -> renameFolder());
        MenuItem del = new MenuItem("删除"); del.setOnAction(e -> deleteFolder()); menu.getItems().addAll(rename, del);
        folderList.setContextMenu(menu);
        VBox box = panel(head, folderList); VBox.setVgrow(folderList, Priority.ALWAYS); return box;
    }

    private Parent questionPane() {
        Button add = new Button("＋ 新建问题"); add.getStyleClass().add("primary-button"); add.setOnAction(e -> createQuestion());
        HBox head = new HBox(8, folderTitle, spacer(), add); head.setAlignment(Pos.CENTER_LEFT);
        search.setPromptText("搜索问题或答案..."); search.textProperty().addListener((o,a,b) -> filter(b));
        questionList.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(QaItem q, boolean empty) {
                super.updateItem(q, empty);
                if (empty || q == null) { setGraphic(null); setText(null); return; }
                Label qt = label(q.question(), "question-text"); qt.setWrapText(true);
                String t = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(q.updatedAt()));
                setGraphic(new VBox(4, qt, label(t, "small-muted"))); setText(null);
            }
        });
        questionList.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> showAnswer(b));
        ContextMenu menu = new ContextMenu();
        MenuItem edit = new MenuItem("编辑"); edit.setOnAction(e -> editQuestion());
        MenuItem del = new MenuItem("删除"); del.setOnAction(e -> deleteQuestion()); menu.getItems().addAll(edit, del); questionList.setContextMenu(menu);
        questionList.setOnMouseClicked(e -> { if (e.getClickCount() == 2) editQuestion(); });
        VBox box = panel(head, search, questionList); VBox.setVgrow(questionList, Priority.ALWAYS); return box;
    }

    private Parent answerPane() {
        Label title = label("答案", "panel-title");
        HBox head = new HBox(8, title, spacer()); head.setAlignment(Pos.CENTER_LEFT);
        currentQuestion.setWrapText(true); currentQuestion.getStyleClass().add("selected-question");
        answer.setWrapText(true); answer.setPromptText("暂无答案，输入后点击保存。");
        saveAnswer.getStyleClass().add("primary-button"); saveAnswer.setOnAction(e -> saveAnswer());
        HBox actions = new HBox(saveAnswer); actions.setAlignment(Pos.CENTER_RIGHT);
        VBox box = panel(head, currentQuestion, answer, actions); VBox.setVgrow(answer, Priority.ALWAYS); return box;
    }

    private void reloadFolders() {
        try {
            Folder selected = folderList.getSelectionModel().getSelectedItem();
            folders.setAll(repo.folders());
            if (folders.isEmpty()) { clearQuestions(); return; }
            int idx = 0;
            if (selected != null) for (int i=0;i<folders.size();i++) if (folders.get(i).id().equals(selected.id())) { idx=i; break; }
            folderList.getSelectionModel().select(idx);
        } catch (Exception e) { fail(e); }
    }

    private void loadQuestions(Folder folder) {
        search.clear();
        if (folder == null) { clearQuestions(); return; }
        folderTitle.setText(folder.name());
        try {
            allItems.setAll(repo.questions(folder.id())); filter("");
            if (!visibleItems.isEmpty()) questionList.getSelectionModel().select(0); else showAnswer(null);
        } catch (Exception e) { fail(e); }
    }

    private void filter(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (k.isEmpty()) visibleItems.setAll(allItems);
        else visibleItems.setAll(allItems.filtered(q -> q.question().toLowerCase(Locale.ROOT).contains(k) || q.answer().toLowerCase(Locale.ROOT).contains(k)));
    }

    private void showAnswer(QaItem item) {
        boolean ok = item != null;
        currentQuestion.setText(ok ? item.question() : "选择一个问题");
        answer.setText(ok ? item.answer() : ""); answer.setDisable(!ok); saveAnswer.setDisable(!ok);
    }

    private void createFolder() {
        try {
            var name = Dialogs.text(window(), "新建文件夹", "新建文件夹", "");
            if (name.isEmpty()) return;
            String n = Validation.folderName(name.get());
            long now = System.currentTimeMillis();
            Folder f = new Folder(Ids.create("folder"), n, folders.size(), now, now);
            repo.insertFolder(f); reloadFolders(); folderList.getSelectionModel().select(folders.size()-1);
        } catch (Exception e) { fail(e); }
    }

    private void renameFolder() {
        Folder f = folderList.getSelectionModel().getSelectedItem(); if (f == null) return;
        try {
            var name = Dialogs.text(window(), "重命名文件夹", "新的文件夹名称", f.name());
            if (name.isEmpty()) return; repo.renameFolder(f.id(), name.get()); reloadFolders();
        } catch (Exception e) { fail(e); }
    }

    private void deleteFolder() {
        Folder f = folderList.getSelectionModel().getSelectedItem(); if (f == null) return;
        if (!Dialogs.confirm(window(), "删除文件夹？", "将删除“" + f.name() + "”以及其中的全部问题，此操作无法撤销。")) return;
        try { repo.deleteFolder(f.id()); reloadFolders(); } catch (Exception e) { fail(e); }
    }

    private void createQuestion() {
        Folder f = folderList.getSelectionModel().getSelectedItem();
        if (f == null) { Dialogs.info(window(), "提示", "请先创建或选择文件夹"); return; }
        try {
            var r = Dialogs.question(window(), "新建问答", "", ""); if (r.isEmpty()) return;
            String q = Validation.question(r.get().question()); String a = Validation.answer(r.get().answer()); long now = System.currentTimeMillis();
            repo.upsertQuestion(new QaItem(Ids.create("qa"), f.id(), q, a, now, now)); loadQuestions(f);
        } catch (Exception e) { fail(e); }
    }

    private void editQuestion() {
        QaItem item = questionList.getSelectionModel().getSelectedItem(); if (item == null) return;
        try {
            var r = Dialogs.question(window(), "编辑问答", item.question(), item.answer()); if (r.isEmpty()) return;
            repo.upsertQuestion(item.withContent(Validation.question(r.get().question()), Validation.answer(r.get().answer()), System.currentTimeMillis()));
            loadQuestions(folderList.getSelectionModel().getSelectedItem());
        } catch (Exception e) { fail(e); }
    }

    private void deleteQuestion() {
        QaItem item = questionList.getSelectionModel().getSelectedItem(); if (item == null) return;
        if (!Dialogs.confirm(window(), "删除问题？", "确定删除“" + item.question() + "”吗？此操作无法撤销。")) return;
        try { repo.deleteQuestion(item.id()); loadQuestions(folderList.getSelectionModel().getSelectedItem()); } catch (Exception e) { fail(e); }
    }

    private void saveAnswer() {
        QaItem item = questionList.getSelectionModel().getSelectedItem(); if (item == null) return;
        try {
            repo.upsertQuestion(item.withContent(item.question(), Validation.answer(answer.getText()), System.currentTimeMillis()));
            loadQuestions(folderList.getSelectionModel().getSelectedItem());
        } catch (Exception e) { fail(e); }
    }

    private void importJson() {
        FileChooser fc = new FileChooser(); fc.setTitle("导入 QuestionHub JSON");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
        var f = fc.showOpenDialog(window()); if (f == null) return;
        if (!Dialogs.confirm(window(), "导入并覆盖当前数据？", "导入会覆盖当前文件夹与问答数据。建议先导出备份。")) return;
        try { json.importAndReplace(f.toPath()); reloadFolders(); Dialogs.info(window(), "导入成功", "数据已全量覆盖。 "); }
        catch (Exception e) { fail(e); }
    }

    private void exportJson() {
        FileChooser fc = new FileChooser(); fc.setTitle("导出 QuestionHub JSON");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
        fc.setInitialFileName("question-archive-" + java.time.LocalDate.now() + ".json");
        var f = fc.showSaveDialog(window()); if (f == null) return;
        try { json.exportTo(f.toPath()); Dialogs.info(window(), "导出成功", "归档文件已保存。 "); } catch (Exception e) { fail(e); }
    }

    private void openDataDir() {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dataDir.toFile());
            else Dialogs.info(window(), "数据目录", dataDir.toString());
        } catch (Exception e) { Dialogs.info(window(), "数据目录", dataDir.toString()); }
    }

    private void clearQuestions() {
        allItems.clear(); visibleItems.clear(); folderTitle.setText("问题列表"); questionList.getSelectionModel().clearSelection(); showAnswer(null);
    }

    private VBox panel(javafx.scene.Node... nodes) {
        VBox box = new VBox(10, nodes); box.getStyleClass().add("panel"); box.setPadding(new Insets(16)); return box;
    }
    private Label label(String text, String cls) { Label l = new Label(text); l.getStyleClass().add(cls); return l; }
    private Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }
    private Button softButton(String text) { Button b = new Button(text); b.getStyleClass().add("soft-button"); return b; }
    private Window window() { return root.getScene() == null ? null : root.getScene().getWindow(); }
    private void fail(Throwable e) { Dialogs.error(window(), e); }
}
