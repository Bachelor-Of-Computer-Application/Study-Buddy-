package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Department;
import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.models.User;
import com.studybuddy.models.UserActivity;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.AuthorizationService;
import com.studybuddy.services.ResourceService;
import com.studybuddy.utils.AcademicFilterHelper;
import com.studybuddy.utils.EventBus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for ResourcesView.fxml.
 *
 * Fixes applied:
 *  - subjectFilter now loads from SubjectDAO (via ResourceService.getAllSubjectNames()).
 *  - Upload dialog now uses cascading Department→Semester→Subject ComboBoxes.
 *  - subjectId is passed to ResourceDAO so the FK is stored on upload.
 *  - loadMockResources() is removed; DB failure shows empty-state, not fake data.
 */
public class ResourcesController {

    // ── Filter bar ────────────────────────────────────────────────────────────
    @FXML private BorderPane rootPane;
    @FXML private TabPane resourcesTabPane;
    @FXML private TextField               searchField;
    @FXML private ComboBox<Department>    departmentFilter;
    @FXML private ComboBox<Semester>      semesterFilter;
    @FXML private ComboBox<Subject>       subjectFilter;

    // ── Community library ─────────────────────────────────────────────────────
    @FXML private FlowPane resourcesFlowPane;
    @FXML private VBox emptyState;

    // ── Upload history table ──────────────────────────────────────────────────
    @FXML private TableView<Resource>      historyTable;
    @FXML private TableColumn<Resource, String>  titleCol;
    @FXML private TableColumn<Resource, String>  subjectCol;
    @FXML private TableColumn<Resource, String>  fileNameCol;
    @FXML private TableColumn<Resource, String>  dateCol;
    @FXML private TableColumn<Resource, String>  statusCol;

    @FXML private TableColumn<Resource, Void> actionsCol;

    private final ResourceService resourceService = new ResourceService();
    private final AcademicService academicService  = AcademicService.getInstance();
    private final AuthorizationService authService = AuthorizationService.getInstance();
    private final com.studybuddy.dao.UserActivityDAO activityDAO = new com.studybuddy.dao.UserActivityDAO();

    private User currentUser;

    private List<Resource> activeResources     = new ArrayList<>();
    private List<Resource> userUploadedHistory = new ArrayList<>();

    // =========================
    // SUBJECT FILTER LOADING
    // =========================

    /**
     * Populates the subject filter ComboBox with all active subject names from the
     * canonical Subjects table via ResourceService → SubjectDAO.
     * Called on initialize and whenever the cascade is reset.
     */
    public void loadSubjectFilter() {
        List<Subject> subjects = academicService.getAllActiveSubjects();
        subjectFilter.setItems(FXCollections.observableArrayList(subjects));
    }

    // =========================
    // INITIALISE
    // =========================

    @FXML
    public void initialize() {
        currentUser = App.getCurrentUser();
        setupHistoryTableColumns();
        AcademicFilterHelper.setupFilterBar(academicService, departmentFilter, semesterFilter, subjectFilter);
        loadResources();

        EventBus.getInstance().subscribe(EventBus.ResourcesChangedEvent.class, (event) -> loadResources());
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, (event) -> loadResources());
    }

    /**
     * Populates the Department filter and wires the cascade chain.
     * When a department is chosen, semesters load; when a semester is chosen,
     * the subject filter narrows to that semester's subjects.
     */
    private void loadFilterDepartments() {
        if (departmentFilter == null) return;
        if (semesterFilter != null) {
            semesterFilter.setDisable(true);
        }
        try {
            departmentFilter.setItems(FXCollections.observableArrayList(
                    academicService.getAllActiveDepartments()));
        } catch (Exception e) {
            System.err.println("[ResourcesController] Dept filter load failed: " + e.getMessage());
        }

        departmentFilter.setOnAction(e -> {
            Department dept = departmentFilter.getValue();
            if (semesterFilter != null) {
                semesterFilter.getItems().clear();
                semesterFilter.setValue(null);
                semesterFilter.setDisable(dept == null);
            }
            subjectFilter.getItems().clear();
            subjectFilter.setValue(null);

            if (dept != null && semesterFilter != null) {
                try {
                    semesterFilter.setItems(FXCollections.observableArrayList(
                            academicService.getSemestersByDepartment(dept.getId())));
                } catch (Exception ex) {
                    System.err.println("[ResourcesController] Sem filter load failed: " + ex.getMessage());
                }
            } else {
                loadSubjectFilter();
            }
        });

        if (semesterFilter != null) {
            semesterFilter.setOnAction(e -> {
                Semester sem = semesterFilter.getValue();
                subjectFilter.getItems().clear();
                subjectFilter.setValue(null);
                if (sem != null) {
                    try {
                        // Narrow subject filter to chosen semester
                        List<Subject> semSubjects = academicService
                                .getSubjectsBySemester(sem.getId());
                        subjectFilter.setItems(FXCollections.observableArrayList(semSubjects));
                    } catch (Exception ex) {
                        System.err.println("[ResourcesController] Sub filter load failed: " + ex.getMessage());
                    }
                } else {
                    loadSubjectFilter(); // reset to full list
                }
            });
        }
    }

    private void setupHistoryTableColumns() {
        if (historyTable != null) {
            historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }
        if (titleCol   != null) titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (subjectCol != null) subjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (fileNameCol != null) fileNameCol.setCellValueFactory(cellData -> {
            String path = cellData.getValue().getFilePath();
            if (path == null) return new SimpleStringProperty("");
            int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            return new SimpleStringProperty(idx >= 0 ? path.substring(idx + 1) : path);
        });
        if (dateCol   != null) dateCol.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
        if (statusCol != null) statusCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isActive() ? "Approved" : "Pending"));

        if (actionsCol != null) {
            actionsCol.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn = new Button("👁");
                private final Button deleteBtn = new Button("🗑");
                private final HBox box = new HBox(6, viewBtn, deleteBtn);

                {
                    viewBtn.getStyleClass().add("btn-action-icon");
                    deleteBtn.getStyleClass().add("btn-action-delete");
                    viewBtn.setOnAction(e -> {
                        Resource r = getTableView().getItems().get(getIndex());
                        if (r != null) handlePreview(r);
                    });
                    deleteBtn.setOnAction(e -> {
                        Resource r = getTableView().getItems().get(getIndex());
                        if (r != null) deleteResource(r);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }
    }

    // =========================
    // LOAD DATA
    // =========================

    private void loadResources() {
        try {
            activeResources = resourceService.getAllActiveResources();
            if (activeResources == null) activeResources = new ArrayList<>();
            activeResources = filterVisibleResources(activeResources);
            displayResources(activeResources);

            int userId = currentUser != null ? currentUser.getId() : 1;
            userUploadedHistory = resourceService.getResourcesByUser(userId);
            if (userUploadedHistory == null) userUploadedHistory = new ArrayList<>();
            if (historyTable != null)
                historyTable.setItems(FXCollections.observableArrayList(userUploadedHistory));

        } catch (Exception e) {
            System.err.println("[ResourcesController] Failed to load resources: " + e.getMessage());
            activeResources = new ArrayList<>();
            displayResources(activeResources);
        }
    }

    private List<Resource> filterVisibleResources(List<Resource> resources) {
        List<Subject> allSubjects = academicService.getAllActiveSubjects();
        Map<Integer, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (a, b) -> a));
        return resources.stream()
                .filter(r -> AcademicFilterHelper.isVisibleToUser(
                        r.getDepartmentId(), r.getSemesterId(),
                        r.getSubjectId() > 0 ? r.getSubjectId() : null,
                        subjectMap, currentUser, academicService))
                .collect(Collectors.toList());
    }

    // =========================
    // DISPLAY CARDS
    // =========================

    private void displayResources(List<Resource> resources) {
        resourcesFlowPane.getChildren().clear();

        boolean isEmpty = (resources == null || resources.isEmpty());
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);

        if (isEmpty) return;

        for (Resource r : resources) {
            resourcesFlowPane.getChildren().add(createResourceCard(r));
        }
    }

    private VBox createResourceCard(Resource r) {
        VBox card = new VBox(8);
        card.getStyleClass().add("resource-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(260.0);
        card.setMaxWidth(320.0);

        String type = r.getFileType() != null ? r.getFileType() : guessTypeFromPath(r.getFilePath());
        Label iconLabel = new Label(fileTypeIcon(type) + " " + type.toUpperCase());
        iconLabel.getStyleClass().add("resource-icon");

        Label titleLabel = new Label(r.getTitle());
        titleLabel.getStyleClass().add("resource-title");
        titleLabel.setWrapText(true);
        
        // Build author label with full name, dept, sem
        StringBuilder authorText = new StringBuilder("👤 Uploaded by: ");
        String fullName = r.getUserFullName();
        if (fullName != null && !fullName.isEmpty()) {
            authorText.append(fullName);
        } else {
            authorText.append("User ").append(r.getUploadedBy());
        }
        String dept = r.getUserDepartment();
        String sem = r.getUserSemester();
        if (dept != null || sem != null) {
            authorText.append(" (");
            if (dept != null) authorText.append(dept);
            if (dept != null && sem != null) authorText.append(" • ");
            if (sem != null) authorText.append(sem);
            authorText.append(")");
        }
        Label authorLabel = new Label(authorText.toString());
        authorLabel.getStyleClass().add("resource-meta");

        String subjectText = r.getSubject() != null ? r.getSubject() : "—";
        Label subjectLabel = new Label("📚 " + subjectText);
        subjectLabel.getStyleClass().add("resource-subject");

        String descText = r.getDescription() != null ? r.getDescription() : "";
        Label descLabel = new Label(descText);
        descLabel.getStyleClass().add("resource-meta");
        descLabel.setWrapText(true);
        descLabel.setPrefHeight(40);

        HBox actionBox = new HBox(10);
        Button previewBtn = new Button("👁 Preview");
        previewBtn.getStyleClass().add("btn-action-view");
        previewBtn.setOnAction(e -> handlePreview(r));

        Button downloadBtn = new Button("📥 Download");
        downloadBtn.getStyleClass().add("btn-action-download");
        downloadBtn.setOnAction(e -> handleDownload(r));

        actionBox.getChildren().addAll(previewBtn, downloadBtn);
        if (authService.canDeleteResource(currentUser, r)) {
            Button deleteBtn = new Button("🗑 Delete");
            deleteBtn.getStyleClass().add("btn-action-delete");
            deleteBtn.setOnAction(e -> deleteResource(r));
            actionBox.getChildren().add(deleteBtn);
        }
        card.getChildren().addAll(iconLabel, titleLabel, authorLabel, subjectLabel, descLabel, actionBox);
        return card;
    }

    private String guessTypeFromPath(String path) {
        if (path == null || !path.contains(".")) return "FILE";
        return path.substring(path.lastIndexOf('.') + 1).toUpperCase();
    }

    private String fileTypeIcon(String fileType) {
        if (fileType == null) return "📄";
        return switch (fileType.toUpperCase()) {
            case "PDF" -> "📕";
            case "DOC", "DOCX" -> "📘";
            case "PPT", "PPTX" -> "📙";
            case "XLS", "XLSX" -> "📗";
            case "PNG", "JPG", "JPEG", "GIF", "WEBP" -> "🖼";
            case "ZIP", "RAR", "7Z" -> "🗜";
            case "MP4", "AVI", "MKV", "MOV" -> "🎬";
            case "MP3", "WAV", "AAC" -> "🎵";
            default -> "📄";
        };
    }

    // =========================
    // FILTERS
    // =========================

    @FXML
    public void applyFilters() {
        String query = searchField.getText().trim().toLowerCase();
        Department dept = departmentFilter.getValue();
        Semester sem = semesterFilter.getValue();
        Subject selectedSubject = subjectFilter.getValue();
        String subject = selectedSubject != null ? selectedSubject.getName() : null;

        List<Subject> allSubjects = academicService.getAllActiveSubjects();
        Map<Integer, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (s1, unused) -> s1));

        List<Resource> filtered = activeResources.stream()
                .filter(r -> {
                    String t = r.getTitle() != null ? r.getTitle().toLowerCase() : "";
                    String s = r.getSubject() != null ? r.getSubject() : "";
                    if (!query.isEmpty() && !t.contains(query)) return false;
                    if (subject != null && !s.equalsIgnoreCase(subject)) return false;
                    return AcademicFilterHelper.matchesDeptSemFilter(
                            r.getDepartmentId(), r.getSemesterId(), r.getSubjectId(),
                            dept, sem, subjectMap, allSubjects, s);
                })
                .collect(Collectors.toList());

        displayResources(filtered);
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        AcademicFilterHelper.resetFilters(academicService, departmentFilter, semesterFilter, subjectFilter);
        displayResources(activeResources);
    }

    private void deleteResource(Resource r) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must be logged in.");
            return;
        }
        if (!authService.canDeleteResource(currentUser, r)) {
            showAlert(Alert.AlertType.ERROR, "Denied", "You can only delete your own resources.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete resource '" + r.getTitle() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            resourceService.deleteResourceWithFile(r.getId(), currentUser);
            EventBus.getInstance().publish(new EventBus.ResourcesChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
            loadResources();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Resource deleted successfully.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error", "Delete failed: " + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // =========================
    // UPLOAD DIALOG — cascading Dept → Semester → Subject
    // =========================

    @FXML
    public void handleUploadResource() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose PDF Resource");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(searchField.getScene().getWindow());
        if (file == null) return;

        openUploadDialog(file);
    }

    private void openUploadDialog(File file) {
        Stage dialog = new Stage();
        dialog.setTitle("Upload Resource Details");
        dialog.initModality(Modality.APPLICATION_MODAL);

        // ── Fields ──────────────────────────────────────────────────────────
        TextField titleTxt = new TextField();
        titleTxt.setPromptText("Enter resource title");

        // Cascading ComboBoxes — Department → Semester → Subject
        ComboBox<Department> deptCombo = new ComboBox<>();
        deptCombo.setPromptText("Select Department");
        deptCombo.setPrefWidth(280);

        ComboBox<Semester> semCombo = new ComboBox<>();
        semCombo.setPromptText("Select Semester");
        semCombo.setPrefWidth(280);

        ComboBox<Subject> subCombo = new ComboBox<>();
        subCombo.setPromptText("Select Subject");
        subCombo.setPrefWidth(280);

        TextArea descTxt = new TextArea();
        descTxt.setPromptText("Enter short description");
        descTxt.setPrefHeight(70);

        deptCombo.setItems(AcademicFilterHelper.departmentsForFilter(academicService));
        deptCombo.setValue(AcademicFilterHelper.allDepartments());
        semCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
        semCombo.setValue(AcademicFilterHelper.allSemesters());
        semCombo.setDisable(false);

        AcademicFilterHelper.wireCascade(academicService, deptCombo, semCombo, subCombo,
                () -> AcademicFilterHelper.loadSubjects(academicService, deptCombo.getValue(), semCombo.getValue(), subCombo));
        AcademicFilterHelper.loadSubjects(academicService, deptCombo.getValue(), semCombo.getValue(), subCombo);

        // ── Submit button ────────────────────────────────────────────────────
        Button submitBtn = new Button("Submit for Approval");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setOnAction(e -> {
            if (App.getCurrentUser() == null) {
                showAlert(Alert.AlertType.WARNING, "Login Required", "Please log in to upload resources.");
                return;
            }
            if (titleTxt.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing Fields", "Title is required.");
                return;
            }
            Department dept = deptCombo.getValue();
            Semester sem = semCombo.getValue();
            Subject selectedSubject = subCombo.getValue();
            if (!AcademicFilterHelper.isAllSemesters(sem) && selectedSubject == null) {
                showAlert(Alert.AlertType.WARNING, "Missing Fields",
                        "Select a Subject when a specific semester is chosen.");
                return;
            }

            Note noteToShare = new Note();
            noteToShare.setId(0);
            noteToShare.setTitle(titleTxt.getText().trim());
            noteToShare.setDepartmentId(AcademicFilterHelper.resolveDepartmentId(dept));
            noteToShare.setSemesterId(AcademicFilterHelper.resolveSemesterId(sem));
            if (selectedSubject != null) {
                noteToShare.setSubject(selectedSubject.getName());
                noteToShare.setSubjectId(selectedSubject.getId());
            } else {
                noteToShare.setSubject(AcademicFilterHelper.isAllDepartments(dept) ? "All Departments" : "General");
            }
            noteToShare.setSource("Community Upload");
            noteToShare.setUploadDate(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            noteToShare.setFileName(file.getName());
            noteToShare.setFileType("PDF");
            noteToShare.setDescription(descTxt.getText().trim());
            noteToShare.setUserId(App.getCurrentUser().getId());
            noteToShare.setPrivate(false);

            try {
                boolean isAdmin = "admin".equalsIgnoreCase(App.getCurrentUser().getRole());
                // Use FileStorageService to store the file properly
                com.studybuddy.services.FileStorageService storage = com.studybuddy.services.FileStorageService.getInstance();
                String storedPath = storage.storeFile(file, "resources");
                resourceService.shareAsResource(noteToShare, storedPath, isAdmin);
                
                // Log activity
                if (App.getCurrentUser() != null) {
                    UserActivity activity = new UserActivity(
                            App.getCurrentUser().getId(),
                            App.getCurrentUser().getDisplayFullName(),
                            "UPLOAD_RESOURCE",
                            "RESOURCE",
                            noteToShare.getTitle()
                    );
                    try {
                        activityDAO.logActivity(activity);
                    } catch (SQLException ex) {
                        showAlert(Alert.AlertType.WARNING, "Activity Log Failed", "Could not log upload activity: " + ex.getMessage());
                    }
                }
                
                // Publish events
                EventBus.getInstance().publish(new EventBus.ResourcesChangedEvent());
                EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
                
                Alert success = new Alert(Alert.AlertType.INFORMATION,
                        "Resource submitted for admin approval!");
                success.setHeaderText(null);
                success.showAndWait();
                dialog.close();
                loadResources();   // Refresh the view
                loadSubjectFilter(); // Subject list may have grown
            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR,
                        "Upload failed: " + ex.getMessage());
                err.setHeaderText("Upload Error");
                err.showAndWait();
                ex.printStackTrace();
            }
        });

        // ── Layout ───────────────────────────────────────────────────────────
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(20));

        form.add(new Label("Selected file:"),  0, 0);
        form.add(new Label(file.getName()),    1, 0);
        form.add(new Label("Title:"),          0, 1);
        form.add(titleTxt,                     1, 1);
        form.add(new Label("Department:"),     0, 2);
        form.add(deptCombo,                    1, 2);
        form.add(new Label("Semester:"),       0, 3);
        form.add(semCombo,                     1, 3);
        form.add(new Label("Subject:"),        0, 4);
        form.add(subCombo,                     1, 4);
        form.add(new Label("Description:"),    0, 5);
        form.add(descTxt,                      1, 5);
        form.add(submitBtn,                    1, 6);

        dialog.setScene(new Scene(form, 480, 420));
        dialog.setMinWidth(440);
        dialog.showAndWait();
    }

    // =========================
    // PREVIEW / DOWNLOAD
    // =========================

    private void handlePreview(Resource r) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Resource Preview");
        alert.setHeaderText(r.getTitle());
        alert.setContentText(
                "Subject: "     + nullSafe(r.getSubject())     + "\n" +
                "Source: "      + nullSafe(r.getSource())       + "\n" +
                "Description: " + nullSafe(r.getDescription())  + "\n" +
                "File: "        + nullSafe(r.getFilePath()));
        alert.showAndWait();
    }

    private void handleDownload(Resource r) {
        if (r == null || r.getFilePath() == null || r.getFilePath().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Download Error", "No file available for download.");
            return;
        }

        Path sourcePath = Paths.get(r.getFilePath());
        if (!Files.exists(sourcePath)) {
            showAlert(Alert.AlertType.ERROR, "Download Error", "Source file not found on server.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        String fileName = r.getFilePath().substring(Math.max(0, Math.max(r.getFilePath().lastIndexOf('/'), r.getFilePath().lastIndexOf('\\')) + 1));
        if (fileName.isBlank()) {
            fileName = r.getTitle() + ".pdf";
        }
        fileChooser.setInitialFileName(fileName);
        fileChooser.setTitle("Save Resource As");
        File destFile = fileChooser.showSaveDialog(searchField.getScene().getWindow());

        if (destFile == null) {
            return; // user canceled
        }

        try {
            Files.copy(sourcePath, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            showAlert(Alert.AlertType.INFORMATION, "Download Success", 
                "'" + r.getTitle() + "' has been downloaded successfully.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Download Error", 
                "Failed to download file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String nullSafe(String s) { return s != null ? s : ""; }
}