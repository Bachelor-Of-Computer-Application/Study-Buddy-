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

/**
 * Main dashboard shell controller.
 * Manages the sidebar navigation, active-button highlighting, global search,
 * and dynamic content area.
 */
public class AdminDashboardController {

    @FXML private StackPane adminContentArea;
    @FXML private Label     adminNameLabel;
    @FXML private Label     adminRoleLabel;
    @FXML private TextField globalSearchField;

    // Sidebar nav buttons (for active-state tracking)
    @FXML private Button btnOverview;
    @FXML private Button btnUsers;
    @FXML private Button btnNotes;
    @FXML private Button btnResources;
    @FXML private Button btnQuestions;
    @FXML private Button btnReports;
    @FXML private Button btnNotifications;
    @FXML private Button btnActivityLogs;
    @FXML private Button btnSettings;

    private Button activeButton;
    private final AdminService adminService = AdminService.getInstance();

    @FXML
    public void initialize() {
        // Populate admin info from session
        if (SessionManager.getCurrentAdmin() != null) {
            adminNameLabel.setText(SessionManager.getCurrentAdmin().getName());
            adminRoleLabel.setText(SessionManager.getCurrentAdmin().getRole());
        }

        // Global search on Enter
        if (globalSearchField != null) {
            globalSearchField.setOnAction(e -> handleGlobalSearch());
        }

        // Load default view
        showOverview();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML public void showOverview() {
        setActive(btnOverview);
        loadView("/com/studybuddy/admin/fxml/AdminDashboardOverview.fxml");
    }

    @FXML public void showUsers() {
        setActive(btnUsers);
        loadView("/com/studybuddy/admin/fxml/AdminUsersModeration.fxml");
    }

    @FXML public void showNotes() {
        setActive(btnNotes);
        loadView("/com/studybuddy/admin/fxml/AdminNotesModeration.fxml");
    }

    @FXML public void showResources() {
        setActive(btnResources);
        loadView("/com/studybuddy/admin/fxml/AdminResourcesModeration.fxml");
    }

    @FXML public void showQuestions() {
        setActive(btnQuestions);
        loadView("/com/studybuddy/admin/fxml/AdminQuestionsModeration.fxml");
    }

    @FXML public void showReports() {
        setActive(btnReports);
        loadView("/com/studybuddy/admin/fxml/AdminReports.fxml");
    }

    @FXML public void showNotifications() {
        setActive(btnNotifications);
        loadView("/com/studybuddy/admin/fxml/AdminNotifications.fxml");
    }

    @FXML public void showActivityLogs() {
        setActive(btnActivityLogs);
        loadView("/com/studybuddy/admin/fxml/AdminActivityLogs.fxml");
    }

    @FXML public void showSettings() {
        setActive(btnSettings);
        loadView("/com/studybuddy/admin/fxml/AdminSettings.fxml");
    }

    @FXML
    public void handleLogout() {
        adminService.logout();
        Stage stage = (Stage) adminContentArea.getScene().getWindow();
        AdminSceneManager.showLoginPage(stage);
    }

    // ── Global Search ─────────────────────────────────────────────────────────

    @FXML
    public void handleGlobalSearch() {
        String query = globalSearchField != null ? globalSearchField.getText().trim() : "";
        if (query.isEmpty()) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/studybuddy/admin/fxml/AdminGlobalSearch.fxml"));
            // If global search FXML exists, pass query to its controller
            // For now fall back gracefully
            Parent view = loader.load();
            adminContentArea.getChildren().setAll(view);
        } catch (IOException e) {
            // Global search page not yet present – show overview
            showOverview();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            adminContentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Could not load view: " + fxmlPath + "\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    /** Apply active style to the clicked sidebar button. */
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
