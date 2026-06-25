package com.studybuddy.utils;

import com.studybuddy.App;
import com.studybuddy.models.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

/**
 * Handles view switching and stage loading.
 */
public class SceneManager {

    /**
     * Show the registration page.
     *
     * @param stage Primary stage
     */
    public static void showRegistrationPage(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                App.class.getResource("/com/studybuddy/fxml/RegisterView.fxml")
            );
            Parent root = loader.load();
            stage.setTitle("Study Buddy - Register");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show the login page.
     *
     * @param stage Primary stage
     */
    public static void showLoginPage(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                App.class.getResource("/com/studybuddy/fxml/LoginView.fxml")
            );
            Parent root = loader.load();
            stage.setTitle("Study Buddy - Login");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show the home page and save the user to the application session state.
     *
     * @param stage Primary stage
     * @param user Authenticated user
     */
    public static void showHomePage(Stage stage, User user) {
        try {
            // Set current user in App for session
            App.setCurrentUser(user);

            FXMLLoader loader = new FXMLLoader(
                App.class.getResource("/com/studybuddy/fxml/HomeView.fxml")
            );
            Parent root = loader.load();
            stage.setTitle("Study Buddy - Home");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Support static state stage reference
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
