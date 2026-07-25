package com.studybuddy.admin;

import com.studybuddy.admin.utils.AdminSceneManager;
import java.util.Objects;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class AdminApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.getIcons().add(new Image(
                Objects.requireNonNull(
                        getClass().getResourceAsStream("/com/studybuddy/images/main_logo_study_buddyy.png")
                )
        ));

        AdminSceneManager.setPrimaryStage(stage);
        AdminSceneManager.showLoginPage(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
