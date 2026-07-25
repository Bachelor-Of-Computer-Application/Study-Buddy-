package com.studybuddy.controllers;

import com.studybuddy.models.RegisterInput;
import com.studybuddy.services.AuthService;
import com.studybuddy.utils.SceneManager;
import com.studybuddy.utils.ValidationUtil;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
 * Controller for the RegisterView.fxml.
 */
public class RegisterController implements Initializable {

    @FXML
    private BorderPane rootPane;

    @FXML
    private TextField nameInput;

    @FXML
    private TextField emailInput;

    @FXML
    private PasswordField passwordInput;

    @FXML
    private PasswordField confirmPasswordInput;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        if (rootPane != null) {
            addFadeInAnimation(rootPane);
        }
    }

    /**
     * Handles registration request.
     */
    @FXML
    public void onRegisterButtonClick() {
        RegisterInput input = new RegisterInput(
            nameInput.getText(),
            emailInput.getText(),
            passwordInput.getText(),
            confirmPasswordInput.getText()
        );

        // Validate input using ValidationUtil
        if (!ValidationUtil.isValidRegistration(input)) {
            showError("Invalid input");
            return;
        }

        // Register user via AuthService
        boolean success = AuthService.registerUser(input.getName(), input.getEmail(), input.getPassword());

        if (success) {
            showSuccess("Registration successful! Please login.");
            // Auto-redirect to Login Page after 1 second
            Timeline redirect = new Timeline(
                new KeyFrame(Duration.seconds(1),
                    new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent e) {
                            try {
                                SceneManager.showLoginPage(getStage());
                            } catch (java.io.IOException ex) {
                                java.util.logging.Logger.getLogger(RegisterController.class.getName()).warning("[RegisterController] ❌ Could not load LoginView.fxml");
                                java.util.logging.Logger.getLogger(RegisterController.class.getName()).log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                            }
                        }
                    })
            );
            redirect.play();
        } else {
            showError("Registration failed. User may already exist.");
        }
    }

    /**
     * Redirects to login page.
     */
    @FXML
    public void onGoToRegistrationClick() {
        try {
            SceneManager.showLoginPage(getStage());
        } catch (java.io.IOException e) {
            java.util.logging.Logger.getLogger(RegisterController.class.getName()).warning("[RegisterController] ❌ Could not load LoginView.fxml");
            java.util.logging.Logger.getLogger(RegisterController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
