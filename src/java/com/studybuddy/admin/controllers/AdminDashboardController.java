package com.studybuddy.admin.controllers;

import com.studybuddy.admin.utils.AdminSceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminDashboardController {

    @FXML private StackPane adminContentArea;

    @FXML
    public void initialize() {
        showOverview();
    }

    @FXML
    public void showOverview() {
        loadView("/com/studybuddy/admin/fxml/AdminDashboardOverview.fxml");
    }

    @FXML
    public void showUsers() {
        loadView("/com/studybuddy/admin/fxml/AdminUsersModeration.fxml");
    }

    @FXML
    public void showNotes() {
        loadView("/com/studybuddy/admin/fxml/AdminNotesModeration.fxml");
    }

    @FXML
    public void showResources() {
        loadView("/com/studybuddy/admin/fxml/AdminResourcesModeration.fxml");
    }

    @FXML
    public void showQuestions() {
        loadView("/com/studybuddy/admin/fxml/AdminQuestionsModeration.fxml");
    }

    @FXML
    public void showReports() {
        loadView("/com/studybuddy/admin/fxml/AdminReports.fxml");
    }

    @FXML
    public void showSettings() {
        loadView("/com/studybuddy/admin/fxml/AdminSettings.fxml");
    }

    @FXML
    public void handleLogout() {
        Stage stage = (Stage) adminContentArea.getScene().getWindow();
        AdminSceneManager.showLoginPage(stage);
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            adminContentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not load view: " + fxmlPath + "\nError: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
