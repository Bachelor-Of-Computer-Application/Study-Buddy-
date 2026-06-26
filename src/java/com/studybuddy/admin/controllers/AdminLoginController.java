package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.admin.utils.AdminSceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AdminLoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AdminService adminService = AdminService.getInstance();

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (adminService.validateAdminLogin(username, password)) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            AdminSceneManager.showDashboardPage(stage);
        } else {
            errorLabel.setText("Invalid admin credentials!");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
}
