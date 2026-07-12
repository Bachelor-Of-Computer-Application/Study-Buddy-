
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.studybuddy.models.Question;
import com.studybuddy.models.UserActivity;
import com.studybuddy.services.QuestionService;
import com.studybuddy.services.AuthorizationService;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    @FXML private Label lblCompletedTasks;
    @FXML private Label lblPendingTasks;
    @FXML private Label lblInProgressTasks;
    @FXML private Label lblTaskCompletionPercentage;

    // Chart & Progress
    @FXML private BarChart<String, Number> studyBarChart;
    @FXML private Label lblProgressPercentage;
    @FXML private ProgressBar overallProgressBar;

    // Library Filtering
    @FXML private ComboBox<Department> departmentComboBox;
    @FXML private ComboBox<Semester> semesterComboBox;
    @FXML private ComboBox<Subject> subjectComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private TextField noteSearchField;
    @FXML private FlowPane notesFlowPane;

    // My Uploads
    @FXML private Button tabMyNotes;
    @FXML private Button tabMyResources;
    @FXML private Button tabMyQuestions;
    @FXML private FlowPane myUploadsFlowPane;
    @FXML private TextField myUploadsSearchField;
    @FXML private Label summaryTotal;
    @FXML private Label summaryApproved;
    @FXML private Label summaryPending;
    @FXML private Label summaryRejected;
    
    private enum UploadTab { NOTES, RESOURCES, QUESTIONS }
    private UploadTab activeTab = UploadTab.NOTES;
    
    private Note selectedNote;
    private Resource selectedResource;
    private Question selectedQuestion;

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
        refreshDashboard();
        setupListeners();

        // Subscribe to EventBus events - use Platform.runLater() to ensure DB calls
        // don't block the UI when events are published from background threads.
        EventBus.getInstance().subscribe(EventBus.NotesChangedEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshDashboard));
        EventBus.getInstance().subscribe(EventBus.ResourcesChangedEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshDashboard));
        EventBus.getInstance().subscribe(EventBus.QuestionsChangedEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshDashboard));
        EventBus.getInstance().subscribe(EventBus.TasksChangedEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshDashboard));
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshDashboard));
        EventBus.getInstance().subscribe(EventBus.ProfileChangedEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshHeroAvatar));
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

    @FXML public void handleMyUploadsTabChange(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        if (source == tabMyNotes) {
            activeTab = UploadTab.NOTES;
            tabMyNotes.getStyleClass().remove("my-uploads-tab");
            tabMyNotes.getStyleClass().add("my-uploads-tab-active");
            tabMyResources.getStyleClass().remove("my-uploads-tab-active");
            tabMyResources.getStyleClass().add("my-uploads-tab");
            tabMyQuestions.getStyleClass().remove("my-uploads-tab-active");
            tabMyQuestions.getStyleClass().add("my-uploads-tab");
        } else if (source == tabMyResources) {
            activeTab = UploadTab.RESOURCES;
            tabMyResources.getStyleClass().remove("my-uploads-tab");
            tabMyResources.getStyleClass().add("my-uploads-tab-active");
            tabMyNotes.getStyleClass().remove("my-uploads-tab-active");
            tabMyNotes.getStyleClass().add("my-uploads-tab");
            tabMyQuestions.getStyleClass().remove("my-uploads-tab-active");
            tabMyQuestions.getStyleClass().add("my-uploads-tab");
        } else if (source == tabMyQuestions) {
            activeTab = UploadTab.QUESTIONS;
            tabMyQuestions.getStyleClass().remove("my-uploads-tab");
            tabMyQuestions.getStyleClass().add("my-uploads-tab-active");
            tabMyNotes.getStyleClass().remove("my-uploads-tab-active");
            tabMyNotes.getStyleClass().add("my-uploads-tab");
            tabMyResources.getStyleClass().remove("my-uploads-tab-active");
            tabMyResources.getStyleClass().add("my-uploads-tab");
        }
        
        selectedNote = null;
        selectedResource = null;
        selectedQuestion = null;
        
        refreshMyUploads();
    }

    @FXML public void refreshMyUploads() {
        if (currentUser == null) return;
        myUploadsFlowPane.getChildren().clear();
        String searchText = myUploadsSearchField.getText().trim().toLowerCase();
        
        int total = 0, approved = 0, pending = 0, rejected = 0;
        
        try {
            if (activeTab == UploadTab.NOTES) {
                List<Note> notes = noteService.getNotesByUserId(currentUser.getId());
                for (Note note : notes) {
                    if (searchText.isEmpty() || 
                        note.getTitle().toLowerCase().contains(searchText) ||
                        note.getSubject().toLowerCase().contains(searchText)) {
                        myUploadsFlowPane.getChildren().add(createMyUploadNoteCard(note));
                    }
                    total++;
                    if ("Approved".equalsIgnoreCase(note.getStatus())) approved++;
                    else if ("Pending".equalsIgnoreCase(note.getStatus())) pending++;
                    else if ("Rejected".equalsIgnoreCase(note.getStatus())) rejected++;
                }
            } else if (activeTab == UploadTab.RESOURCES) {
                List<Resource> resources = resourceService.getResourcesByUser(currentUser.getId());
                for (Resource res : resources) {
                    if (searchText.isEmpty() || 
                        res.getTitle().toLowerCase().contains(searchText) ||
                        res.getSubject().toLowerCase().contains(searchText)) {
                        myUploadsFlowPane.getChildren().add(createMyUploadResourceCard(res));
                    }
                    total++;
                    String status = res.getStatus() != null ? res.getStatus() : (res.isActive() ? "Approved" : "Pending");
                    if ("Approved".equalsIgnoreCase(status)) approved++;
                    else if ("Pending".equalsIgnoreCase(status)) pending++;
                    else if ("Rejected".equalsIgnoreCase(status)) rejected++;
                }
            } else if (activeTab == UploadTab.QUESTIONS) {
                List<Question> questions = questionService.getQuestionsByUserId(currentUser.getId());
                for (Question q : questions) {
                    String title = q.getTitle() != null ? q.getTitle() : q.getQuestionText();
                    if (searchText.isEmpty() || 
                        title.toLowerCase().contains(searchText) ||
                        q.getSubject().toLowerCase().contains(searchText)) {
                        myUploadsFlowPane.getChildren().add(createMyUploadQuestionCard(q));
                    }
                    total++;
                    // Questions don't have status, count all as "Approved" for summary
                    approved++;
                }
            }
            
            // Update summary
            summaryTotal.setText(String.valueOf(total));
            summaryApproved.setText(String.valueOf(approved));
            summaryPending.setText(String.valueOf(pending));
            summaryRejected.setText(String.valueOf(rejected));
            
        } catch (SQLException e) {
            System.err.println("Failed to load my uploads: " + e.getMessage());
        }
    }
    
    private VBox createMyUploadNoteCard(Note note) {
        VBox card = new VBox(10);
        card.getStyleClass().add("upload-card");
        if (selectedNote != null && selectedNote.getId() == note.getId()) {
            card.getStyleClass().add("upload-card-selected");
        }
        
        // Header with icon and title
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label icon = new Label("📝");
        icon.getStyleClass().add("upload-card-icon");
        Label title = new Label(note.getTitle());
        title.getStyleClass().add("upload-card-title");
        title.setWrapText(true);
        title.setMaxWidth(300);
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(icon, title, spacer);
        
        // Badges
        HBox badges = new HBox(8);
        badges.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label deptBadge = new Label(note.getUserDepartment() != null ? note.getUserDepartment() : "N/A");
        deptBadge.getStyleClass().addAll("badge", "badge-info");
        
        Label semBadge = new Label(note.getUserSemester() != null ? note.getUserSemester() : "N/A");
        semBadge.getStyleClass().addAll("badge", "badge-info");
        
        Label subjBadge = new Label(note.getSubject());
        subjBadge.getStyleClass().addAll("badge", "badge-purple");
        
        badges.getChildren().addAll(deptBadge, semBadge, subjBadge);
        
        // Metadata
        HBox metadata = new HBox(15);
        metadata.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label dateLabel = new Label("📅 " + (note.getUploadDate() != null ? note.getUploadDate() : "N/A"));
        dateLabel.getStyleClass().add("upload-card-meta");
        metadata.getChildren().addAll(dateLabel);
        
        // Status
        String status = note.getStatus() != null ? note.getStatus() : "Pending";
        Label statusLabel = new Label("⚡ " + status);
        if ("Approved".equalsIgnoreCase(status)) {
            statusLabel.getStyleClass().addAll("badge", "badge-success");
        } else if ("Pending".equalsIgnoreCase(status)) {
            statusLabel.getStyleClass().addAll("badge", "badge-warning");
        } else {
            statusLabel.getStyleClass().addAll("badge", "badge-danger");
        }
        
        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button viewBtn = new Button("👁");
        viewBtn.getStyleClass().add("upload-action-btn");
        viewBtn.setOnAction(e -> {
            selectedNote = note;
            handleViewMyNote();
        });
        
        Button downloadBtn = new Button("⬇");
        downloadBtn.getStyleClass().add("upload-action-btn");
        downloadBtn.setOnAction(e -> {
            selectedNote = note;
            handleDownloadMyNote();
        });
        
        Button editBtn = new Button("✏");
        editBtn.getStyleClass().add("upload-action-btn");
        editBtn.setOnAction(e -> {
            selectedNote = note;
            handleEditMyNote();
        });
        
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().addAll("upload-action-btn", "upload-action-btn-danger");
        deleteBtn.setOnAction(e -> {
            selectedNote = note;
            handleDeleteMyNote();
        });
        
        actions.getChildren().addAll(viewBtn, downloadBtn, editBtn, deleteBtn);
        
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, javafx.scene.layout.Priority.ALWAYS);
        bottomRow.getChildren().addAll(statusLabel, bottomSpacer, actions);
        
        card.getChildren().addAll(header, badges, metadata, bottomRow);
        
        // Make card selectable
        card.setOnMouseClicked(e -> {
            selectedNote = note;
            selectedResource = null;
            selectedQuestion = null;
            refreshMyUploads();
        });
        
        return card;
    }
    
    private VBox createMyUploadResourceCard(Resource res) {
        VBox card = new VBox(10);
        card.getStyleClass().add("upload-card");
        if (selectedResource != null && selectedResource.getId() == res.getId()) {
            card.getStyleClass().add("upload-card-selected");
        }
        
        // Header with icon and title
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label icon = new Label("📂");
        icon.getStyleClass().add("upload-card-icon");
        Label title = new Label(res.getTitle());
        title.getStyleClass().add("upload-card-title");
        title.setWrapText(true);
        title.setMaxWidth(300);
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(icon, title, spacer);
        
        // Badges
        HBox badges = new HBox(8);
        badges.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label deptBadge = new Label(res.getUserDepartment() != null ? res.getUserDepartment() : "N/A");
        deptBadge.getStyleClass().addAll("badge", "badge-info");
        
        Label semBadge = new Label(res.getUserSemester() != null ? res.getUserSemester() : "N/A");
        semBadge.getStyleClass().addAll("badge", "badge-info");
        
        Label subjBadge = new Label(res.getSubject());
        subjBadge.getStyleClass().addAll("badge", "badge-purple");
        
        badges.getChildren().addAll(deptBadge, semBadge, subjBadge);
        
        // Metadata
        HBox metadata = new HBox(15);
        metadata.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label dateLabel = new Label("📅 " + (res.getUploadDate() != null ? res.getUploadDate() : "N/A"));
        dateLabel.getStyleClass().add("upload-card-meta");
        Label sizeLabel = new Label("� " + res.getDownloads() + " downloads");
        sizeLabel.getStyleClass().add("upload-card-meta");
        metadata.getChildren().addAll(dateLabel, sizeLabel);
        
        // Status
        String status = res.getStatus() != null ? res.getStatus() : (res.isActive() ? "Approved" : "Pending");
        Label statusLabel = new Label("⚡ " + status);
        if ("Approved".equalsIgnoreCase(status)) {
            statusLabel.getStyleClass().addAll("badge", "badge-success");
        } else if ("Pending".equalsIgnoreCase(status)) {
            statusLabel.getStyleClass().addAll("badge", "badge-warning");
        } else {
            statusLabel.getStyleClass().addAll("badge", "badge-danger");
        }
        
        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button viewBtn = new Button("👁");
        viewBtn.getStyleClass().add("upload-action-btn");
        viewBtn.setOnAction(e -> {
            selectedResource = res;
            handleViewMyResource();
        });
        
        Button downloadBtn = new Button("⬇");
        downloadBtn.getStyleClass().add("upload-action-btn");
        downloadBtn.setOnAction(e -> {
            selectedResource = res;
            handleDownloadMyResource();
        });
        
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().addAll("upload-action-btn", "upload-action-btn-danger");
        deleteBtn.setOnAction(e -> {
            selectedResource = res;
            handleDeleteMyResource();
        });
        
        actions.getChildren().addAll(viewBtn, downloadBtn, deleteBtn);
        
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, javafx.scene.layout.Priority.ALWAYS);
        bottomRow.getChildren().addAll(statusLabel, bottomSpacer, actions);
        
        card.getChildren().addAll(header, badges, metadata, bottomRow);
        
        // Make card selectable
        card.setOnMouseClicked(e -> {
            selectedResource = res;
            selectedNote = null;
            selectedQuestion = null;
            refreshMyUploads();
        });
        
        return card;
    }
    
    private VBox createMyUploadQuestionCard(Question q) {
        VBox card = new VBox(10);
        card.getStyleClass().add("upload-card");
        if (selectedQuestion != null && selectedQuestion.getId() == q.getId()) {
            card.getStyleClass().add("upload-card-selected");
        }
        
        // Header with icon and title
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label icon = new Label("🙋");
        icon.getStyleClass().add("upload-card-icon");
        String titleText = q.getTitle() != null ? q.getTitle() : q.getQuestionText();
        Label title = new Label(titleText);
        title.getStyleClass().add("upload-card-title");
        title.setWrapText(true);
        title.setMaxWidth(300);
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(icon, title, spacer);
        
        // Badges
        HBox badges = new HBox(8);
        badges.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label deptBadge = new Label(q.getDepartmentName() != null ? q.getDepartmentName() : "N/A");
        deptBadge.getStyleClass().addAll("badge", "badge-info");
        
        Label semBadge = new Label(q.getSemesterName() != null ? q.getSemesterName() : "N/A");
        semBadge.getStyleClass().addAll("badge", "badge-info");
        
        Label subjBadge = new Label(q.getSubject());
        subjBadge.getStyleClass().addAll("badge", "badge-purple");
        
        badges.getChildren().addAll(deptBadge, semBadge, subjBadge);
        
        // Metadata
        HBox metadata = new HBox(15);
        metadata.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label dateLabel = new Label("📅 " + (q.getCreatedAt() != null ? q.getCreatedAt() : "N/A"));
        dateLabel.getStyleClass().add("upload-card-meta");
        metadata.getChildren().addAll(dateLabel);
        
        // Status (questions don't have status, use approved)
        Label statusLabel = new Label("⚡ Approved");
        statusLabel.getStyleClass().addAll("badge", "badge-success");
        
        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button viewBtn = new Button("👁");
        viewBtn.getStyleClass().add("upload-action-btn");
        viewBtn.setOnAction(e -> {
            selectedQuestion = q;
            handleViewMyQuestion();
        });
        
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().addAll("upload-action-btn", "upload-action-btn-danger");
        deleteBtn.setOnAction(e -> {
            selectedQuestion = q;
            handleDeleteMyQuestion();
        });
        
        actions.getChildren().addAll(viewBtn, deleteBtn);
        
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, javafx.scene.layout.Priority.ALWAYS);
        bottomRow.getChildren().addAll(statusLabel, bottomSpacer, actions);
        
        card.getChildren().addAll(header, badges, metadata, bottomRow);
        
        // Make card selectable
        card.setOnMouseClicked(e -> {
            selectedQuestion = q;
            selectedNote = null;
            selectedResource = null;
            refreshMyUploads();
        });
        
        return card;
    }

    @FXML public void handleDeleteMyNote() {
        Note n = selectedNote;
        if (n == null) { showAlert("Select", "Select a note to delete."); return; }
        if (!authService.canDeleteNote(currentUser, n)) { showAlert("Denied", "You can only delete your own notes."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete note '" + n.getTitle() + "'?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            noteService.deleteNoteWithFile(n.getId(), currentUser);
            selectedNote = null;
            refreshMyUploads();
            refreshDashboard();
            EventBus.getInstance().publish(new EventBus.NotesChangedEvent());
        } catch (Exception e) {
            showAlert("Error", "Delete failed: " + e.getMessage());
        }
    }

    @FXML public void handleEditMyNote() { handleUploadNotes(); }

    @FXML public void handleViewMyNote() { 
        Note note = selectedNote;
        if (note == null) {
            return;
        }
        openFile(note.getFilePath()); 
    }

    @FXML public void handleDownloadMyNote() { 
        Note note = selectedNote;
        if (note == null) {
            return;
        }
        downloadFile(note.getFilePath(), note.getTitle(), note.getFileType());
    }

    private void downloadFile(String filePath, String title, String fileType) {
        if (filePath == null || filePath.isBlank()) {
            showAlert("Error", "No file available for download.");
            return;
        }

        Path sourcePath = Paths.get(filePath);
        if (!Files.exists(sourcePath)) {
            showAlert("Error", "Source file not found on server.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        String fileName = filePath.substring(Math.max(0, Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\')) + 1));
        if (fileName.isBlank()) {
            String ext = (fileType != null && !fileType.isBlank()) ? "." + fileType.toLowerCase() : ".pdf";
            fileName = title + ext;
        }
        fileChooser.setInitialFileName(fileName);
        fileChooser.setTitle("Save As");
        File destFile = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

        if (destFile == null) {
            return; // user canceled
        }

        try {
            Files.copy(sourcePath, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            showAlert("Download Success", "'" + title + "' has been downloaded successfully.");
        } catch (IOException e) {
            showAlert("Download Error", "Failed to download file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML public void handleDeleteMyResource() {
        Resource r = selectedResource;
        if (r == null) { showAlert("Select", "Select a resource."); return; }
        if (!authService.canDeleteResource(currentUser, r)) { showAlert("Denied", "You can only delete your own resources."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete resource?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            resourceService.deleteResourceWithFile(r.getId(), currentUser);
            selectedResource = null;
            EventBus.getInstance().publish(new EventBus.ResourcesChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
            refreshMyUploads();
            refreshDashboard();
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML public void handleViewMyResource() {
        Resource r = selectedResource;
        if (r != null) openFile(r.getFilePath());
    }

    @FXML public void handleDownloadMyResource() { 
        Resource r = selectedResource;
        if (r == null) {
            return;
        }
        downloadFile(r.getFilePath(), r.getTitle(), r.getFileType());
    }

    @FXML public void handleDeleteMyQuestion() {
        Question q = selectedQuestion;
        if (q == null) return;
        if (!authService.canDeleteQuestion(currentUser, q)) { showAlert("Denied", "You can only delete your own questions."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this question?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            questionService.deleteQuestion(q.getId(), currentUser);
            selectedQuestion = null;
            refreshMyUploads();
            EventBus.getInstance().publish(new EventBus.QuestionsChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML public void handleViewMyQuestion() {
        Question q = selectedQuestion;
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
            
            // Initialize new task stat labels
            lblCompletedTasks.setText("0");
            lblPendingTasks.setText("0");
            lblInProgressTasks.setText("0");
            lblTaskCompletionPercentage.setText("0%");
            
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
        int totalTasks = taskService.getTotalTaskCount(userId);
        int completedTasks = taskService.getCompletedTaskCount(userId);
        int pendingTasks = taskService.getPendingTaskCount(userId);
        int inProgressTasks = taskService.getInProgressTaskCount(userId);
        int progress = taskService.getCompletedPercentage(userId);

        lblTotalTasks.setText(String.valueOf(totalTasks));
        lblProgressPercentageStat.setText(progress + "%");
        lblProgressPercentage.setText(progress + "%");
        overallProgressBar.setProgress((double) progress / 100);
        
        // Update new task stat labels
        lblCompletedTasks.setText(String.valueOf(completedTasks));
        lblPendingTasks.setText(String.valueOf(pendingTasks));
        lblInProgressTasks.setText(String.valueOf(inProgressTasks));
        lblTaskCompletionPercentage.setText(progress + "%");
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
        Subject selectedSubjectObj = subjectComboBox.getValue();
        String selectedSubjectName = selectedSubjectObj != null ? selectedSubjectObj.getName() : null;

        Integer deptId = AcademicFilterHelper.resolveDepartmentId(selectedDept);
        Integer semId = AcademicFilterHelper.resolveSemesterId(selectedSem);

        // Find subjectId if subject selected
        Integer subjectId = null;
        if (selectedSubjectName != null) {
            try {
                List<Subject> subjects = academicService.getSubjects(deptId, semId);
                for (Subject subj : subjects) {
                    if (subj.getName().equalsIgnoreCase(selectedSubjectName)) {
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
            Scene scene = new Scene(dialogContent, 750, 800);
            dialog.setScene(scene);
            dialog.setResizable(true);
            dialog.setMinWidth(650);
            dialog.setMinHeight(700);
            dialog.showAndWait();
            
            refreshDashboard();
        } catch (IOException e) {
            showAlert("Error", "Failed to open create note dialog: " + e.getMessage());
            e.printStackTrace();
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
        
        myUploadsSearchField.textProperty().addListener((obs, old, newVal) -> {
            refreshMyUploads();
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
