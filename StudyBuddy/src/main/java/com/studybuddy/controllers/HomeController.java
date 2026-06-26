package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.User;
import com.studybuddy.services.AuthService;
import com.studybuddy.services.TaskService;
import com.studybuddy.utils.SceneManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller for the HomeView.fxml.
 */
public class HomeController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private StackPane contentArea;

    @FXML
    private Text usernameLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private Text totalTasksCount;

    @FXML
    private Text completedTasksCount;

    @FXML
    private Text studyHoursCount;

    private User currentUser;
    private final TaskService taskService = new TaskService();

    @FXML
    public void initialize() {
        if (rootPane != null) {
            addFadeInAnimation(rootPane);
        }

        // Get current user from App/session
        currentUser = App.getCurrentUser();

        if (currentUser != null) {
            // Display user info
            if (usernameLabel != null) {
                usernameLabel.setText(currentUser.getName());
            }
            if (emailLabel != null) {
                emailLabel.setText(currentUser.getEmail());
            }
        }

        // Load dashboard data (placeholder for MSSQL future binding)
        loadDashboardStats();
        loadRecentTasks();
        loadStudyProgress();
    }

    // TODO: Bind dashboard stats to real database data
    // TODO: Implement role-based access (admin/user)

    private void loadDashboardStats() {
        int userId = (currentUser != null) ? currentUser.getId() : 0;
        
        if (totalTasksCount != null) {
            int total = taskService.getTotalTaskCount(userId);
            int completed = taskService.getCompletedTaskCount(userId);
            totalTasksCount.setText(completed + " / " + total);
        }
        if (completedTasksCount != null) {
            int progress = taskService.getCompletedPercentage(userId);
            completedTasksCount.setText(progress + "%");
        }
        if (studyHoursCount != null) {
            double hours = taskService.getStudyHours(userId);
            studyHoursCount.setText(hours + "h");
        }
    }

    private void loadRecentTasks() {
        // Placeholder stub for future tasks binding
        System.out.println("⚠️ loadRecentTasks() stub - MSSQL pending");
    }

    private void loadStudyProgress() {
        // Placeholder stub for future progress binding
        System.out.println("⚠️ loadStudyProgress() stub - MSSQL pending");
    }

    @FXML
    public void goToDashboard() {
        loadCenterView("/com/studybuddy/fxml/DashboardView.fxml");
    }
    
    @FXML
    public void goToNotes() {
        loadCenterView("/com/studybuddy/fxml/NotesView.fxml");
    }

    @FXML
    public void goToQuestions() {
        loadCenterView("/com/studybuddy/fxml/QuestionsView.fxml");
    }

    @FXML
    public void goToResources() {
        loadCenterView("/com/studybuddy/fxml/ResourcesView.fxml");
    }

    @FXML
    public void goToProgress() {
        System.out.println("Navigation to Progress");
    }

    @FXML
    public void goToProfile() {
        // Load ProfileView inside the content area
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/studybuddy/fxml/ProfileView.fxml"));
            Parent view = loader.load();
            
            // Pass currentUser to ProfileController if needed
            ProfileController controller = loader.getController();
            if (controller != null) {
                // ProfileController initializes profile info by reading from App.getCurrentUser() 
                // but we can call a refresh or similar if needed.
            }
            
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            showAlert("Navigation Error", "Could not navigate to Profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void onLogoutClick() {
        AuthService.logout();
        SceneManager.showLoginPage(getStage());
    }

    private void loadCenterView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            showAlert("Navigation Error", "Could not navigate to " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Stage getStage() {
        return (Stage) rootPane.getScene().getWindow();
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