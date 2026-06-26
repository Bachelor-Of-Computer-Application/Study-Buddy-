package com.studybuddy.admin.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminSettingsController {

    @FXML private ComboBox<String> themeComboBox;
    @FXML private CheckBox systemMailsCheck;
    @FXML private CheckBox realTimeAlertsCheck;

    @FXML private TextField adminNameField;
    @FXML private TextField adminEmailField;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML
    public void initialize() {
        themeComboBox.setItems(FXCollections.observableArrayList("Academic Indigo Light", "Administrative Slate Dark"));
        themeComboBox.setValue("Academic Indigo Light");
    }

    @FXML
    public void saveGeneralSettings() {
        showSuccess("Preferences Saved", "General application settings have been updated successfully.");
    }

    @FXML
    public void saveProfileInfo() {
        String name = adminNameField.getText().trim();
        String email = adminEmailField.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showError("Input Error", "Name and email cannot be left blank.");
            return;
        }

        showSuccess("Profile Updated", "Profile information for " + name + " updated successfully.");
    }

    @FXML
    public void changePassword() {
        String current = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showError("Input Error", "All password fields are required.");
            return;
        }

        if (!newPass.equals(confirm)) {
            showError("Validation Error", "New passwords do not match.");
            return;
        }

        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        showSuccess("Password Changed", "Security credentials modified successfully.");
    }

    private void showSuccess(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
