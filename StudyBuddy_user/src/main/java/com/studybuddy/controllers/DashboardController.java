package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;
import com.studybuddy.models.User;
import com.studybuddy.services.DashboardService;
import com.studybuddy.services.NoteService;
import com.studybuddy.services.QuestionService;
import com.studybuddy.services.ResourceService;
import com.studybuddy.services.TaskService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private Label welcomeLabel;
    @FXML private Label lblTotalNotes;
    @FXML private Label lblSharedResources;
    @FXML private Label lblTotalQuestions;
    @FXML private Label lblTotalTasks;
    @FXML private Label lblProgressPercentageStat;
    @FXML private BarChart<String, Number> studyBarChart;
    @FXML private Label lblProgressPercentage;
    @FXML private ProgressBar overallProgressBar;
    @FXML private ComboBox<String> subjectComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private TextField noteSearchField;
    @FXML private FlowPane notesFlowPane;
    @FXML private HBox recentNotesContainer;
    @FXML private HBox recentResourcesContainer;

    private final DashboardService dashboardService = DashboardService.getInstance();
    private final NoteService noteService = new NoteService();
    private final ResourceService resourceService = new ResourceService();
    private final QuestionService questionService = new QuestionService();
    private final TaskService taskService = new TaskService();

    private List<Note> approvedNotes = List.of();
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = App.getCurrentUser();
        initializeComboBoxes();
        refreshDashboard();
        setupListeners();
    }

    private void initializeComboBoxes() {
        sortComboBox.setItems(FXCollections.observableArrayList("Newest", "Oldest"));
        sortComboBox.setValue("Newest");

        subjectComboBox.setItems(FXCollections.observableArrayList(
                "Physics", "Computer Science", "Mathematics", "Chemistry",
                "Civil Engineering", "Electrical Engineering", "Mechanical",
                "Architecture", "Biology", "Economics", "Business", "Programming"
        ));
    }

    @FXML
    public void refreshDashboard() {
        currentUser = App.getCurrentUser();
        welcomeLabel.setText(currentUser != null
                ? "Welcome Back, " + currentUser.getName() + "!"
                : "Welcome Back!");

        loadDashboardStats();
        loadCharts();
        loadApprovedNotes();
        loadRecentAndTrending();
    }

    private void loadDashboardStats() {
        int userId = currentUser != null ? currentUser.getId() : 0;

        try {
            lblTotalNotes.setText(String.valueOf(noteService.getNotesByUserId(userId).size()));
        } catch (Exception e) {
            lblTotalNotes.setText("0");
            showAlert("Database Error", "Failed to load notes count: " + e.getMessage());
        }

        try {
            lblSharedResources.setText(String.valueOf(resourceService.countActiveResources()));
        } catch (Exception e) {
            lblSharedResources.setText("0");
            showAlert("Database Error", "Failed to load resources count: " + e.getMessage());
        }

        try {
            lblTotalQuestions.setText(String.valueOf(questionService.countQuestionsByUser(userId)));
        } catch (Exception e) {
            lblTotalQuestions.setText("0");
            showAlert("Database Error", "Failed to load questions count: " + e.getMessage());
        }

        try {
            int totalTasks = taskService.getTotalTaskCount(userId);
            int completedTasks = taskService.getCompletedTaskCount(userId);
            int progress = taskService.getCompletedPercentage(userId);

            lblTotalTasks.setText(String.valueOf(totalTasks - completedTasks));
            lblProgressPercentageStat.setText(progress + "%");
            lblProgressPercentage.setText(progress + "%");
            overallProgressBar.setProgress((double) progress / 100);
        } catch (Exception e) {
            lblTotalTasks.setText("0");
            lblProgressPercentageStat.setText("0%");
            lblProgressPercentage.setText("0%");
            overallProgressBar.setProgress(0);
            showAlert("Database Error", "Failed to load task statistics: " + e.getMessage());
        }
    }

    private void loadCharts() {
        studyBarChart.getData().clear();
    }

    public void loadApprovedNotes() {
        try {
            approvedNotes = dashboardService.getApprovedNotes();
            displayNotes(approvedNotes);
        } catch (Exception e) {
            approvedNotes = List.of();
            displayNotes(approvedNotes);
            showAlert("Database Error", "Failed to load approved notes: " + e.getMessage());
        }
    }

    private void loadRecentAndTrending() {
        recentNotesContainer.getChildren().clear();
        recentResourcesContainer.getChildren().clear();

        try {
            for (Note note : dashboardService.getRecentNotes().stream().limit(2).toList()) {
                VBox card = new VBox(5);
                card.setPadding(new Insets(10));
                card.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8;");
                Label label = new Label(note.getTitle());
                label.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                label.setWrapText(true);
                label.setMaxWidth(160);
                Label date = new Label(note.getUploadDate());
                date.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
                card.getChildren().addAll(label, date);
                recentNotesContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            showAlert("Database Error", "Failed to load recent notes: " + e.getMessage());
        }

        try {
            for (Resource resource : resourceService.getAllActiveResources().stream().limit(2).toList()) {
                VBox card = new VBox(5);
                card.setPadding(new Insets(10));
                card.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 8;");
                Label label = new Label(resource.getTitle());
                label.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #16a34a;");
                label.setWrapText(true);
                label.setMaxWidth(160);
                Label date = new Label(resource.getUploadDate());
                date.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
                card.getChildren().addAll(label, date);
                recentResourcesContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            showAlert("Database Error", "Failed to load recent resources: " + e.getMessage());
        }
    }

    public void searchNotes() {
        String query = noteSearchField.getText().trim();
        String subject = subjectComboBox.getValue();

        try {
            List<Note> filtered = dashboardService.searchNotes(query, subject);
            displayNotes(filtered);
        } catch (Exception e) {
            showAlert("Database Error", "Failed to search notes: " + e.getMessage());
        }
    }

    private void displayNotes(List<Note> notes) {
        notesFlowPane.getChildren().clear();
        for (Note note : notes) {
            notesFlowPane.getChildren().add(createNoteCard(note));
        }
    }

    private VBox createNoteCard(Note note) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setPrefWidth(200.0);
        card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 10;");

        Label title = new Label(note.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
        title.setWrapText(true);

        Label sub = new Label(note.getSubject());
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        Label author = new Label("By: User " + note.getUserId());
        author.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(title, sub, author);
        return card;
    }

    @FXML
    public void handleUploadNotes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/studybuddy/fxml/CreateNoteDialog.fxml"));
            VBox dialogContent = loader.load();

            Stage dialog = new Stage();
            dialog.setTitle("Create New Note");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(dialogContent));
            dialog.setResizable(false);
            dialog.showAndWait();

            refreshDashboard();
        } catch (IOException e) {
            showAlert("Error", "Failed to open create note dialog: " + e.getMessage());
        }
    }

    @FXML
    public void handleGoToQuestions() {
        loadCenterView("/com/studybuddy/fxml/QuestionsView.fxml");
    }

    @FXML
    public void handleUploadResourceAction() {
        loadCenterView("/com/studybuddy/fxml/ResourcesView.fxml");
    }

    @FXML
    public void handleGoToProfile() {
        loadCenterView("/com/studybuddy/fxml/ProfileView.fxml");
    }

    @FXML
    public void handleGoToResources() {
        loadCenterView("/com/studybuddy/fxml/ResourcesView.fxml");
    }

    private void loadCenterView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            } else {
                BorderPane mainBorderPane = (BorderPane) rootPane.getScene().getRoot();
                mainBorderPane.setCenter(view);
            }
        } catch (Exception e) {
            showAlert("Navigation Error", "Could not navigate to " + fxmlPath + ": " + e.getMessage());
        }
    }

    private void setupListeners() {
        sortComboBox.setOnAction(e -> displayNotes(dashboardService.sortNotes(approvedNotes, sortComboBox.getValue())));
        subjectComboBox.setOnAction(e -> searchNotes());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
