package com.studybuddy;

import com.studybuddy.dao.DatabaseConnection;
import com.studybuddy.models.User;
import com.studybuddy.utils.SceneManager;
import javafx.application.Application;
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
        // Initialize database connection (placeholder for MSSQL)
        DatabaseConnection.initialize();

        // Register stage in SceneManager
        SceneManager.setPrimaryStage(stage);

        // Show Login Page by default
        SceneManager.showLoginPage(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
