package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.admin.utils.AdminSceneManager;
import com.studybuddy.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminDashboardController {

    @FXML
    private StackPane adminContentArea;

    @FXML
    private Label adminNameLabel;

    @FXML
    private Label adminRoleLabel;

    @FXML
    private TextField globalSearchField;

    @FXML
    private Button btnOverview;

    @FXML
    private Button btnUsers;

    @FXML
    private Button btnNotes;

    @FXML
    private Button btnResources;

    @FXML
    private Button btnQuestions;

    @FXML
    private Button btnReports;

    @FXML
    private Button btnNotifications;

    @FXML
    private Button btnActivityLogs;

    @FXML
    private Button btnSettings;

    private Button activeButton;

    private final AdminService adminService = AdminService.getInstance();

    public AdminDashboardController() {
        System.out.println("========================================");
        System.out.println("AdminDashboardController Constructor");
        System.out.println("Controller ID = " + System.identityHashCode(this));
        System.out.println("========================================");
    }

    @FXML
    public void initialize() {

        System.out.println("AdminDashboardController.initialize()");
        System.out.println("Controller ID = " + System.identityHashCode(this));

        if (SessionManager.getCurrentAdmin() != null) {
            adminNameLabel.setText(SessionManager.getCurrentAdmin().getName());
            adminRoleLabel.setText(SessionManager.getCurrentAdmin().getRole());
        }

        if (globalSearchField != null) {
            globalSearchField.setOnAction(e -> handleGlobalSearch());
        }

        showOverview();
    }

    @FXML
    public void showOverview() {
        System.out.println("Opening Overview...");
        setActive(btnOverview);
        loadView("/com/studybuddy/admin/fxml/AdminDashboardOverview.fxml");
    }

    @FXML
    public void showUsers() {
        setActive(btnUsers);
        loadView("/com/studybuddy/admin/fxml/AdminUsersModeration.fxml");
    }

    @FXML
    public void showNotes() {
        setActive(btnNotes);
        loadView("/com/studybuddy/admin/fxml/AdminNotesModeration.fxml");
    }

    @FXML
    public void showResources() {
        setActive(btnResources);
        loadView("/com/studybuddy/admin/fxml/AdminResourcesModeration.fxml");
    }

    @FXML
    public void showQuestions() {
        setActive(btnQuestions);
        loadView("/com/studybuddy/admin/fxml/AdminQuestionsModeration.fxml");
    }

    @FXML
    public void showReports() {
        setActive(btnReports);
        loadView("/com/studybuddy/admin/fxml/AdminReports.fxml");
    }

    @FXML
    public void showNotifications() {
        setActive(btnNotifications);
        loadView("/com/studybuddy/admin/fxml/AdminNotifications.fxml");
    }

    @FXML
    public void showActivityLogs() {
        setActive(btnActivityLogs);
        loadView("/com/studybuddy/admin/fxml/AdminActivityLogs.fxml");
    }

    @FXML
    public void showSettings() {
        setActive(btnSettings);
        loadView("/com/studybuddy/admin/fxml/AdminSettings.fxml");
    }

    @FXML
    public void handleLogout() {

        adminService.logout();

        Stage stage = (Stage) adminContentArea.getScene().getWindow();

        AdminSceneManager.showLoginPage(stage);
    }

    @FXML
    public void handleGlobalSearch() {

        String query = globalSearchField == null ? "" : globalSearchField.getText().trim();

        if (query.isEmpty()) {
            return;
        }

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/studybuddy/admin/fxml/AdminGlobalSearch.fxml"));

            Parent root = loader.load();

            System.out.println("Loaded Global Search Controller = "
                    + loader.getController());

            adminContentArea.getChildren().setAll(root);

        } catch (IOException ex) {

            ex.printStackTrace();

            showOverview();
        }
    }

    private void loadView(String fxmlPath) {
        System.out.println("[DEBUG] FXML loading path: " + fxmlPath);
        if (fxmlPath.contains("AdminDashboardOverview.fxml")) {
            System.out.println("[DEBUG] AdminDashboardOverview.fxml loaded");
        }
        try {

            System.out.println("--------------------------------");
            System.out.println("Loading FXML : " + fxmlPath);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            Parent root = loader.load();

            Object controller = loader.getController();

            System.out.println("FXML Loaded Successfully");
            System.out.println("Controller = " + controller);

            if (controller != null) {
                System.out.println("Controller Class = " + controller.getClass().getName());
                System.out.println("Controller Hash = " + System.identityHashCode(controller));
            }

            adminContentArea.getChildren().clear();
            adminContentArea.getChildren().add(root);

            System.out.println("Children Count = "
                    + adminContentArea.getChildren().size());

            System.out.println("--------------------------------");

        } catch (Exception ex) {

            ex.printStackTrace();

            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    "Unable to load:\n" + fxmlPath + "\n\n" + ex.getMessage());

            alert.showAndWait();
        }
    }

    private void setActive(Button btn) {

        if (activeButton != null) {
            activeButton.getStyleClass().remove("active-nav");
        }

        if (btn != null) {
            btn.getStyleClass().add("active-nav");
            activeButton = btn;
        }
    }
}