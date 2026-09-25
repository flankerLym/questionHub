package com.questionhub.desktop;

import com.questionhub.desktop.db.ArchiveRepository;
import com.questionhub.desktop.db.Database;
import com.questionhub.desktop.service.*;
import com.questionhub.desktop.ui.LoginView;
import com.questionhub.desktop.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;

public final class QuestionHubApp extends Application {
    private Stage stage;
    private Path dataDir;
    private ArchiveRepository repo;
    private ArchiveJsonService json;

    @Override public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        stage.setTitle("QuestionHub · 学习问题库");
        stage.setMinWidth(1080);
        stage.setMinHeight(700);
        dataDir = AppPaths.resolveDataDir(stage);
        Database db = new Database(dataDir);
        BackupService.backupIfNeeded(db.dbFile(), dataDir);
        db.init();
        repo = new ArchiveRepository(db);
        json = new ArchiveJsonService(repo);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        LoginView view = new LoginView(new AuthService(), this::showMain);
        setScene(view.root(), 1080, 720);
    }

    private void showMain() {
        MainView view = new MainView(repo, json, dataDir, this::showLogin);
        setScene(view.root(), 1380, 860);
    }

    private void setScene(javafx.scene.Parent root, double w, double h) {
        Scene scene = new Scene(root, w, h);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    public static void main(String[] args) { launch(args); }
}
