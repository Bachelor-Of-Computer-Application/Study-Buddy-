package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.ActivityLogService;
import com.studybuddy.admin.services.SettingsService;
import com.studybuddy.models.Department;
import com.studybuddy.models.Semester;
import com.studybuddy.services.AcademicService;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Settings: app config + Department/Semester CRUD (all from database).
 */
public class AdminSettingsController {

    @FXML private TextField   appNameField;
    @FXML private CheckBox    maintenanceModeCheck;
    @FXML private TextField   maxUploadSizeField;
    @FXML private TextField   allowedFileTypesField;
    @FXML private TextField   storageDirectoryField;
    @FXML private ComboBox<Department> defaultDepartmentCombo;
    @FXML private ComboBox<Semester> defaultSemesterCombo;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    // Department management
    @FXML private TableView<Department> departmentsTable;
    @FXML private TableColumn<Department, String> deptNameCol;
    @FXML private TableColumn<Department, String> deptCodeCol;
    @FXML private TableColumn<Department, Boolean> deptActiveCol;
    @FXML private TextField deptNameField;
    @FXML private TextField deptCodeField;
    @FXML private TextArea deptDescField;

    // Semester management
    @FXML private TableView<Semester> semestersTable;
    @FXML private TableColumn<Semester, String> semNameCol;
    @FXML private TableColumn<Semester, Number> semNumberCol;
    @FXML private TableColumn<Semester, String> semDeptCol;
    @FXML private TableColumn<Semester, Boolean> semActiveCol;
    @FXML private ComboBox<Department> semDeptCombo;
    @FXML private TextField semNameField;
    @FXML private TextField semNumberField;
    @FXML private TextArea semDescField;

    private final SettingsService settingsService = SettingsService.getInstance();
    private final ActivityLogService logService = ActivityLogService.getInstance();
    private final AcademicService academicService = AcademicService.getInstance();
    private Department editingDept;
    private Semester editingSem;

    @FXML
    public void initialize() {
        setupDeptTable();
        setupSemTable();
        setupDefaultCombos();
        loadDepartments();
        loadSemesters();
        loadSettingsFromDB();
    }

    private void setupDefaultCombos() {
        defaultDepartmentCombo.setItems(FXCollections.observableArrayList(academicService.getAllActiveDepartments()));
        defaultDepartmentCombo.setOnAction(e -> {
            Department d = defaultDepartmentCombo.getValue();
            defaultSemesterCombo.getItems().clear();
            if (d != null) {
                defaultSemesterCombo.setItems(FXCollections.observableArrayList(
                        academicService.getSemestersByDepartment(d.getId())));
            }
        });
        if (semDeptCombo != null) {
            semDeptCombo.setItems(FXCollections.observableArrayList(academicService.getAllActiveDepartments()));
        }
    }

    private void setupDeptTable() {
        if (deptNameCol != null) deptNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (deptCodeCol != null) deptCodeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        if (deptActiveCol != null) deptActiveCol.setCellValueFactory(cell ->
                new SimpleBooleanProperty(cell.getValue().isActive()));
        if (departmentsTable != null) {
            departmentsTable.getSelectionModel().selectedItemProperty().addListener((o, a, d) -> {
                if (d != null) {
                    editingDept = d;
                    deptNameField.setText(d.getName());
                    deptCodeField.setText(d.getCode());
                    deptDescField.setText(d.getDescription());
                }
            });
        }
    }

    private void setupSemTable() {
        if (semNameCol != null) semNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (semNumberCol != null) semNumberCol.setCellValueFactory(new PropertyValueFactory<>("semesterNumber"));
        if (semDeptCol != null) semDeptCol.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        if (semActiveCol != null) semActiveCol.setCellValueFactory(cell ->
                new SimpleBooleanProperty(cell.getValue().isActive()));
        if (semestersTable != null) {
            semestersTable.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
                if (s != null) {
                    editingSem = s;
                    semNameField.setText(s.getName());
                    semNumberField.setText(String.valueOf(s.getSemesterNumber()));
                    semDescField.setText(s.getDescription());
                    semDeptCombo.setValue(academicService.getDepartmentById(s.getDepartmentId()));
                }
            });
        }
    }

    private void loadSettingsFromDB() {
        Map<String, String> settings = settingsService.getAllSettings();
        if (appNameField != null) appNameField.setText(settings.getOrDefault("app_name", "StudyBuddy"));
        if (maintenanceModeCheck != null) maintenanceModeCheck.setSelected("true".equalsIgnoreCase(settings.getOrDefault("maintenance_mode", "false")));
        if (maxUploadSizeField != null) maxUploadSizeField.setText(settings.getOrDefault("max_upload_size_mb", "50"));
        if (allowedFileTypesField != null) allowedFileTypesField.setText(settings.getOrDefault("allowed_file_types", "pdf,docx,pptx,ppt,zip,xlsx,jpg,png,jpeg"));
        if (storageDirectoryField != null) storageDirectoryField.setText(settings.getOrDefault("storage_directory", ""));
        loadDefaultDeptSemFromSettings(settings);
    }

    private void loadDefaultDeptSemFromSettings(Map<String, String> settings) {
        String deptName = settings.get("default_department");
        if (deptName != null && !deptName.isBlank() && defaultDepartmentCombo != null) {
            for (Department d : defaultDepartmentCombo.getItems()) {
                if (d.getName().equalsIgnoreCase(deptName.trim())) {
                    defaultDepartmentCombo.setValue(d);
                    if (defaultSemesterCombo != null) {
                        defaultSemesterCombo.setItems(FXCollections.observableArrayList(
                                academicService.getSemestersByDepartment(d.getId())));
                    }
                    break;
                }
            }
        }
        String semVal = settings.get("default_semester");
        if (semVal != null && !semVal.isBlank() && defaultSemesterCombo != null) {
            for (Semester s : defaultSemesterCombo.getItems()) {
                if (String.valueOf(s.getSemesterNumber()).equals(semVal.trim())
                        || s.getName().equalsIgnoreCase(semVal.trim())) {
                    defaultSemesterCombo.setValue(s);
                    break;
                }
            }
        }
    }

    private void loadDepartments() {
        if (departmentsTable != null) {
            departmentsTable.setItems(FXCollections.observableArrayList(academicService.getAllDepartments()));
        }
        setupDefaultCombos();
    }

    private void loadSemesters() {
        if (semestersTable != null) {
            semestersTable.setItems(FXCollections.observableArrayList(academicService.getAllSemesters()));
        }
    }

    @FXML public void handleSave() {
        try { Integer.parseInt(maxUploadSizeField.getText().trim()); }
        catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Maximum upload size must be a number.");
            return;
        }
        if (confirm("Save all settings?")) {
            Map<String, String> settings = buildSettingsMap();
            if (settingsService.saveAllSettings(settings)) {
                logService.logAction("Settings Updated", "Settings", "General");
                showAlert(Alert.AlertType.INFORMATION, "Settings saved successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed to save settings. Run migration_v2.sql first.");
            }
        }
    }

    @FXML public void handleReset() {
        if (confirm("Reset settings to defaults?")) {
            appNameField.setText("StudyBuddy");
            maintenanceModeCheck.setSelected(false);
            maxUploadSizeField.setText("50");
            allowedFileTypesField.setText("pdf,docx,pptx,ppt,zip,xlsx,jpg,png,jpeg");
            storageDirectoryField.clear();
            showAlert(Alert.AlertType.INFORMATION, "Reset to defaults. Click Save to persist.");
        }
    }

    @FXML public void handleAddDepartment() {
        try {
            Department d = new Department();
            d.setName(deptNameField.getText().trim());
            d.setCode(deptCodeField.getText().trim().toUpperCase());
            d.setDescription(deptDescField.getText());
            d.setActive(true);
            academicService.createDepartment(d);
            clearDeptForm();
            loadDepartments();
            showAlert(Alert.AlertType.INFORMATION, "Department added.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML public void handleUpdateDepartment() {
        if (editingDept == null) { showAlert(Alert.AlertType.WARNING, "Select a department."); return; }
        try {
            editingDept.setName(deptNameField.getText().trim());
            editingDept.setCode(deptCodeField.getText().trim().toUpperCase());
            editingDept.setDescription(deptDescField.getText());
            academicService.updateDepartment(editingDept);
            loadDepartments();
            showAlert(Alert.AlertType.INFORMATION, "Department updated.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML public void handleDeleteDepartment() {
        if (editingDept == null) return;
        if (confirm("Delete department '" + editingDept.getName() + "'?")) {
            try {
                academicService.deleteDepartment(editingDept.getId());
                clearDeptForm();
                loadDepartments();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, e.getMessage());
            }
        }
    }

    @FXML public void handleToggleDepartment() {
        if (editingDept == null) return;
        try {
            academicService.setDepartmentActive(editingDept.getId(), !editingDept.isActive());
            loadDepartments();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML public void handleAddSemester() {
        if (semDeptCombo.getValue() == null) { showAlert(Alert.AlertType.WARNING, "Select department."); return; }
        try {
            Semester s = new Semester();
            s.setDepartmentId(semDeptCombo.getValue().getId());
            s.setName(semNameField.getText().trim());
            s.setSemesterNumber(Integer.parseInt(semNumberField.getText().trim()));
            s.setDescription(semDescField.getText());
            s.setActive(true);
            academicService.createSemester(s);
            clearSemForm();
            loadSemesters();
            showAlert(Alert.AlertType.INFORMATION, "Semester added.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML public void handleUpdateSemester() {
        if (editingSem == null) return;
        try {
            editingSem.setName(semNameField.getText().trim());
            editingSem.setSemesterNumber(Integer.parseInt(semNumberField.getText().trim()));
            editingSem.setDescription(semDescField.getText());
            if (semDeptCombo.getValue() != null) editingSem.setDepartmentId(semDeptCombo.getValue().getId());
            academicService.updateSemester(editingSem);
            loadSemesters();
            showAlert(Alert.AlertType.INFORMATION, "Semester updated.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML public void handleDeleteSemester() {
        if (editingSem == null) return;
        if (confirm("Delete semester '" + editingSem.getName() + "'?")) {
            try {
                academicService.deleteSemester(editingSem.getId());
                clearSemForm();
                loadSemesters();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, e.getMessage());
            }
        }
    }

    @FXML public void handleToggleSemester() {
        if (editingSem == null) return;
        try {
            academicService.setSemesterActive(editingSem.getId(), !editingSem.isActive());
            loadSemesters();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML public void handleChangePassword() {
        if (currentPasswordField == null) return;
        String current = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();
        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "All password fields required."); return;
        }
        if (newPass.length() < 6) { showAlert(Alert.AlertType.WARNING, "Password min 6 chars."); return; }
        if (!newPass.equals(confirm)) { showAlert(Alert.AlertType.ERROR, "Passwords don't match."); return; }
        com.studybuddy.models.User admin = com.studybuddy.utils.SessionManager.getCurrentAdmin();
        if (admin != null) {
            boolean ok = com.studybuddy.utils.PasswordHasher.verifyPassword(current, admin.getPassword()) || current.equals(admin.getPassword());
            if (!ok) { showAlert(Alert.AlertType.ERROR, "Current password incorrect."); return; }
            com.studybuddy.admin.services.AdminService.getInstance().resetPassword(admin.getId(), admin.getEmail(), newPass);
        }
        currentPasswordField.clear(); newPasswordField.clear(); confirmPasswordField.clear();
        showAlert(Alert.AlertType.INFORMATION, "Password changed.");
    }

    private void clearDeptForm() {
        editingDept = null;
        deptNameField.clear(); deptCodeField.clear(); deptDescField.clear();
    }

    private void clearSemForm() {
        editingSem = null;
        semNameField.clear(); semNumberField.clear(); semDescField.clear();
    }

    private Map<String, String> buildSettingsMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("app_name", appNameField.getText().trim());
        m.put("maintenance_mode", String.valueOf(maintenanceModeCheck.isSelected()));
        m.put("max_upload_size_mb", maxUploadSizeField.getText().trim());
        m.put("allowed_file_types", allowedFileTypesField.getText().trim());
        m.put("storage_directory", storageDirectoryField != null ? storageDirectoryField.getText().trim() : "");
        if (defaultDepartmentCombo.getValue() != null) {
            m.put("default_department", defaultDepartmentCombo.getValue().getName());
        }
        if (defaultSemesterCombo.getValue() != null) {
            m.put("default_semester", String.valueOf(defaultSemesterCombo.getValue().getSemesterNumber()));
        }
        return m;
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg); a.setHeaderText(null); a.showAndWait();
    }
}
