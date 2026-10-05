package com.questionhub.desktop.ui;

import com.questionhub.desktop.db.ArchiveRepository;
import com.questionhub.desktop.model.Folder;
import com.questionhub.desktop.model.QaItem;
import com.questionhub.desktop.service.ArchivePackageService;
import com.questionhub.desktop.service.AssetService;
import com.questionhub.desktop.util.Ids;
import com.questionhub.desktop.util.Validation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.Locale;

public final class MainView {
    private final BorderPane root = new BorderPane();
    private final ArchiveRepository repo;
    private final ArchivePackageService archive;
    private final AssetService assets;
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

    private final WebView answerPreview = new WebView();
    private final TextArea answer = new TextArea();
    private final VBox answerEditorPane = new VBox();
    private final Label answerCount = new Label("0 / 50000");
    private final Label answerMode = new Label("阅读模式");
    private final Button expandAnswer = new Button("放大查看");
    private final Button editAnswer = new Button("编辑整理");
    private final Button shrinkImage = new Button("图片 −");
    private final Button growImage = new Button("图片 ＋");
    private final Button cancelAnswer = new Button("取消");
    private final Button saveAnswer = new Button("保存修改");

    private boolean editingAnswer;
    private String activeImageAsset;

    public MainView(ArchiveRepository repo, ArchivePackageService archive, Path dataDir, Runnable onLogout) {
        this.repo = repo;
        this.archive = archive;
        this.dataDir = dataDir;
        this.assets = new AssetService(dataDir);
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

        Button importBtn = softButton("⇩", "导入数据包");
        importBtn.setTooltip(new Tooltip("导入完整 QuestionHub 数据包，包含问题、答案和图片"));
        importBtn.setOnAction(e -> importPackage());

        Button exportBtn = softButton("⇧", "备份数据包");
        exportBtn.setTooltip(new Tooltip("导出完整数据包，问题、答案、截图会一起打包"));
        exportBtn.setOnAction(e -> exportPackage());

        Button folderBtn = softButton("⌂", "存储位置");
        folderBtn.setTooltip(new Tooltip("打开本地数据目录"));
        folderBtn.setOnAction(e -> openDataDir());

        Button logout = softButton("◇", "锁定资料库");
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
        // 给右侧“我的整理”更多长期阅读/代码/公式展示空间
        split.setDividerPositions(.18, .48);
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
        box.setMinWidth(220);
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
        VBox heading = new VBox(2, titleLine, label("这里只显示问题，答案统一在右侧查看", "section-hint"));
        HBox head = new HBox(8, heading, spacer(), add);
        head.setAlignment(Pos.CENTER_LEFT);

        search.setPromptText("搜索问题、答案或关键词…");
        search.getStyleClass().add("flat-field");
        search.textProperty().addListener((o, a, b) -> filter(b));
        Label searchIcon = new Label("⌕");
        searchIcon.getStyleClass().add("input-leading-icon");
        HBox searchBox = new HBox(9, searchIcon, search);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().addAll("input-shell", "search-shell");
        HBox.setHgrow(search, Priority.ALWAYS);

        questionList.setPlaceholder(emptyState("这里还没有问题", "点击“记录问题”，把今天遇到的第一个问题记下来"));
        questionList.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(QaItem q, boolean empty) {
                super.updateItem(q, empty);
                if (empty || q == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                // 老板要求：问题列表只展示问题本身，不再泄露/预览答案。
                Label qt = label(q.question(), "question-text");
                qt.setWrapText(true);
                qt.setMaxWidth(Double.MAX_VALUE);
                VBox card = new VBox(qt);
                card.getStyleClass().add("question-only-card");
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

        VBox box = panel(head, searchBox, questionList);
        box.setMinWidth(360);
        VBox.setVgrow(questionList, Priority.ALWAYS);
        return box;
    }

    private Parent answerPane() {
        currentQuestion.setWrapText(true);
        currentQuestion.getStyleClass().add("question-focus-text");

        Label questionBadge = label("问题", "question-focus-badge");
        HBox questionBar = new HBox(10, questionBadge, currentQuestion);
        questionBar.setAlignment(Pos.CENTER_LEFT);
        questionBar.getStyleClass().add("question-focus-bar");
        HBox.setHgrow(currentQuestion, Priority.ALWAYS);

        answer.setWrapText(true);
        answer.setPromptText("""
                在这里整理答案、解题思路、代码片段、公式和截图……

                代码块：
                ```java
                int ans = 0;
                ```

                LaTeX：
                行内公式 $x^2+y^2$
                独立公式 $$\\frac{a}{b}$$

                截图：
                Win + Shift + S 截图后，回到这里直接 Ctrl + V。
                """);
        answer.getStyleClass().addAll("answer-editor", "flat-area");
        answerCount.getStyleClass().add("char-count");
        answer.textProperty().addListener((o, a, b) -> answerCount.setText(b.length() + " / 50000"));
        answer.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (editingAnswer && e.isShortcutDown() && e.getCode() == KeyCode.V) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                if (clipboard.hasImage()) {
                    try {
                        pasteScreenshot(clipboard);
                        e.consume();
                    } catch (Exception ex) {
                        fail(ex);
                        e.consume();
                    }
                }
            }
        });

        Label editorHelp = label(
                "支持文本、代码、LaTeX；Win+Shift+S 后 Ctrl+V 可插入截图，选中图片标记后可缩放",
                "editor-help");

        shrinkImage.getStyleClass().add("soft-button");
        shrinkImage.setTooltip(new Tooltip("将当前截图缩小 10%"));
        shrinkImage.setOnAction(e -> resizeCurrentImage(-10));

        growImage.getStyleClass().add("soft-button");
        growImage.setTooltip(new Tooltip("将当前截图放大 10%"));
        growImage.setOnAction(e -> resizeCurrentImage(10));

        HBox imageTools = new HBox(7,
                label("截图", "image-tool-label"),
                shrinkImage,
                growImage);
        imageTools.setAlignment(Pos.CENTER_LEFT);

        answerEditorPane.getChildren().setAll(editorHelp, imageTools, answer);
        answerEditorPane.setSpacing(8);
        answerEditorPane.getStyleClass().add("answer-editor-pane");
        VBox.setVgrow(answer, Priority.ALWAYS);

        answerPreview.setContextMenuEnabled(false);
        answerPreview.setMinHeight(520);
        answerPreview.setPrefHeight(720);
        answerPreview.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        answerPreview.getStyleClass().add("answer-preview");

        StackPane content = new StackPane(answerPreview, answerEditorPane);
        content.getStyleClass().add("answer-content");
        content.setMinHeight(520);
        VBox.setVgrow(content, Priority.ALWAYS);

        Label pen = new Label("✎");
        pen.getStyleClass().add("editor-toolbar-icon");
        Label editorTitle = label("我的整理", "editor-toolbar-title");
        Label editorHint = label("代码、公式和截图都会在阅读模式中排版显示", "editor-toolbar-hint");
        VBox editorTitles = new VBox(1, editorTitle, editorHint);

        answerMode.getStyleClass().add("mode-badge");

        expandAnswer.getStyleClass().add("soft-button");
        expandAnswer.setTooltip(new Tooltip("在独立大窗口中查看当前整理"));
        expandAnswer.setOnAction(e -> openAnswerWindow());

        editAnswer.getStyleClass().add("soft-button");
        editAnswer.setOnAction(e -> beginAnswerEdit());

        cancelAnswer.getStyleClass().add("soft-button");
        cancelAnswer.setOnAction(e -> cancelAnswerEdit());

        saveAnswer.getStyleClass().add("primary-button");
        saveAnswer.setOnAction(e -> saveAnswer());

        HBox toolbar = new HBox(9, pen, editorTitles, spacer(),
                answerMode, answerCount, cancelAnswer, expandAnswer, editAnswer, saveAnswer);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("editor-toolbar");

        VBox editorCard = new VBox(toolbar, content);
        editorCard.getStyleClass().add("editor-card");
        VBox.setVgrow(content, Priority.ALWAYS);

        VBox box = new VBox(10, questionBar, editorCard);
        box.getStyleClass().addAll("panel", "answer-panel-clean");
        box.setPadding(new Insets(14));
        box.setMinWidth(560);
        VBox.setVgrow(editorCard, Priority.ALWAYS);

        setAnswerEditMode(false);
        renderAnswer("", "选择左侧问题后，这里会展示你的整理内容。");
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
        loadQuestions(folder, null);
    }

    private void loadQuestions(Folder folder, String selectedId) {
        search.clear();
        if (folder == null) { clearQuestions(); return; }
        folderTitle.setText(folder.name());
        try {
            allItems.setAll(repo.questions(folder.id()));
            filter("");

            if (visibleItems.isEmpty()) {
                showAnswer(null);
                return;
            }

            int idx = 0;
            if (selectedId != null) {
                for (int i = 0; i < visibleItems.size(); i++) {
                    if (selectedId.equals(visibleItems.get(i).id())) {
                        idx = i;
                        break;
                    }
                }
            }
            questionList.getSelectionModel().select(idx);
        } catch (Exception e) { fail(e); }
    }

    private void filter(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (k.isEmpty()) visibleItems.setAll(allItems);
        else visibleItems.setAll(allItems.filtered(q ->
                q.question().toLowerCase(Locale.ROOT).contains(k)
                        || q.answer().toLowerCase(Locale.ROOT).contains(k)));
        questionCount.setText(k.isEmpty()
                ? visibleItems.size() + " 条记录"
                : visibleItems.size() + " / " + allItems.size() + " 条");
    }

    private void showAnswer(QaItem item) {
        setAnswerEditMode(false);
        boolean ok = item != null;

        currentQuestion.setText(ok ? item.question() : "先从左侧选择一个问题");
        if (ok) {
            answer.setText(item.answer());
            renderAnswer(item.answer(), "这条问题还没有整理答案，点击“编辑整理”开始记录。");
        } else {
            answer.setText("");
            renderAnswer("", "选择左侧问题后，这里会展示你的整理内容。");
        }

        editAnswer.setDisable(!ok);
        expandAnswer.setDisable(!ok);
        saveAnswer.setDisable(!ok);
    }

    private void beginAnswerEdit() {
        QaItem item = questionList.getSelectionModel().getSelectedItem();
        if (item == null) return;
        answer.setText(item.answer());
        setAnswerEditMode(true);
        answer.requestFocus();
        answer.positionCaret(answer.getLength());
    }

    private void cancelAnswerEdit() {
        QaItem item = questionList.getSelectionModel().getSelectedItem();
        if (item != null) {
            answer.setText(item.answer());
            renderAnswer(item.answer(), "这条问题还没有整理答案，点击“编辑整理”开始记录。");
        }
        setAnswerEditMode(false);
    }

    private void setAnswerEditMode(boolean editing) {
        editingAnswer = editing;

        answerPreview.setVisible(!editing);
        answerPreview.setManaged(!editing);
        answerEditorPane.setVisible(editing);
        answerEditorPane.setManaged(editing);

        editAnswer.setVisible(!editing);
        editAnswer.setManaged(!editing);
        expandAnswer.setVisible(!editing);
        expandAnswer.setManaged(!editing);
        saveAnswer.setVisible(editing);
        saveAnswer.setManaged(editing);
        cancelAnswer.setVisible(editing);
        cancelAnswer.setManaged(editing);
        answerCount.setVisible(editing);
        answerCount.setManaged(editing);

        answerMode.setText(editing ? "编辑模式" : "阅读模式");
        answerMode.getStyleClass().removeAll("mode-badge-edit", "mode-badge-read");
        answerMode.getStyleClass().add(editing ? "mode-badge-edit" : "mode-badge-read");

        // 编辑期间锁定导航，避免切换问题导致未保存内容丢失。
        folderList.setDisable(editing);
        questionList.setDisable(editing);
        search.setDisable(editing);
    }

    private void renderAnswer(String text, String emptyText) {
        answerPreview.getEngine().loadContent(AnswerRenderer.toHtml(text, emptyText, dataDir), "text/html");
    }

    private void pasteScreenshot(Clipboard clipboard) throws Exception {
        var image = clipboard.getImage();
        if (image == null) return;

        String file = assets.saveImage(image);
        String token = AssetService.imageToken(file, 80);

        int start = answer.getSelection().getStart();
        int end = answer.getSelection().getEnd();
        String all = answer.getText();

        String prefix = start > 0 && all.charAt(start - 1) != '\n' ? "\n" : "";
        String suffix = end < all.length() && all.charAt(end) != '\n' ? "\n" : "";
        String insert = prefix + token + suffix;

        answer.replaceText(start, end, insert);
        int tokenStart = start + prefix.length();
        activeImageAsset = file;
        answer.selectRange(tokenStart, tokenStart + token.length());
    }

    private void resizeCurrentImage(int delta) {
        try {
            ImageToken token = findCurrentImageToken();
            if (token == null) {
                Dialogs.info(window(), "先选择一张截图",
                        "把光标放到图片标记上，或刚粘贴截图后直接点击“图片 − / 图片 ＋”。");
                return;
            }

            int width = Math.max(20, Math.min(100, token.width() + delta));
            String replacement = AssetService.imageToken(token.file(), width);
            answer.replaceText(token.start(), token.end(), replacement);
            answer.selectRange(token.start(), token.start() + replacement.length());
            activeImageAsset = token.file();
        } catch (Exception e) {
            fail(e);
        }
    }

    private ImageToken findCurrentImageToken() {
        String text = answer.getText();
        int caret = answer.getCaretPosition();
        java.util.regex.Matcher m = AssetService.IMAGE_TOKEN.matcher(text);
        ImageToken fallback = null;

        while (m.find()) {
            String file = m.group(1);
            int width = Integer.parseInt(m.group(2));

            if (caret >= m.start() && caret <= m.end()) {
                return new ImageToken(m.start(), m.end(), file, width);
            }
            if (activeImageAsset != null && activeImageAsset.equals(file)) {
                fallback = new ImageToken(m.start(), m.end(), file, width);
            }
        }
        return fallback;
    }

    private void openAnswerWindow() {
        QaItem item = questionList.getSelectionModel().getSelectedItem();
        if (item == null) return;

        WebView web = new WebView();
        web.setContextMenuEnabled(false);
        web.getEngine().loadContent(
                AnswerRenderer.toHtml(item.answer(), "这条问题还没有整理答案。", dataDir),
                "text/html");

        Label question = new Label(item.question());
        question.setWrapText(true);
        question.getStyleClass().add("popup-question");

        Label hint = new Label("Esc 关闭窗口 · 可以使用系统窗口按钮继续最大化/还原");
        hint.getStyleClass().add("small-muted");

        VBox title = new VBox(3, question, hint);
        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("answer-popup-root");
        pane.setTop(title);
        pane.setCenter(web);
        BorderPane.setMargin(title, new Insets(16, 20, 12, 20));
        BorderPane.setMargin(web, new Insets(0, 14, 14, 14));

        Stage stage = new Stage();
        AppIcons.apply(stage);
        if (window() != null) stage.initOwner(window());
        stage.setTitle("QuestionHub · " + item.question());
        Scene scene = new Scene(pane, 1200, 820);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.show();
        stage.setMaximized(true);
    }

    private record ImageToken(int start, int end, String file, int width) {}

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
        if (!Dialogs.confirm(window(), "删除这个学习分类？",
                "“" + f.name() + "”中的全部问题和答案都会一起删除。此操作无法撤销。")) return;
        try { repo.deleteFolder(f.id()); reloadFolders(); }
        catch (Exception e) { fail(e); }
    }

    private void createQuestion() {
        Folder f = folderList.getSelectionModel().getSelectedItem();
        if (f == null) {
            Dialogs.info(window(), "先选择学习分类", "创建或选择一个分类后，再记录问题。这样以后复习时更容易找到。");
            return;
        }
        try {
            var r = Dialogs.question(window(), "记录一个问题", "", "");
            if (r.isEmpty()) return;
            String q = Validation.question(r.get().question());
            String a = Validation.answer(r.get().answer());
            long now = System.currentTimeMillis();
            QaItem created = new QaItem(Ids.create("qa"), f.id(), q, a, now, now);
            repo.upsertQuestion(created);
            loadQuestions(f, created.id());
        } catch (Exception e) { fail(e); }
    }

    private void editQuestion() {
        QaItem item = questionList.getSelectionModel().getSelectedItem();
        if (item == null || editingAnswer) return;
        try {
            var r = Dialogs.question(window(), "编辑学习记录", item.question(), item.answer());
            if (r.isEmpty()) return;
            QaItem updated = item.withContent(
                    Validation.question(r.get().question()),
                    Validation.answer(r.get().answer()),
                    System.currentTimeMillis());
            repo.upsertQuestion(updated);
            loadQuestions(folderList.getSelectionModel().getSelectedItem(), item.id());
        } catch (Exception e) { fail(e); }
    }

    private void deleteQuestion() {
        QaItem item = questionList.getSelectionModel().getSelectedItem();
        if (item == null || editingAnswer) return;
        if (!Dialogs.confirm(window(), "删除这条学习记录？",
                "确定删除“" + item.question() + "”吗？删除后无法恢复。")) return;
        try {
            repo.deleteQuestion(item.id());
            loadQuestions(folderList.getSelectionModel().getSelectedItem());
        } catch (Exception e) { fail(e); }
    }

    private void saveAnswer() {
        QaItem item = questionList.getSelectionModel().getSelectedItem();
        if (item == null) return;
        try {
            QaItem updated = item.withContent(
                    item.question(),
                    Validation.answer(answer.getText()),
                    System.currentTimeMillis());
            repo.upsertQuestion(updated);
            setAnswerEditMode(false);
            loadQuestions(folderList.getSelectionModel().getSelectedItem(), item.id());
        } catch (Exception e) { fail(e); }
    }

    private void importPackage() {
        if (editingAnswer) {
            Dialogs.info(window(), "请先保存当前整理", "正在编辑内容，请先保存或取消编辑后再导入数据包。");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("导入 QuestionHub 完整数据包");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("QuestionHub 数据包 (*.qhb, *.zip)", "*.qhb", "*.zip"));
        var f = fc.showOpenDialog(window());
        if (f == null) return;

        if (!Dialogs.confirm(window(), "导入并覆盖当前资料？",
                "数据包会完整替换当前分类和问答，并恢复其中的截图。建议先执行一次“备份数据包”。")) return;

        try {
            archive.importAndReplace(f.toPath());
            reloadFolders();
            Dialogs.info(window(), "数据包导入完成", "问题、答案和截图都已恢复，可以继续使用。");
        } catch (Exception e) {
            fail(e);
        }
    }

    private void exportPackage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("备份 QuestionHub 完整数据包");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("QuestionHub 数据包 (*.qhb)", "*.qhb"));
        fc.setInitialFileName("questionhub-backup-" + java.time.LocalDate.now() + ".qhb");
        var f = fc.showSaveDialog(window());
        if (f == null) return;

        try {
            Path target = f.toPath();
            if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".qhb")) {
                target = target.resolveSibling(target.getFileName() + ".qhb");
            }
            archive.exportTo(target);
            Dialogs.info(window(), "完整备份已保存",
                    "分类、问题、答案和截图已经打包到一个数据包中，可在其他电脑直接导入。");
        } catch (Exception e) {
            fail(e);
        }
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

    private Button softButton(String icon, String text) {
        Label i = new Label(icon);
        i.getStyleClass().add("button-icon");
        Label t = new Label(text);
        HBox box = new HBox(6, i, t);
        box.setAlignment(Pos.CENTER);
        Button b = new Button();
        b.setGraphic(box);
        b.getStyleClass().add("soft-button");
        return b;
    }

    private Window window() { return root.getScene() == null ? null : root.getScene().getWindow(); }
    private void fail(Throwable e) { Dialogs.error(window(), e); }
}
