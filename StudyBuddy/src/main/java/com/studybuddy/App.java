package com.studybuddy;

import com.studybuddy.dao.DatabaseConnection;
import com.studybuddy.models.User;
import com.studybuddy.utils.SceneManager;
import java.util.Objects;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Main application class extending Application to manage JavaFX life cycle.
 */
public class App extends Application {
    private static User currentUser;

    /**
     * Gets the currently logged in user session.
     *
     * @return current user
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the currently logged in user session.
     *
     * @param user current user
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    @Override
    public void start(Stage stage) {
        stage.getIcons().add(new Image(
                Objects.requireNonNull(
                        getClass().getResourceAsStream("/com/studybuddy/images/main_logo_study_buddyy.png")
                )
        ));

        // Attempt database connection — failure must NOT prevent the UI from showing
        try {
            DatabaseConnection.initialize();
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(App.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        // Register stage in SceneManager
        SceneManager.setPrimaryStage(stage);

        // Show Login Page — this MUST succeed; any exception here is fatal and rethrown
        try {
            SceneManager.showLoginPage(stage);
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(App.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Failed to launch Login screen", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
