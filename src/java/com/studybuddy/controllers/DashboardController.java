package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Note;
import com.studybuddy.models.User;
import com.studybuddy.services.DashboardService;
import com.studybuddy.services.NoteService;
import com.studybuddy.services.ResourceService;
import com.studybuddy.services.TaskService;
import com.studybuddy.utils.ImageLoader;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
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
import java.util.stream.Collectors;

public class DashboardController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private Label welcomeLabel;

    // Stat Cards
    @FXML private Label lblTotalNotes;
    @FXML private Label lblSharedResources;
    @FXML private Label lblTotalQuestions;
    @FXML private Label lblTotalTasks;
    @FXML private Label lblProgressPercentageStat;

    // Chart & Progress
    @FXML private BarChart<String, Number> studyBarChart;
    @FXML private Label lblProgressPercentage;
    @FXML private ProgressBar overallProgressBar;

    // Library Filtering
    @FXML private ComboBox<String> subjectComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private TextField noteSearchField;
    @FXML private FlowPane notesFlowPane;

    // Recent Containers
    @FXML private HBox recentNotesContainer;
    @FXML private HBox recentResourcesContainer;

    private final DashboardService dashboardService = DashboardService.getInstance();
    private final NoteService noteService = new NoteService();
    private final ResourceService resourceService = new ResourceService();
    private final TaskService taskService = new TaskService();
    private final ImageLoader imageLoader = ImageLoader.getInstance();

    private List<Note> approvedNotes;
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = App.getCurrentUser();

        initializeComboBoxes();
        refreshDashboard();
        setupListeners();
    }

    private void initializeComboBoxes() {
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Most Popular", "Most Downloaded", "Highest Rated", "Newest", "Oldest"
        ));
        sortComboBox.setValue("Most Popular");

        subjectComboBox.setItems(FXCollections.observableArrayList(
                "Physics", "Computer Science", "Mathematics", "Chemistry",
                "Civil Engineering", "Electrical Engineering", "Mechanical",
                "Architecture", "Biology", "Economics", "Business", "Programming"
        ));
    }

    @FXML
    public void refreshDashboard() {
        currentUser = App.getCurrentUser();
        
        // Welcome greeting
        if (currentUser != null) {
            welcomeLabel.setText("Welcome Back, " + currentUser.getName() + "! 👋");
        } else {
            welcomeLabel.setText("Welcome Back, Guest! 👋");
        }

        loadDashboardStats();
        loadCharts();
        loadApprovedNotes();
        loadRecentAndTrending();
    }

    private void loadDashboardStats() {
        int userId = (currentUser != null) ? currentUser.getId() : 1;

        // Total Notes
        try {
            List<Note> notes = noteService.getNotesByUserId(userId);
            lblTotalNotes.setText(String.valueOf(notes != null ? notes.size() : 4));
        } catch (Exception e) {
            lblTotalNotes.setText("4");
        }

        // Shared Resources
        try {
            int shared = resourceService.countActiveResources();
            lblSharedResources.setText(String.valueOf(shared > 0 ? shared : 3));
        } catch (Exception e) {
            lblSharedResources.setText("3");
        }

        // Questions Count (Mocked)
        lblTotalQuestions.setText("5");

        // Tasks Statistics
        int totalTasks = taskService.getTotalTaskCount(userId);
        int completedTasks = taskService.getCompletedTaskCount(userId);
        int pendingTasks = totalTasks - completedTasks;
        int progress = taskService.getCompletedPercentage(userId);

        // Fallbacks if DB has no mock tasks loaded
        if (totalTasks == 0) {
            totalTasks = 8;
            completedTasks = 6;
            pendingTasks = 2;
            progress = 75;
        }

        lblTotalTasks.setText(String.valueOf(pendingTasks));
        lblProgressPercentageStat.setText(progress + "%");
        lblProgressPercentage.setText(progress + "%");
        overallProgressBar.setProgress((double) progress / 100);
    }

    private void loadCharts() {
        studyBarChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Hours Studied");
        series.getData().add(new XYChart.Data<>("Math", 12));
        series.getData().add(new XYChart.Data<>("Physics", 8));
        series.getData().add(new XYChart.Data<>("CompSci", 18));
        series.getData().add(new XYChart.Data<>("Chemistry", 5));
        series.getData().add(new XYChart.Data<>("Programming", 15));

        studyBarChart.getData().add(series);
    }

    public void loadApprovedNotes() {
        approvedNotes = dashboardService.getApprovedNotes();
        if (approvedNotes == null || approvedNotes.isEmpty()) {
            displayNotes(FXCollections.emptyObservableList());
        } else {
            displayNotes(approvedNotes);
        }
    }

    private void loadRecentAndTrending() {
        recentNotesContainer.getChildren().clear();
        recentResourcesContainer.getChildren().clear();

        // Recent shared notes
        List<Note> recent = dashboardService.getRecentNotes();
        if (recent != null) {
            for (Note note : recent.stream().limit(2).collect(Collectors.toList())) {
                VBox card = new VBox(5);
                card.setPadding(new Insets(10));
                card.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8;");
                Label label = new Label(note.getTitle());
                label.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                label.setWrapText(true);
                label.setMaxWidth(160);
                Label date = new Label("📅 " + note.getUploadDate());
                date.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
                card.getChildren().addAll(label, date);
                recentNotesContainer.getChildren().add(card);
            }
        }

        // Recent shared resources (Mocked)
        String[] resTitles = {"Calculus II Study Guide", "OS Scheduling CheatSheet"};
        String[] resDates = {"2026-06-18", "2026-06-19"};
        for (int i = 0; i < resTitles.length; i++) {
            VBox card = new VBox(5);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 8;");
            Label label = new Label(resTitles[i]);
            label.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #16a34a;");
            label.setWrapText(true);
            label.setMaxWidth(160);
            Label date = new Label("📅 " + resDates[i]);
            date.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
            card.getChildren().addAll(label, date);
            recentResourcesContainer.getChildren().add(card);
        }
    }

    public void searchNotes() {
        String query = noteSearchField.getText().trim();
        String subject = subjectComboBox.getValue();

        List<Note> filtered = dashboardService.searchNotes(query);
        if (subject != null) {
            filtered = filtered.stream()
                    .filter(n -> n.getSubject().equalsIgnoreCase(subject))
                    .collect(Collectors.toList());
        }

        displayNotes(filtered);
    }

    private void displayNotes(List<Note> notes) {
        notesFlowPane.getChildren().clear();
        for (Note note : notes) {
            VBox card = createNoteCard(note);
            notesFlowPane.getChildren().add(card);
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

        Label sub = new Label("📚 " + note.getSubject());
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        Label author = new Label("👤 By: User " + note.getUserId());
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
            e.printStackTrace();
        }
    }

    private void setupListeners() {
        sortComboBox.setOnAction(e -> {
            String sortBy = sortComboBox.getValue();
            List<Note> sorted = dashboardService.sortNotes(approvedNotes, sortBy);
            displayNotes(sorted);
        });

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