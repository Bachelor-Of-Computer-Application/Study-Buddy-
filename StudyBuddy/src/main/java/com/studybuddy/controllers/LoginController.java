package com.studybuddy.controllers;

import com.studybuddy.models.User;
import com.studybuddy.services.AuthService;
import com.studybuddy.utils.SceneManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller for the LoginView.fxml.
 */
public class LoginController implements Initializable {

    @FXML
    private BorderPane rootPane;

    @FXML
    private TextField emailInput;

    @FXML
    private PasswordField passwordInput;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        if (rootPane != null) {
            addFadeInAnimation(rootPane);
        }
    }

    /**
     * Handles login request.
     */
    @FXML
    public void onLoginButtonClick() {
        String email = emailInput.getText();
        String password = passwordInput.getText();

        // Validate credentials via AuthService
        User user = AuthService.login(email, password);

        if (user != null) {
            showSuccess("Login successful! Welcome, " + user.getName());
            // Redirect to Home Page
            try {
                App.setCurrentUser(user);
                SceneManager.showHomePage(getStage(), user);
            } catch (java.io.IOException e) {
                System.err.println("[LoginController] ❌ Could not load HomeView.fxml");
                e.printStackTrace();
                showError("Navigation error: " + e.getMessage());
            }
        } else {
            showError("Invalid email or password.");
        }
    }

    /**
     * Redirects to registration page.
     */
    @FXML
    public void onGoToRegistrationClick() {
        try {
            SceneManager.showRegistrationPage(getStage());
        } catch (java.io.IOException e) {
            System.err.println("[LoginController] ❌ Could not load RegisterView.fxml");
            e.printStackTrace();
            showError("Navigation error: " + e.getMessage());
        }
    }

    private Stage getStage() {
        return (Stage) emailInput.getScene().getWindow();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Adds fade-in transition when view loads.
     */
    private void addFadeInAnimation(Node node) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }
}