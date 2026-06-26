package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.admin.utils.AdminSceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Controller for the admin login screen.
 * On success: stores admin in SessionManager and navigates to the dashboard.
 */
public class AdminLoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;

    private final AdminService adminService = AdminService.getInstance();

    @FXML
    public void initialize() {
        // Allow pressing Enter in password field to submit
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Signing in…");

        if (adminService.validateAdminLogin(username, password)) {
            hideError();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            AdminSceneManager.showDashboardPage(stage);
        } else {
            showError("Invalid credentials or insufficient privileges.");
            loginButton.setDisable(false);
            loginButton.setText("Sign In");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
