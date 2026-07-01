package com.studybuddy;

import com.studybuddy.dao.DatabaseConnection;
import com.studybuddy.utils.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {

        try {
            DatabaseConnection.initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SceneManager.setPrimaryStage(stage);

        try {
            SceneManager.showLoginPage(stage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to launch Login screen", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}