package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.ActivityLogService;
import com.studybuddy.admin.services.SettingsService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Settings controller: load from DB on init, save back to DB on Save.
 * Manages: app name, maintenance mode, max upload size, allowed file types,
 * default semester, default department.
 */
public class AdminSettingsController {

    @FXML private TextField   appNameField;
    @FXML private CheckBox    maintenanceModeCheck;
    @FXML private TextField   maxUploadSizeField;
    @FXML private TextField   allowedFileTypesField;
    @FXML private ComboBox<String> defaultSemesterCombo;
    @FXML private ComboBox<String> defaultDepartmentCombo;

    // Password change section
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    private final SettingsService  settingsService  = SettingsService.getInstance();
    private final ActivityLogService logService     = ActivityLogService.getInstance();

    @FXML
    public void initialize() {
        setupCombos();
        loadSettingsFromDB();
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private void loadSettingsFromDB() {
        Map<String, String> settings = settingsService.getAllSettings();

        if (appNameField          != null) appNameField.setText(settings.getOrDefault("app_name", "StudyBuddy"));
        if (maintenanceModeCheck  != null) maintenanceModeCheck.setSelected("true".equalsIgnoreCase(settings.getOrDefault("maintenance_mode", "false")));
        if (maxUploadSizeField    != null) maxUploadSizeField.setText(settings.getOrDefault("max_upload_size_mb", "50"));
        if (allowedFileTypesField != null) allowedFileTypesField.setText(settings.getOrDefault("allowed_file_types", "pdf,docx,pptx,xlsx,jpg,png"));

        String sem  = settings.getOrDefault("default_semester", "1");
        String dept = settings.getOrDefault("default_department", "Computer Science");
        if (defaultSemesterCombo   != null) defaultSemesterCombo.setValue(sem);
        if (defaultDepartmentCombo != null) defaultDepartmentCombo.setValue(dept);
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @FXML
    public void handleSave() {
        // Validate numeric fields
        String maxSize = maxUploadSizeField != null ? maxUploadSizeField.getText().trim() : "50";
        try { Integer.parseInt(maxSize); }
        catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Maximum upload size must be a number (e.g. 50).");
            return;
        }

        if (confirm("Save all settings?")) {
            Map<String, String> settings = buildSettingsMap();
            boolean ok = settingsService.saveAllSettings(settings);
            if (ok) {
                logService.logAction("Settings Updated", "Settings", "General Settings");
                showAlert(Alert.AlertType.INFORMATION, "Settings saved successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR,
                        "Failed to save settings. Check if the Settings table exists in your database.\n\n" +
                        "Create it with:\nCREATE TABLE Settings (\n  setting_key NVARCHAR(100) PRIMARY KEY,\n  setting_value NVARCHAR(MAX),\n  updated_at DATETIME2 DEFAULT GETDATE()\n);");
            }
        }
    }

    @FXML
    public void handleReset() {
        if (confirm("Reset all settings to defaults?")) {
            if (appNameField          != null) appNameField.setText("StudyBuddy");
            if (maintenanceModeCheck  != null) maintenanceModeCheck.setSelected(false);
            if (maxUploadSizeField    != null) maxUploadSizeField.setText("50");
            if (allowedFileTypesField != null) allowedFileTypesField.setText("pdf,docx,pptx,xlsx,jpg,png");
            if (defaultSemesterCombo   != null) defaultSemesterCombo.setValue("1");
            if (defaultDepartmentCombo != null) defaultDepartmentCombo.setValue("Computer Science");
            showAlert(Alert.AlertType.INFORMATION, "Settings reset to defaults. Click Save to persist.");
        }
    }

    // ── Password Change ───────────────────────────────────────────────────────

    @FXML
    public void handleChangePassword() {
        if (currentPasswordField == null || newPasswordField == null || confirmPasswordField == null) return;

        String current  = currentPasswordField.getText();
        String newPass  = newPasswordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "All password fields are required."); return;
        }
        if (newPass.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "New password must be at least 6 characters."); return;
        }
        if (!newPass.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "New passwords do not match."); return;
        }

        // Verify current password via SessionManager + PasswordHasher
        com.studybuddy.models.User admin = com.studybuddy.utils.SessionManager.getCurrentAdmin();
        if (admin != null) {
            boolean currentValid = com.studybuddy.utils.PasswordHasher.verifyPassword(current, admin.getPassword())
                    || current.equals(admin.getPassword());
            if (!currentValid) { showAlert(Alert.AlertType.ERROR, "Current password is incorrect."); return; }
            String hashed = com.studybuddy.utils.PasswordHasher.hashPassword(newPass);
            com.studybuddy.admin.services.AdminService.getInstance().resetPassword(admin.getId(), admin.getEmail(), newPass);
        }

        currentPasswordField.clear(); newPasswordField.clear(); confirmPasswordField.clear();
        logService.logAction("Admin Password Changed", "Settings", "Security");
        showAlert(Alert.AlertType.INFORMATION, "Password changed successfully.");
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupCombos() {
        if (defaultSemesterCombo != null) defaultSemesterCombo.setItems(
                FXCollections.observableArrayList("1","2","3","4","5","6","7","8"));
        if (defaultDepartmentCombo != null) defaultDepartmentCombo.setItems(
                FXCollections.observableArrayList(
                        "Computer Science", "Information Technology",
                        "Business Administration", "Mathematics",
                        "Physics", "Engineering", "Other"));
    }

    private Map<String, String> buildSettingsMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("app_name",           appNameField          != null ? appNameField.getText().trim() : "StudyBuddy");
        m.put("maintenance_mode",   maintenanceModeCheck  != null ? String.valueOf(maintenanceModeCheck.isSelected()) : "false");
        m.put("max_upload_size_mb", maxUploadSizeField    != null ? maxUploadSizeField.getText().trim() : "50");
        m.put("allowed_file_types", allowedFileTypesField != null ? allowedFileTypesField.getText().trim() : "pdf,docx,pptx");
        m.put("default_semester",   defaultSemesterCombo   != null && defaultSemesterCombo.getValue()   != null ? defaultSemesterCombo.getValue()   : "1");
        m.put("default_department", defaultDepartmentCombo != null && defaultDepartmentCombo.getValue() != null ? defaultDepartmentCombo.getValue() : "Computer Science");
        return m;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg); a.setHeaderText(null); a.showAndWait();
    }
}
