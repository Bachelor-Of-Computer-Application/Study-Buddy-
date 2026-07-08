package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Department;
import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.models.UserActivity;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.ResourceService;
import com.studybuddy.utils.EventBus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    @FXML private TextField               searchField;
    @FXML private ComboBox<Department>    departmentFilter;
    @FXML private ComboBox<Semester>      semesterFilter;
    @FXML private ComboBox<String>        subjectFilter;

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

    private final ResourceService resourceService = new ResourceService();
    private final AcademicService academicService  = AcademicService.getInstance();
    private final com.studybuddy.dao.UserActivityDAO activityDAO = new com.studybuddy.dao.UserActivityDAO();

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
        List<String> subjects = resourceService.getAllSubjectNames();
        subjectFilter.setItems(FXCollections.observableArrayList(subjects));
    }

    // =========================
    // INITIALISE
    // =========================

    @FXML
    public void initialize() {
        setupHistoryTableColumns();
        loadFilterDepartments();   // cascading filter: Dept → Sem → Subject
        loadSubjectFilter();       // flat subject list for direct filtering
        loadResources();
        
        // Subscribe to EventBus events
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
                        List<String> semSubjects = academicService
                                .getSubjectsBySemester(sem.getId())
                                .stream()
                                .map(Subject::getName)
                                .collect(Collectors.toList());
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
    }

    // =========================
    // LOAD DATA
    // =========================

    private void loadResources() {
        try {
            activeResources = resourceService.getAllActiveResources();
            if (activeResources == null) activeResources = new ArrayList<>();
            displayResources(activeResources);

            int userId = App.getCurrentUser() != null ? App.getCurrentUser().getId() : 1;
            userUploadedHistory = resourceService.getResourcesByUser(userId);
            if (userUploadedHistory == null) userUploadedHistory = new ArrayList<>();
            if (historyTable != null)
                historyTable.setItems(FXCollections.observableArrayList(userUploadedHistory));

        } catch (Exception e) {
            System.err.println("[ResourcesController] Failed to load resources: " + e.getMessage());
            e.printStackTrace();
            // Show empty state — no mock fallback data
            activeResources = new ArrayList<>();
            displayResources(activeResources);
        }
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
        card.setPadding(new Insets(15));
        card.setPrefWidth(260.0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12px;" +
                " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

        Label iconLabel = new Label("📕 PDF");
        iconLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545; -fx-font-size: 11px;");

        Label titleLabel = new Label(r.getTitle());
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
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
        authorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        String subjectText = r.getSubject() != null ? r.getSubject() : "—";
        Label subjectLabel = new Label("📚 " + subjectText);
        subjectLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");

        String descText = r.getDescription() != null ? r.getDescription() : "";
        Label descLabel = new Label(descText);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        descLabel.setWrapText(true);
        descLabel.setPrefHeight(40);

        HBox actionBox = new HBox(10);
        Button previewBtn = new Button("👁️ Preview");
        previewBtn.setStyle("-fx-background-color: #f1f5f9; -fx-font-size: 11px; -fx-cursor: hand;");
        previewBtn.setOnAction(e -> handlePreview(r));

        Button downloadBtn = new Button("📥 Download");
        downloadBtn.setStyle("-fx-background-color: #e0f2fe; -fx-font-size: 11px; -fx-cursor: hand;");
        downloadBtn.setOnAction(e -> handleDownload(r));

        actionBox.getChildren().addAll(previewBtn, downloadBtn);
        card.getChildren().addAll(iconLabel, titleLabel, authorLabel, subjectLabel, descLabel, actionBox);
        return card;
    }

    // =========================
    // FILTERS
    // =========================

    @FXML
    public void applyFilters() {
        String query = searchField.getText().trim().toLowerCase();
        Department dept = departmentFilter.getValue();
        Semester sem = semesterFilter.getValue();
        String subject = subjectFilter.getValue();

        List<Subject> allSubjects = academicService.getAllActiveSubjects();
        java.util.Map<Integer, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (s1, unused) -> s1));

        List<Resource> filtered = activeResources.stream()
                .filter(r -> {
                    String t = r.getTitle() != null ? r.getTitle().toLowerCase() : "";
                    String s = r.getSubject() != null ? r.getSubject() : "";
                    boolean matchesQuery = query.isEmpty() || t.contains(query);
                    if (!matchesQuery) return false;

                    boolean matchesSub = subject == null || s.equalsIgnoreCase(subject);
                    if (!matchesSub) return false;

                    if (dept != null || sem != null) {
                        Subject subModel = subjectMap.get(r.getSubjectId());
                        if (subModel == null) {
                            return allSubjects.stream().anyMatch(sub ->
                                sub.getName().equalsIgnoreCase(s) &&
                                (dept == null || sub.getDepartmentId() == dept.getId()) &&
                                (sem == null || sub.getSemesterId() == sem.getId())
                            );
                        }
                        if (dept != null && subModel.getDepartmentId() != dept.getId()) {
                            return false;
                        }
                        if (sem != null && subModel.getSemesterId() != sem.getId()) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        displayResources(filtered);
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        if (departmentFilter != null) departmentFilter.getSelectionModel().clearSelection();
        if (semesterFilter != null) {
            semesterFilter.getItems().clear();
            semesterFilter.setValue(null);
            semesterFilter.setDisable(true);
        }
        subjectFilter.getSelectionModel().clearSelection();
        loadSubjectFilter();
        displayResources(activeResources);
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
        semCombo.setDisable(true);
        semCombo.setPrefWidth(280);

        ComboBox<Subject> subCombo = new ComboBox<>();
        subCombo.setPromptText("Select Subject");
        subCombo.setDisable(true);
        subCombo.setPrefWidth(280);

        TextArea descTxt = new TextArea();
        descTxt.setPromptText("Enter short description");
        descTxt.setPrefHeight(70);

        // ── Cascade logic ────────────────────────────────────────────────────
        try {
            List<Department> departments = academicService.getAllActiveDepartments();
            deptCombo.setItems(FXCollections.observableArrayList(departments));
        } catch (Exception e) {
            System.err.println("[ResourcesController] Failed to load departments: " + e.getMessage());
        }

        deptCombo.setOnAction(e -> {
            Department dept = deptCombo.getValue();
            semCombo.getItems().clear();
            subCombo.getItems().clear();
            semCombo.setValue(null);
            subCombo.setValue(null);
            semCombo.setDisable(dept == null);
            subCombo.setDisable(true);

            if (dept != null) {
                try {
                    semCombo.setItems(FXCollections.observableArrayList(
                            academicService.getSemestersByDepartment(dept.getId())));
                } catch (Exception ex) {
                    System.err.println("[ResourcesController] Failed to load semesters: " + ex.getMessage());
                }
            }
        });

        semCombo.setOnAction(e -> {
            Semester sem = semCombo.getValue();
            subCombo.getItems().clear();
            subCombo.setValue(null);
            subCombo.setDisable(sem == null);

            if (sem != null) {
                try {
                    subCombo.setItems(FXCollections.observableArrayList(
                            academicService.getSubjectsBySemester(sem.getId())));
                } catch (Exception ex) {
                    System.err.println("[ResourcesController] Failed to load subjects: " + ex.getMessage());
                }
            }
        });

        // ── Submit button ────────────────────────────────────────────────────
        Button submitBtn = new Button("Submit for Approval");
        submitBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white;" +
                " -fx-font-weight: bold; -fx-cursor: hand;");
        submitBtn.setOnAction(e -> {
            if (titleTxt.getText().trim().isEmpty() || subCombo.getValue() == null) {
                Alert warn = new Alert(Alert.AlertType.WARNING,
                        "Title and Subject are required.\n" +
                        "Please select Department → Semester → Subject.");
                warn.setHeaderText("Missing Fields");
                warn.showAndWait();
                return;
            }

            Subject selectedSubject = subCombo.getValue();

            Note noteToShare = new Note();
            noteToShare.setId(0); // No linked Note row
            noteToShare.setTitle(titleTxt.getText().trim());
            noteToShare.setSubject(selectedSubject.getName());
            noteToShare.setSubjectId(selectedSubject.getId());  // FK to Subjects table
            noteToShare.setSource("Community Upload");
            noteToShare.setUploadDate(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            noteToShare.setFileName(file.getName());
            noteToShare.setFileType("PDF");
            noteToShare.setDescription(descTxt.getText().trim());
            noteToShare.setUserId(
                    App.getCurrentUser() != null ? App.getCurrentUser().getId() : 1);
            noteToShare.setPrivate(false);

            try {
                boolean isAdmin = App.getCurrentUser() != null && "admin".equalsIgnoreCase(App.getCurrentUser().getRole());
                resourceService.shareAsResource(noteToShare, file.getAbsolutePath(), isAdmin);
                
                // Log activity
                if (App.getCurrentUser() != null) {
                    UserActivity activity = new UserActivity(
                            App.getCurrentUser().getId(),
                            App.getCurrentUser().getFullName() != null ? App.getCurrentUser().getFullName() : App.getCurrentUser().getName(),
                            "UPLOAD_RESOURCE",
                            "RESOURCE",
                            noteToShare.getTitle()
                    );
                    try {
                        activityDAO.logActivity(activity);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
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

        dialog.setScene(new Scene(form, 430, 400));
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Download");
        alert.setHeaderText("Download Success");
        alert.setContentText("'" + r.getTitle() + "' has been downloaded successfully.");
        alert.showAndWait();
    }

    private String nullSafe(String s) { return s != null ? s : ""; }
}