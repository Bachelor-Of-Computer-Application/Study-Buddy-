package com.studybuddy.admin;

import com.studybuddy.admin.utils.AdminSceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class AdminApp extends Application {
    @Override
    public void start(Stage stage) {
        AdminSceneManager.setPrimaryStage(stage);
        AdminSceneManager.showLoginPage(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
