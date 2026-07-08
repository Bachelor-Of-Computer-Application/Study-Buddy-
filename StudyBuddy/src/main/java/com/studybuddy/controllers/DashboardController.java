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
import com.studybuddy.utils.AcademicFilterHelper;
import com.studybuddy.utils.EventBus;
import com.studybuddy.utils.ImageLoader;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.studybuddy.models.Question;
import com.studybuddy.models.UserActivity;
import com.studybuddy.services.QuestionService;
import com.studybuddy.services.AuthorizationService;
import javafx.scene.layout.BorderPane;
import java.awt.Desktop;
import java.io.File;
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
    @FXML private ImageView heroAvatarView;

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

    // My Uploads
    @FXML private TabPane myUploadsTabPane;
    @FXML private TableView<Note> myNotesTable;
    @FXML private TableColumn<Note, String> myNoteTitleCol;
    @FXML private TableColumn<Note, String> myNoteSubjectCol;
    @FXML private TableColumn<Note, String> myNoteStatusCol;
    @FXML private TableColumn<Note, String> myNoteDateCol;
    @FXML private TableView<Resource> myResourcesTable;
    @FXML private TableColumn<Resource, String> myResTitleCol;
    @FXML private TableColumn<Resource, String> myResSubjectCol;
    @FXML private TableColumn<Resource, String> myResStatusCol;
    @FXML private TableColumn<Resource, String> myResDateCol;
    @FXML private TableView<Question> myQuestionsTable;
    @FXML private TableColumn<Question, String> myQTitleCol;
    @FXML private TableColumn<Question, String> myQSubjectCol;
    @FXML private TableColumn<Question, String> myQDateCol;

    private final AcademicService academicService = AcademicService.getInstance();

    // Recent Containers
    @FXML private HBox recentNotesContainer;
    @FXML private HBox recentResourcesContainer;
    @FXML private HBox recentQuestionsContainer;
    @FXML private HBox recentActivityContainer;

    private final com.studybuddy.dao.UserActivityDAO userActivityDAO = new com.studybuddy.dao.UserActivityDAO();

    private final DashboardService dashboardService = DashboardService.getInstance();
    private final NoteService noteService = new NoteService();
    private final ResourceService resourceService = new ResourceService();
    private final TaskService taskService = new TaskService();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final QuestionService questionService = new QuestionService();
    private final AuthorizationService authService = AuthorizationService.getInstance();
    private final com.studybuddy.dao.NoteDAO noteDAO = new com.studybuddy.dao.NoteDAO();

    private List<Note> approvedNotes;
    private User currentUser;
    private final ImageLoader imageLoader = ImageLoader.getInstance();
    private static final double HERO_AVATAR_SIZE = 88;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = App.getCurrentUser();

        initializeComboBoxes();
        setupMyUploadsTables();
        refreshDashboard();
        setupListeners();

        // Subscribe to EventBus events
        EventBus.getInstance().subscribe(EventBus.NotesChangedEvent.class, (_event) -> refreshDashboard());
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, (_event) -> refreshDashboard());
        EventBus.getInstance().subscribe(EventBus.ProfileChangedEvent.class, (_event) -> refreshHeroAvatar());
        refreshHeroAvatar();
    }

    private void refreshHeroAvatar() {
        if (heroAvatarView == null) return;
        currentUser = App.getCurrentUser();
        String path = currentUser != null ? currentUser.getProfileImagePath() : null;
        imageLoader.applyAvatarToView(heroAvatarView, path, HERO_AVATAR_SIZE);
    }

    private void initializeComboBoxes() {
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Most Popular", "Most Downloaded", "Highest Rated", "Newest", "Oldest"
        ));
        sortComboBox.setValue("Most Popular");

        AcademicFilterHelper.setupFilterBar(academicService, departmentComboBox, semesterComboBox, subjectComboBox);
    }

    @FXML
    public void refreshDashboard() {
        refreshStats();
        refreshMyUploads();
    }

    private void setupMyUploadsTables() {
        if (myNoteTitleCol != null) myNoteTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (myNoteSubjectCol != null) myNoteSubjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (myNoteStatusCol != null) myNoteStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (myNoteDateCol != null) myNoteDateCol.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
        if (myResTitleCol != null) myResTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (myResSubjectCol != null) myResSubjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (myResStatusCol != null) myResStatusCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getStatus() != null ? cell.getValue().getStatus()
                                : (cell.getValue().isActive() ? "Approved" : "Pending")));
        if (myResDateCol != null) myResDateCol.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
        if (myQTitleCol != null) myQTitleCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getTitle() != null ? cell.getValue().getTitle()
                                : cell.getValue().getQuestionText()));
        if (myQSubjectCol != null) myQSubjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (myQDateCol != null) myQDateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    @FXML public void refreshMyUploads() {
        if (currentUser == null) return;
        try {
            if (myNotesTable != null) {
                myNotesTable.setItems(FXCollections.observableArrayList(
                        noteService.getNotesByUserId(currentUser.getId())));
            }
            if (myResourcesTable != null) {
                myResourcesTable.setItems(FXCollections.observableArrayList(
                        resourceService.getResourcesByUser(currentUser.getId())));
            }
            if (myQuestionsTable != null) {
                myQuestionsTable.setItems(FXCollections.observableArrayList(
                        questionService.getQuestionsByUserId(currentUser.getId())));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load my uploads: " + e.getMessage());
        }
    }

    @FXML public void handleDeleteMyNote() {
        Note n = myNotesTable.getSelectionModel().getSelectedItem();
        if (n == null) { showAlert("Select", "Select a note to delete."); return; }
        if (!authService.canDeleteNote(currentUser, n)) { showAlert("Denied", "You can only delete your own notes."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete note '" + n.getTitle() + "'?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            noteService.deleteNoteWithFile(n.getId(), currentUser);
            refreshMyUploads();
            refreshDashboard();
            EventBus.getInstance().publish(new EventBus.NotesChangedEvent());
        } catch (Exception e) {
            showAlert("Error", "Delete failed: " + e.getMessage());
        }
    }

    @FXML public void handleEditMyNote() { handleUploadNotes(); }

    @FXML public void handleViewMyNote() { openFile(myNotesTable.getSelectionModel().getSelectedItem() != null
            ? myNotesTable.getSelectionModel().getSelectedItem().getFilePath() : null); }

    @FXML public void handleDownloadMyNote() { handleViewMyNote(); }

    @FXML public void handleDeleteMyResource() {
        Resource r = myResourcesTable.getSelectionModel().getSelectedItem();
        if (r == null) { showAlert("Select", "Select a resource."); return; }
        if (!authService.canDeleteResource(currentUser, r)) { showAlert("Denied", "You can only delete your own resources."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete resource?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            resourceService.deleteResourceWithFile(r.getId(), currentUser);
            EventBus.getInstance().publish(new EventBus.ResourcesChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
            refreshMyUploads();
            refreshDashboard();
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML public void handleViewMyResource() {
        Resource r = myResourcesTable.getSelectionModel().getSelectedItem();
        if (r != null) openFile(r.getFilePath());
    }

    @FXML public void handleDownloadMyResource() { handleViewMyResource(); }

    @FXML public void handleDeleteMyQuestion() {
        Question q = myQuestionsTable.getSelectionModel().getSelectedItem();
        if (q == null) return;
        if (!authService.canDeleteQuestion(currentUser, q)) { showAlert("Denied", "You can only delete your own questions."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this question?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            questionService.deleteQuestion(q.getId(), currentUser);
            refreshMyUploads();
            EventBus.getInstance().publish(new EventBus.QuestionsChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML public void handleViewMyQuestion() {
        Question q = myQuestionsTable.getSelectionModel().getSelectedItem();
        if (q != null) showAlert("Question", q.getQuestionText());
    }

    private void openFile(String path) {
        if (path == null || path.isBlank()) { showAlert("No file", "No file attached."); return; }
        File f = new File(path);
        if (!f.exists()) { showAlert("Missing", "File not found."); return; }
        try { Desktop.getDesktop().open(f); } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    private void refreshStats() {
        currentUser = App.getCurrentUser();
        
        // Welcome greeting
        if (currentUser != null) {
            welcomeLabel.setText("Welcome Back, " + currentUser.getDisplayFullName() + "! 👋");
        } else {
            welcomeLabel.setText("Welcome Back, Guest! 👋");
        }

        loadDashboardStats();
        loadCharts();
        loadApprovedNotes();
        loadRecentAndTrending();
    }

    private void loadDashboardStats() {
        if (currentUser == null) {
            lblTotalNotes.setText("0");
            lblTotalTasks.setText("0");
            lblProgressPercentageStat.setText("0%");
            lblProgressPercentage.setText("0%");
            overallProgressBar.setProgress(0);
            try {
                lblSharedResources.setText(String.valueOf(resourceService.countActiveResources()));
            } catch (Exception e) {
                lblSharedResources.setText("0");
            }
            try {
                lblTotalQuestions.setText(String.valueOf(questionDAO.countAllQuestions()));
            } catch (Exception e) {
                lblTotalQuestions.setText("0");
            }
            return;
        }

        int userId = currentUser.getId();

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

        if (currentUser == null) {
            series.getData().add(new XYChart.Data<>("Log in to view", 0));
            studyBarChart.getData().add(series);
            return;
        }

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
        if (recentQuestionsContainer != null) recentQuestionsContainer.getChildren().clear();
        if (recentActivityContainer != null) recentActivityContainer.getChildren().clear();

        List<Note> recent = dashboardService.getRecentNotes();
        if (recent != null && !recent.isEmpty()) {
            for (Note note : recent.stream().limit(2).collect(Collectors.toList())) {
                recentNotesContainer.getChildren().add(buildMiniFeedCard(
                        note.getTitle(), note.getUploadDate(), false));
            }
        } else {
            recentNotesContainer.getChildren().add(buildWidgetEmptyLabel("No recent notes yet."));
        }

        try {
            List<Resource> recentResources = resourceService.getAllActiveResources();
            if (recentResources != null && !recentResources.isEmpty()) {
                for (Resource res : recentResources.stream().limit(2).collect(Collectors.toList())) {
                    recentResourcesContainer.getChildren().add(buildMiniFeedCard(
                            res.getTitle(), res.getUploadDate(), true));
                }
            } else {
                recentResourcesContainer.getChildren().add(buildWidgetEmptyLabel("No recent resources yet."));
            }
        } catch (Exception e) {
            recentResourcesContainer.getChildren().add(buildWidgetEmptyLabel("Could not load resources."));
            System.err.println("Could not load recent resources: " + e.getMessage());
        }

        if (recentQuestionsContainer != null) {
            try {
                List<Question> recentQuestions = questionDAO.getAllQuestions();
                if (recentQuestions != null && !recentQuestions.isEmpty()) {
                    for (Question q : recentQuestions.stream().limit(2).collect(Collectors.toList())) {
                        String title = q.getTitle() != null ? q.getTitle() : q.getQuestionText();
                        recentQuestionsContainer.getChildren().add(buildMiniFeedCard(
                                title, q.getCreatedAt(), false));
                    }
                } else {
                    recentQuestionsContainer.getChildren().add(buildWidgetEmptyLabel("No questions posted yet."));
                }
            } catch (SQLException e) {
                recentQuestionsContainer.getChildren().add(buildWidgetEmptyLabel("Could not load questions."));
            }
        }

        if (recentActivityContainer != null) {
            try {
                List<UserActivity> activities = currentUser != null
                        ? userActivityDAO.getUserActivities(currentUser.getId(), 4)
                        : userActivityDAO.getRecentActivities(4);
                if (activities != null && !activities.isEmpty()) {
                    for (UserActivity activity : activities) {
                        recentActivityContainer.getChildren().add(buildMiniFeedCard(
                                activity.getAction() + " · " + activity.getTargetName(),
                                activity.getCreatedAt() != null ? activity.getCreatedAt().toString() : "",
                                false));
                    }
                } else {
                    recentActivityContainer.getChildren().add(buildWidgetEmptyLabel("No recent activity yet."));
                }
            } catch (SQLException e) {
                recentActivityContainer.getChildren().add(buildWidgetEmptyLabel("Activity feed unavailable."));
            }
        }
    }

    private VBox buildMiniFeedCard(String title, String dateText, boolean green) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("mini-feed-card", green ? "mini-feed-card-green" : "");
        Label label = new Label(title);
        label.getStyleClass().addAll("mini-feed-title", green ? "mini-feed-title-green" : "");
        label.setWrapText(true);
        label.setMaxWidth(160);
        Label date = new Label("📅 " + (dateText != null ? dateText : "—"));
        date.getStyleClass().add("mini-feed-date");
        card.getChildren().addAll(label, date);
        return card;
    }

    private Label buildWidgetEmptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("widget-empty");
        return label;
    }

    @FXML
    public void searchNotes() {
        String query = noteSearchField.getText().trim();
        Department selectedDept = departmentComboBox.getValue();
        Semester selectedSem = semesterComboBox.getValue();
        String selectedSubject = subjectComboBox.getValue();

        Integer deptId = AcademicFilterHelper.resolveDepartmentId(selectedDept);
        Integer semId = AcademicFilterHelper.resolveSemesterId(selectedSem);

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
        card.setPrefWidth(200.0);
        card.getStyleClass().add("library-note-card");

        Label title = new Label(note.getTitle());
        title.getStyleClass().add("library-note-title");
        title.setWrapText(true);

        Label sub = new Label("📚 " + note.getSubject());
        sub.getStyleClass().add("library-note-subject");

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
        author.getStyleClass().add("library-note-author");
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