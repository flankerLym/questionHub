package com.questionhub.desktop.ui;

import com.questionhub.desktop.db.ArchiveRepository;
import com.questionhub.desktop.model.Folder;
import com.questionhub.desktop.model.QaItem;
import com.questionhub.desktop.service.ArchiveJsonService;
import com.questionhub.desktop.util.Ids;
import com.questionhub.desktop.util.Validation;
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
    private final Label questionCount = new Label("0 条记录");
    private final Label currentQuestion = new Label("先从左侧选择一个问题");
    private final Label currentMeta = new Label("选择后可以在这里整理答案、思路和易错点");
    private final TextArea answer = new TextArea();
    private final Button saveAnswer = new Button("保存修改");

    public MainView(ArchiveRepository repo, ArchiveJsonService json, Path dataDir, Runnable onLogout) {
        this.repo = repo;
        this.json = json;
        this.dataDir = dataDir;
        this.onLogout = onLogout;
        build();
        reloadFolders();
    }

    public Parent root() { return root; }

    private void build() {
        root.getStyleClass().add("app-root");
        root.setTop(topbar());
        root.setCenter(workspace());
        root.setBottom(statusBar());
    }

    private Parent topbar() {
        Label mark = new Label("QH");
        mark.getStyleClass().add("brand-mark-small");

        Label title = label("QuestionHub 学习资料库", "brand-title");
        Label subtitle = label("把遇到的问题沉淀下来，复习时不再从头找答案", "small-muted");
        VBox brandText = new VBox(2, title, subtitle);
        HBox brand = new HBox(11, mark, brandText);
        brand.setAlignment(Pos.CENTER_LEFT);

        Button importBtn = softButton("导入资料");
        importBtn.setTooltip(new Tooltip("用 JSON 归档覆盖当前资料"));
        importBtn.setOnAction(e -> importJson());

        Button exportBtn = softButton("备份导出");
        exportBtn.setTooltip(new Tooltip("导出完整 JSON 归档"));
        exportBtn.setOnAction(e -> exportJson());

        Button folderBtn = softButton("存储位置");
        folderBtn.setTooltip(new Tooltip("打开本地数据目录"));
        folderBtn.setOnAction(e -> openDataDir());

        Button logout = softButton("锁定资料库");
        logout.setTooltip(new Tooltip("返回口令页，不退出程序"));
        logout.setOnAction(e -> onLogout.run());

        HBox actions = new HBox(7, importBtn, exportBtn, folderBtn, logout);
        actions.setAlignment(Pos.CENTER_RIGHT);

        BorderPane bar = new BorderPane();
        bar.getStyleClass().add("topbar");
        bar.setLeft(brand);
        bar.setRight(actions);
        return bar;
    }

    private Parent workspace() {
        SplitPane split = new SplitPane(folderPane(), questionPane(), answerPane());
        split.setDividerPositions(.19, .56);
        split.getStyleClass().add("workspace");
        BorderPane.setMargin(split, new Insets(16, 16, 10, 16));
        return split;
    }

    private Parent folderPane() {
        Label title = label("学习分类", "panel-title");
        Label hint = label("按课程、技术或专题整理", "section-hint");
        VBox heading = new VBox(2, title, hint);

        Button add = new Button("＋ 分类");
        add.getStyleClass().add("primary-button");
        add.setOnAction(e -> createFolder());

        HBox head = new HBox(8, heading, spacer(), add);
        head.setAlignment(Pos.CENTER_LEFT);

        folderList.setPlaceholder(emptyState("还没有学习分类", "先创建一个分类，例如“Java”“算法”或“高数错题”"));
        folderList.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(Folder f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label dot = new Label("●");
                dot.getStyleClass().add("folder-dot");
                Label name = label(f.name(), "folder-name");
                name.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(name, Priority.ALWAYS);
                HBox row = new HBox(8, dot, name);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        folderList.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> loadQuestions(b));
        folderList.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.DELETE) deleteFolder(); });

        ContextMenu menu = new ContextMenu();
        MenuItem rename = new MenuItem("重命名分类");
        rename.setOnAction(e -> renameFolder());
        MenuItem del = new MenuItem("删除分类");
        del.setOnAction(e -> deleteFolder());
        menu.getItems().addAll(rename, del);
        folderList.setContextMenu(menu);

        VBox box = panel(head, divider(), folderList);
        VBox.setVgrow(folderList, Priority.ALWAYS);
        return box;
    }

    private Parent questionPane() {
        folderTitle.getStyleClass().add("panel-title");
        questionCount.getStyleClass().add("count-badge");

        Button add = new Button("＋ 记录问题");
        add.getStyleClass().add("primary-button");
        add.setOnAction(e -> createQuestion());

        HBox titleLine = new HBox(8, folderTitle, questionCount);
        titleLine.setAlignment(Pos.CENTER_LEFT);
        VBox heading = new VBox(2, titleLine, label("把不会的、易错的、值得复习的都留下来", "section-hint"));
        HBox head = new HBox(8, heading, spacer(), add);
        head.setAlignment(Pos.CENTER_LEFT);

        search.setPromptText("搜索问题、答案或关键词…");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((o, a, b) -> filter(b));

        questionList.setPlaceholder(emptyState("这里还没有问题", "点击“记录问题”，把今天遇到的第一个问题记下来"));
        questionList.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(QaItem q, boolean empty) {
                super.updateItem(q, empty);
                if (empty || q == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label qt = label(q.question(), "question-text");
                qt.setWrapText(true);

                String preview = compact(q.answer());
                Label pv = label(preview.isEmpty() ? "还没有整理答案" : preview, preview.isEmpty() ? "question-empty-answer" : "question-preview");
                pv.setWrapText(false);

                String t = DateTimeFormatter.ofPattern("MM-dd HH:mm")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(q.updatedAt()));
                Label time = label("最近整理  " + t, "small-muted");

                VBox card = new VBox(5, qt, pv, time);
                card.setFillWidth(true);
                setGraphic(card);
                setText(null);
            }
        });
        questionList.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showAnswer(b));

        ContextMenu menu = new ContextMenu();
        MenuItem edit = new MenuItem("编辑问题和答案");
        edit.setOnAction(e -> editQuestion());
        MenuItem del = new MenuItem("删除这条记录");
        del.setOnAction(e -> deleteQuestion());
        menu.getItems().addAll(edit, del);
        questionList.setContextMenu(menu);
        questionList.setOnMouseClicked(e -> { if (e.getClickCount() == 2) editQuestion(); });

        VBox box = panel(head, search, questionList);
        VBox.setVgrow(questionList, Priority.ALWAYS);
        return box;
    }

    private Parent answerPane() {
        Label title = label("学习笔记 / 答案", "panel-title");
        Label hint = label("整理结论、思路、代码和易错点", "section-hint");
        VBox heading = new VBox(2, title, hint);
        HBox head = new HBox(8, heading, spacer());
        head.setAlignment(Pos.CENTER_LEFT);

        Label currentLabel = label("当前问题", "field-caption");
        currentQuestion.setWrapText(true);
        currentQuestion.getStyleClass().add("selected-question");
        currentMeta.getStyleClass().add("selected-meta");
        currentMeta.setWrapText(true);

        answer.setWrapText(true);
        answer.setPromptText("在这里整理答案、解题思路、代码片段、容易忘记的点……");
        answer.getStyleClass().add("answer-editor");

        Label saveHint = label("修改后点击保存，内容会写入本地 SQLite", "small-muted");
        saveAnswer.getStyleClass().add("primary-button");
        saveAnswer.setOnAction(e -> saveAnswer());
        HBox actions = new HBox(10, saveHint, spacer(), saveAnswer);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox box = panel(head, divider(), currentLabel, currentQuestion, currentMeta, answer, actions);
        VBox.setVgrow(answer, Priority.ALWAYS);
        return box;
    }

    private Parent statusBar() {
        Label left = label("本地保存 · 自动备份 · 无网络也能使用", "status-text");
        Label right = label("坚持记录，复习会越来越轻松", "status-text");
        HBox bar = new HBox(10, left, spacer(), right);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void reloadFolders() {
        try {
            Folder selected = folderList.getSelectionModel().getSelectedItem();
            folders.setAll(repo.folders());
            if (folders.isEmpty()) { clearQuestions(); return; }
            int idx = 0;
            if (selected != null) {
                for (int i = 0; i < folders.size(); i++) {
                    if (folders.get(i).id().equals(selected.id())) { idx = i; break; }
                }
            }
            folderList.getSelectionModel().select(idx);
        } catch (Exception e) { fail(e); }
    }

    private void loadQuestions(Folder folder) {
        search.clear();
        if (folder == null) { clearQuestions(); return; }
        folderTitle.setText(folder.name());
        try {
            allItems.setAll(repo.questions(folder.id()));
            filter("");
            if (!visibleItems.isEmpty()) questionList.getSelectionModel().select(0);
            else showAnswer(null);
        } catch (Exception e) { fail(e); }
    }

    private void filter(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (k.isEmpty()) visibleItems.setAll(allItems);
        else visibleItems.setAll(allItems.filtered(q -> q.question().toLowerCase(Locale.ROOT).contains(k) || q.answer().toLowerCase(Locale.ROOT).contains(k)));
        questionCount.setText(k.isEmpty() ? visibleItems.size() + " 条记录" : visibleItems.size() + " / " + allItems.size() + " 条");
    }

    private void showAnswer(QaItem item) {
        boolean ok = item != null;
        currentQuestion.setText(ok ? item.question() : "先从左侧选择一个问题");
        if (ok) {
            String t = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(item.updatedAt()));
            currentMeta.setText("最近整理于 " + t + " · 可以直接在下方继续补充");
        } else {
            currentMeta.setText("选择后可以在这里整理答案、思路和易错点");
        }
        answer.setText(ok ? item.answer() : "");
        answer.setDisable(!ok);
        saveAnswer.setDisable(!ok);
    }

    private void createFolder() {
        try {
            var name = Dialogs.text(window(), "新建学习分类", "给知识找一个长期稳定的位置", "");
            if (name.isEmpty()) return;
            String n = Validation.folderName(name.get());
            long now = System.currentTimeMillis();
            Folder f = new Folder(Ids.create("folder"), n, folders.size(), now, now);
            repo.insertFolder(f);
            reloadFolders();
            folderList.getSelectionModel().select(folders.size() - 1);
        } catch (Exception e) { fail(e); }
    }

    private void renameFolder() {
        Folder f = folderList.getSelectionModel().getSelectedItem();
        if (f == null) return;
        try {
            var name = Dialogs.text(window(), "重命名学习分类", "使用更清楚、以后也看得懂的名称", f.name());
            if (name.isEmpty()) return;
            repo.renameFolder(f.id(), name.get());
            reloadFolders();
        } catch (Exception e) { fail(e); }
    }

    private void deleteFolder() {
        Folder f = folderList.getSelectionModel().getSelectedItem();
        if (f == null) return;
        if (!Dialogs.confirm(window(), "删除这个学习分类？", "“" + f.name() + "”中的全部问题和答案都会一起删除。此操作无法撤销。")) return;
        try { repo.deleteFolder(f.id()); reloadFolders(); }
        catch (Exception e) { fail(e); }
    }

    private void createQuestion() {
        Folder f = folderList.getSelectionModel().getSelectedItem();
        if (f == null) {
            Dialogs.info(window(), "先选择学习分类", "创建或选择一个分类后，再记录问题。这样以后复习时更容易找到。 ");
            return;
        }
        try {
            var r = Dialogs.question(window(), "记录一个问题", "", "");
            if (r.isEmpty()) return;
            String q = Validation.question(r.get().question());
            String a = Validation.answer(r.get().answer());
            long now = System.currentTimeMillis();
            repo.upsertQuestion(new QaItem(Ids.create("qa"), f.id(), q, a, now, now));
            loadQuestions(f);
        } catch (Exception e) { fail(e); }
    }

    private void editQuestion() {
        QaItem item = questionList.getSelectionModel().getSelectedItem();
        if (item == null) return;
        try {
            var r = Dialogs.question(window(), "编辑学习记录", item.question(), item.answer());
            if (r.isEmpty()) return;
            repo.upsertQuestion(item.withContent(
                    Validation.question(r.get().question()),
                    Validation.answer(r.get().answer()),
                    System.currentTimeMillis()));
            loadQuestions(folderList.getSelectionModel().getSelectedItem());
        } catch (Exception e) { fail(e); }
    }

    private void deleteQuestion() {
        QaItem item = questionList.getSelectionModel().getSelectedItem();
        if (item == null) return;
        if (!Dialogs.confirm(window(), "删除这条学习记录？", "确定删除“" + item.question() + "”吗？删除后无法恢复。")) return;
        try { repo.deleteQuestion(item.id()); loadQuestions(folderList.getSelectionModel().getSelectedItem()); }
        catch (Exception e) { fail(e); }
    }

    private void saveAnswer() {
        QaItem item = questionList.getSelectionModel().getSelectedItem();
        if (item == null) return;
        try {
            repo.upsertQuestion(item.withContent(item.question(), Validation.answer(answer.getText()), System.currentTimeMillis()));
            loadQuestions(folderList.getSelectionModel().getSelectedItem());
        } catch (Exception e) { fail(e); }
    }

    private void importJson() {
        FileChooser fc = new FileChooser();
        fc.setTitle("导入 QuestionHub 学习归档");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("QuestionHub JSON 归档", "*.json"));
        var f = fc.showOpenDialog(window());
        if (f == null) return;
        if (!Dialogs.confirm(window(), "导入并覆盖当前资料？", "导入会用所选归档完整替换当前学习分类和问答。建议先执行一次“备份导出”。")) return;
        try {
            json.importAndReplace(f.toPath());
            reloadFolders();
            Dialogs.info(window(), "导入完成", "学习资料已经恢复，可以继续使用。 ");
        } catch (Exception e) { fail(e); }
    }

    private void exportJson() {
        FileChooser fc = new FileChooser();
        fc.setTitle("导出 QuestionHub 学习归档");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("QuestionHub JSON 归档", "*.json"));
        fc.setInitialFileName("questionhub-backup-" + java.time.LocalDate.now() + ".json");
        var f = fc.showSaveDialog(window());
        if (f == null) return;
        try {
            json.exportTo(f.toPath());
            Dialogs.info(window(), "备份已保存", "完整学习归档已经导出。建议把重要备份同步到自己的网盘或移动硬盘。 ");
        } catch (Exception e) { fail(e); }
    }

    private void openDataDir() {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dataDir.toFile());
            else Dialogs.info(window(), "本地数据目录", dataDir.toString());
        } catch (Exception e) {
            Dialogs.info(window(), "本地数据目录", dataDir.toString());
        }
    }

    private void clearQuestions() {
        allItems.clear();
        visibleItems.clear();
        folderTitle.setText("问题列表");
        questionCount.setText("0 条记录");
        questionList.getSelectionModel().clearSelection();
        showAnswer(null);
    }

    private VBox panel(javafx.scene.Node... nodes) {
        VBox box = new VBox(12, nodes);
        box.getStyleClass().add("panel");
        box.setPadding(new Insets(18));
        return box;
    }

    private Parent emptyState(String title, String subtitle) {
        Label t = label(title, "empty-title");
        Label s = label(subtitle, "empty-subtitle");
        s.setWrapText(true);
        VBox box = new VBox(6, t, s);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28, 16, 28, 16));
        box.setMaxWidth(280);
        return box;
    }

    private Separator divider() {
        Separator s = new Separator();
        s.getStyleClass().add("soft-separator");
        return s;
    }

    private String compact(String text) {
        if (text == null || text.isBlank()) return "";
        String s = text.replaceAll("\\s+", " ").trim();
        return s.length() <= 74 ? s : s.substring(0, 74) + "…";
    }

    private Label label(String text, String cls) {
        Label l = new Label(text);
        l.getStyleClass().add(cls);
        return l;
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private Button softButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("soft-button");
        return b;
    }

    private Window window() { return root.getScene() == null ? null : root.getScene().getWindow(); }
    private void fail(Throwable e) { Dialogs.error(window(), e); }
}
