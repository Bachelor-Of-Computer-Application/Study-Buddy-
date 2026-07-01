package com.studybuddy.utils;

import com.studybuddy.utils.SessionManager;
import com.studybuddy.App;
import com.studybuddy.models.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Handles view switching and stage loading.
 * All showXxx() methods declare throws IOException so callers
 * are forced to handle or propagate load failures.
 */
public class SceneManager {

    // Support static state stage reference
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Show the login page.
     *
     * @param stage Primary stage
     * @throws IOException if LoginView.fxml cannot be found or loaded
     */
    public static void showLoginPage(Stage stage) throws IOException {
        java.net.URL fxmlUrl = App.class.getResource("/com/studybuddy/fxml/LoginView.fxml");
        if (fxmlUrl == null) {
            throw new IOException(
                "[SceneManager] LoginView.fxml not found on classpath at " +
                "/com/studybuddy/fxml/LoginView.fxml — " +
                "ensure src/main/resources is marked as a Resource Root in IntelliJ."
            );
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        stage.setTitle("Study Buddy - Login");
        stage.setScene(new Scene(root));
        stage.show();
    }

    /**
     * Show the registration page.
     *
     * @param stage Primary stage
     * @throws IOException if RegisterView.fxml cannot be found or loaded
     */
    public static void showRegistrationPage(Stage stage) throws IOException {
        java.net.URL fxmlUrl = App.class.getResource("/com/studybuddy/fxml/RegisterView.fxml");
        if (fxmlUrl == null) {
            throw new IOException(
                "[SceneManager] RegisterView.fxml not found on classpath."
            );
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        stage.setTitle("Study Buddy - Register");
        stage.setScene(new Scene(root));
        stage.show();
    }

    /**
     * Show the home page and save the user to the application session state.
     *
     * @param stage Primary stage
     * @param user  Authenticated user
     * @throws IOException if HomeView.fxml cannot be found or loaded
     */
    public static void showHomePage(Stage stage, User user) throws IOException {
        // Set current user in App for session
        SessionManager.getInstance().login(user);

        java.net.URL fxmlUrl = App.class.getResource("/com/studybuddy/fxml/HomeView.fxml");
        if (fxmlUrl == null) {
            throw new IOException(
                "[SceneManager] HomeView.fxml not found on classpath."
            );
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        stage.setTitle("Study Buddy - Home");
        stage.setScene(new Scene(root));
        stage.show();
    }
}
