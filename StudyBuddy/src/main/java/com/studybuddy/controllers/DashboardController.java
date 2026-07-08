package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.dao.QuestionDAO;
import com.studybuddy.models.Department;
import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.models.User;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.DashboardService;
import com.studybuddy.services.NoteService;
import com.studybuddy.services.ResourceService;
import com.studybuddy.services.TaskService;
import com.studybuddy.utils.EventBus;
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
import java.sql.SQLException;
import java.util.ArrayList;
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
    @FXML private ComboBox<Department> departmentComboBox;
    @FXML private ComboBox<Semester> semesterComboBox;
    @FXML private ComboBox<String> subjectComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private TextField noteSearchField;
    @FXML private FlowPane notesFlowPane;

    private final AcademicService academicService = AcademicService.getInstance();

    // Recent Containers
    @FXML private HBox recentNotesContainer;
    @FXML private HBox recentResourcesContainer;

    private final DashboardService dashboardService = DashboardService.getInstance();
    private final NoteService noteService = new NoteService();
    private final ResourceService resourceService = new ResourceService();
    private final TaskService taskService = new TaskService();
    private final QuestionDAO questionDAO = new QuestionDAO(); // FIXED: for real question count
    private final com.studybuddy.dao.NoteDAO noteDAO = new com.studybuddy.dao.NoteDAO();
    private final ImageLoader imageLoader = ImageLoader.getInstance();

    private List<Note> approvedNotes;
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = App.getCurrentUser();

        initializeComboBoxes();
        refreshDashboard();
        setupListeners();

        // Subscribe to EventBus events
        EventBus.getInstance().subscribe(EventBus.NotesChangedEvent.class, (_event) -> refreshDashboard());
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, (_event) -> refreshDashboard());
    }

    private void initializeComboBoxes() {
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Most Popular", "Most Downloaded", "Highest Rated", "Newest", "Oldest"
        ));
        sortComboBox.setValue("Most Popular");

        // Load departments
        try {
            departmentComboBox.setItems(FXCollections.observableArrayList(academicService.getAllActiveDepartments()));
        } catch (Exception e) {
            System.err.println("Error loading departments: " + e.getMessage());
        }

        // Disable semester/subject initially
        semesterComboBox.setDisable(true);
        subjectComboBox.setDisable(true);

        // Department change listener
        departmentComboBox.setOnAction(e -> {
            Department selectedDept = departmentComboBox.getValue();
            semesterComboBox.getItems().clear();
            subjectComboBox.getItems().clear();
            semesterComboBox.setValue(null);
            subjectComboBox.setValue(null);
            semesterComboBox.setDisable(selectedDept == null);
            subjectComboBox.setDisable(true);

            if (selectedDept != null) {
                try {
                    semesterComboBox.setItems(FXCollections.observableArrayList(academicService.getSemestersByDepartment(selectedDept.getId())));
                } catch (Exception ex) {
                    System.err.println("Error loading semesters: " + ex.getMessage());
                }
            }
        });

        // Semester change listener
        semesterComboBox.setOnAction(e -> {
            Semester selectedSem = semesterComboBox.getValue();
            subjectComboBox.getItems().clear();
            subjectComboBox.setValue(null);
            subjectComboBox.setDisable(selectedSem == null);

            if (selectedSem != null) {
                try {
                    List<Subject> subjects = academicService.getSubjectsBySemester(selectedSem.getId());
                    subjectComboBox.setItems(FXCollections.observableArrayList(subjects.stream().map(Subject::getName).collect(Collectors.toList())));
                } catch (Exception ex) {
                    System.err.println("Error loading subjects: " + ex.getMessage());
                }
            }
        });
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

        // Total Notes — from SQL Server via NoteService → NoteDAO
        // SQL: SELECT COUNT(*) FROM Notes WHERE userId = ?
        try {
            int noteCount = noteService.countNotesByUser(userId);
            lblTotalNotes.setText(String.valueOf(noteCount));
        } catch (Exception e) {
            lblTotalNotes.setText("0");
        }

        // Shared Resources — from SQL Server via ResourceService → ResourceDAO
        // SQL: SELECT COUNT(*) FROM Resources WHERE isActive = 1
        try {
            int shared = resourceService.countActiveResources();
            lblSharedResources.setText(String.valueOf(shared));
        } catch (Exception e) {
            lblSharedResources.setText("0");
        }

        // Questions Count — FIXED: from SQL Server via QuestionDAO
        // SQL: SELECT COUNT(*) FROM Questions
        try {
            int questionCount = questionDAO.countAllQuestions();
            lblTotalQuestions.setText(String.valueOf(questionCount));
        } catch (Exception e) {
            lblTotalQuestions.setText("0");
        }

        // Tasks Statistics — from SQL Server via TaskService → TaskDAO
        // SQL: SELECT COUNT(*) FROM Tasks WHERE userId = ?
        int totalTasks = taskService.getTotalTaskCount(userId);
        int completedTasks = taskService.getCompletedTaskCount(userId);
        int pendingTasks = totalTasks - completedTasks;
        int progress = taskService.getCompletedPercentage(userId);

        lblTotalTasks.setText(String.valueOf(pendingTasks));
        lblProgressPercentageStat.setText(progress + "%");
        lblProgressPercentage.setText(progress + "%");
        overallProgressBar.setProgress((double) progress / 100);
    }

    private void loadCharts() {
        studyBarChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Notes by Subject");
        
        try {
            java.util.Map<String, Integer> subjectCount = noteDAO.getNotesCountBySubjectForUser(currentUser.getId());
            if (subjectCount.isEmpty()) {
                series.getData().add(new XYChart.Data<>("No Notes", 0));
            } else {
                for (var entry : subjectCount.entrySet()) {
                    series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            series.getData().add(new XYChart.Data<>("Error", 0));
        }
        
        studyBarChart.getData().add(series);
    }

    public void loadApprovedNotes() {
        // FIXED: DashboardService.getApprovedNotes() now fetches from SQL Server
        // SQL: SELECT * FROM Notes WHERE isPrivate = 0 ORDER BY uploadDate DESC
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

        // FIXED: Recent shared notes now come from SQL Server via DashboardService → NoteDAO
        // SQL: SELECT TOP 5 * FROM Notes WHERE isPrivate = 0 ORDER BY uploadDate DESC
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

        // FIXED: Recent resources now come from SQL Server via ResourceService → ResourceDAO
        // SQL: SELECT * FROM Resources WHERE isActive = 1 ORDER BY uploadDate DESC
        try {
            List<Resource> recentResources = resourceService.getAllActiveResources();
            if (recentResources != null) {
                for (Resource res : recentResources.stream().limit(2).collect(Collectors.toList())) {
                    VBox card = new VBox(5);
                    card.setPadding(new Insets(10));
                    card.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 8;");
                    Label label = new Label(res.getTitle());
                    label.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #16a34a;");
                    label.setWrapText(true);
                    label.setMaxWidth(160);
                    Label date = new Label("📅 " + res.getUploadDate());
                    date.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
                    card.getChildren().addAll(label, date);
                    recentResourcesContainer.getChildren().add(card);
                }
            }
        } catch (Exception e) {
            // Resources section fails gracefully
            System.err.println("Could not load recent resources: " + e.getMessage());
        }
    }

    public void searchNotes() {
        String query = noteSearchField.getText().trim();
        Department selectedDept = departmentComboBox.getValue();
        Semester selectedSem = semesterComboBox.getValue();
        String selectedSubject = subjectComboBox.getValue();

        Integer deptId = selectedDept != null ? selectedDept.getId() : null;
        Integer semId = selectedSem != null ? selectedSem.getId() : null;

        // Find subjectId if subject selected
        Integer subjectId = null;
        if (selectedSubject != null && selectedSem != null) {
            try {
                List<Subject> subjects = academicService.getSubjectsBySemester(selectedSem.getId());
                for (Subject subj : subjects) {
                    if (subj.getName().equalsIgnoreCase(selectedSubject)) {
                        subjectId = subj.getId();
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error finding subject ID: " + e.getMessage());
            }
        }

        // Use NoteDAO.searchNotesWithHierarchy which is already implemented!
        List<Note> filtered = new ArrayList<>();
        try {
            filtered = noteDAO.searchNotesWithHierarchy(query, deptId, semId, subjectId);
        } catch (SQLException e) {
            System.err.println("Error searching notes with hierarchy: " + e.getMessage());
            filtered = dashboardService.searchNotes(query);
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

        // Build author label with full name, dept, sem
        StringBuilder authorText = new StringBuilder("👤 By: ");
        String fullName = note.getUserFullName();
        if (fullName != null && !fullName.isEmpty()) {
            authorText.append(fullName);
        } else {
            authorText.append("User ").append(note.getUserId());
        }
        String dept = note.getUserDepartment();
        String sem = note.getUserSemester();
        if (dept != null || sem != null) {
            authorText.append(" (");
            if (dept != null) authorText.append(dept);
            if (dept != null && sem != null) authorText.append(" • ");
            if (sem != null) authorText.append(sem);
            authorText.append(")");
        }
        Label author = new Label(authorText.toString());
        author.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        author.setWrapText(true);

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