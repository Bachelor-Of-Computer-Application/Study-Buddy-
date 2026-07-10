package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.models.Department;
import com.studybuddy.models.Resource;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.models.UserActivity;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.FileStorageService;
import com.studybuddy.services.ResourceService;
import com.studybuddy.utils.AcademicFilterHelper;
import com.studybuddy.utils.EventBus;
import com.studybuddy.utils.SessionManager;
import com.studybuddy.dao.UserActivityDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resource management: activate/deactivate, delete, preview/download, search, sort, download count.
 */
public class AdminResourcesController {

    // Upload form
    @FXML private TextField uploadNameField;
    @FXML private TextArea uploadDescriptionField;
    @FXML private ComboBox<Department> uploadDepartmentCombo;
    @FXML private ComboBox<Semester> uploadSemesterCombo;
    @FXML private ComboBox<Subject> uploadSubjectCombo;
    @FXML private ComboBox<String> uploadCategoryCombo;
    @FXML private TextField uploadFileField;
    @FXML private ComboBox<String> uploadStatusCombo;
    private File selectedUploadFile;

    @FXML private TextField           searchField;
    @FXML private ComboBox<Department> departmentFilter;
    @FXML private ComboBox<Semester>   semesterFilter;
    @FXML private ComboBox<Subject>    subjectFilter;
    @FXML private ComboBox<String>     statusFilter;

    @FXML private TableView<Resource>              resourcesTable;
    @FXML private TableColumn<Resource, Integer>   colId;
    @FXML private TableColumn<Resource, String>    colTitle;
    @FXML private TableColumn<Resource, String>    colSubject;
    @FXML private TableColumn<Resource, String>    colUploader;
    @FXML private TableColumn<Resource, Integer>   colDownloads;
    @FXML private TableColumn<Resource, String>    colStatus;
    @FXML private TableColumn<Resource, String>    colDate;

    @FXML private Label  lblPageNumber;
    @FXML private Label  lblTotalCount;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    private final AdminService adminService = AdminService.getInstance();
    private final ResourceService resourceService = new ResourceService();
    private final AcademicService academicService  = AcademicService.getInstance();
    private final ObservableList<Resource> masterList = FXCollections.observableArrayList();
    private List<Resource> filteredList = new ArrayList<>();
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    @FXML
    public void initialize() {
        setupUploadForm();
        setupColumns();
        setupFilters();
        loadData();
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void setupUploadForm() {
        if (uploadCategoryCombo != null) {
            uploadCategoryCombo.setItems(FXCollections.observableArrayList(
                    "Lecture Notes", "Textbook", "Past Paper", "Assignment", "Lab Manual", "Other"));
        }
        if (uploadStatusCombo != null) {
            uploadStatusCombo.setItems(FXCollections.observableArrayList("Approved", "Pending", "Hidden", "Rejected"));
            uploadStatusCombo.setValue("Approved");
        }
        if (uploadDepartmentCombo != null) {
            uploadDepartmentCombo.setItems(AcademicFilterHelper.departmentsForFilter(academicService));
            uploadDepartmentCombo.setValue(AcademicFilterHelper.allDepartments());
        }
        if (uploadSemesterCombo != null) {
            uploadSemesterCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
            uploadSemesterCombo.setValue(AcademicFilterHelper.allSemesters());
            uploadSemesterCombo.setDisable(false);
        }
        AcademicFilterHelper.wireCascade(academicService, uploadDepartmentCombo, uploadSemesterCombo, uploadSubjectCombo,
                () -> AcademicFilterHelper.loadSubjects(academicService,
                        uploadDepartmentCombo.getValue(), uploadSemesterCombo.getValue(),
                        uploadSubjectCombo));
        AcademicFilterHelper.loadSubjects(academicService,
                uploadDepartmentCombo.getValue(), uploadSemesterCombo.getValue(),
                uploadSubjectCombo);
    }

    @FXML public void handleSelectUploadFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Resource File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Study Materials",
                "*.pdf", "*.docx", "*.doc", "*.ppt", "*.pptx", "*.zip", "*.png", "*.jpg", "*.jpeg"));
        File f = fc.showOpenDialog(resourcesTable.getScene().getWindow());
        if (f != null) { selectedUploadFile = f; uploadFileField.setText(f.getName()); }
    }

    @FXML public void handleUploadResource() {
        if (uploadNameField.getText() == null || uploadNameField.getText().trim().isEmpty()) {
            warn("Resource name is required."); return;
        }
        if (uploadDepartmentCombo.getValue() == null || uploadSemesterCombo.getValue() == null) {
            warn("Select department and semester (or All options)."); return;
        }
        if (!AcademicFilterHelper.isAllSemesters(uploadSemesterCombo.getValue()) && uploadSubjectCombo.getValue() == null) {
            warn("Select a subject when a specific semester is chosen."); return;
        }
        if (selectedUploadFile == null) { warn("Select a file."); return; }
        try {
            Resource r = new Resource();
            r.setTitle(uploadNameField.getText().trim());
            r.setDescription(uploadDescriptionField.getText());
            r.setDepartmentId(AcademicFilterHelper.resolveDepartmentId(uploadDepartmentCombo.getValue()));
            r.setSemesterId(AcademicFilterHelper.resolveSemesterId(uploadSemesterCombo.getValue()));
            Subject sub = uploadSubjectCombo.getValue();
            if (sub != null) {
                r.setSubject(sub.getName());
                r.setSubjectId(sub.getId());
            } else {
                r.setSubject(AcademicFilterHelper.isAllDepartments(uploadDepartmentCombo.getValue()) ? "All Departments" : "General");
            }
            r.setCategory(uploadCategoryCombo.getValue());
            r.setUploadedBy(SessionManager.getCurrentAdmin() != null ? SessionManager.getCurrentAdmin().getId() : 1);
            boolean autoApprove = "Approved".equalsIgnoreCase(uploadStatusCombo.getValue());
            resourceService.createResource(r, selectedUploadFile, autoApprove);
            info("Resource uploaded successfully.");
            resetUploadForm();
            loadData();
            EventBus.getInstance().publish(new EventBus.ResourcesChangedEvent());
        } catch (Exception e) {
            warn("Upload failed: " + e.getMessage());
        }
    }

    @FXML public void handleUpdateResource() {
        Resource sel = selected(); if (sel == null) return;
        try {
            sel.setTitle(uploadNameField.getText().trim());
            sel.setDescription(uploadDescriptionField.getText());
            Subject sub = uploadSubjectCombo.getValue();
            if (sub != null) { sel.setSubject(sub.getName()); sel.setSubjectId(sub.getId()); }
            sel.setDepartmentId(AcademicFilterHelper.resolveDepartmentId(uploadDepartmentCombo.getValue()));
            sel.setSemesterId(AcademicFilterHelper.resolveSemesterId(uploadSemesterCombo.getValue()));
            sel.setCategory(uploadCategoryCombo.getValue());
            sel.setStatus(uploadStatusCombo.getValue());
            sel.setActive("Approved".equalsIgnoreCase(uploadStatusCombo.getValue()));
            if (selectedUploadFile != null) {
                FileStorageService.getInstance().deleteFile(sel.getFilePath());
                sel.setFilePath(FileStorageService.getInstance()
                        .storeFile(selectedUploadFile, "resources"));
            }
            resourceService.updateResource(sel);
            info("Resource updated.");
            resetUploadForm();
            loadData();
        } catch (Exception e) {
            warn("Update failed: " + e.getMessage());
        }
    }

    @FXML public void handleResetUploadForm() { resetUploadForm(); }

    private void resetUploadForm() {
        if (uploadNameField != null) uploadNameField.clear();
        if (uploadDescriptionField != null) uploadDescriptionField.clear();
        if (uploadFileField != null) uploadFileField.clear();
        if (uploadDepartmentCombo != null) uploadDepartmentCombo.setValue(AcademicFilterHelper.allDepartments());
        if (uploadSemesterCombo != null) {
            uploadSemesterCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
            uploadSemesterCombo.setValue(AcademicFilterHelper.allSemesters());
            uploadSemesterCombo.setDisable(false);
        }
        if (uploadSubjectCombo != null) {
            uploadSubjectCombo.getSelectionModel().clearSelection();
            AcademicFilterHelper.loadSubjects(academicService,
                    uploadDepartmentCombo != null ? uploadDepartmentCombo.getValue() : null,
                    uploadSemesterCombo != null ? uploadSemesterCombo.getValue() : null,
                    uploadSubjectCombo);
        }
        if (uploadStatusCombo != null) uploadStatusCombo.setValue("Approved");
        selectedUploadFile = null;
    }

    @FXML public void handleEditResource() {
        Resource r = selected(); if (r == null) return;
        uploadNameField.setText(r.getTitle());
        uploadDescriptionField.setText(r.getDescription());
        uploadFileField.setText(r.getFilePath() != null ? new File(r.getFilePath()).getName() : "");
        uploadCategoryCombo.setValue(r.getCategory());
        uploadStatusCombo.setValue(r.getStatus() != null ? r.getStatus() : (r.isActive() ? "Approved" : "Pending"));
        populateUploadCombosFromResource(r);
    }

    private void populateUploadCombosFromResource(Resource r) {
        if (uploadDepartmentCombo == null) return;
        Integer deptId = r.getDepartmentId();
        if (deptId != null && deptId > 0) {
            for (Department d : academicService.getAllActiveDepartments()) {
                if (d.getId() == deptId) {
                    uploadDepartmentCombo.setValue(d);
                    uploadSemesterCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, d));
                    uploadSemesterCombo.setDisable(false);
                    break;
                }
            }
        } else {
            uploadDepartmentCombo.setValue(AcademicFilterHelper.allDepartments());
            uploadSemesterCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
            uploadSemesterCombo.setDisable(false);
        }
        Integer semId = r.getSemesterId();
        if (semId != null && semId > 0) {
            for (Semester s : uploadSemesterCombo.getItems()) {
                if (s.getId() == semId) {
                    uploadSemesterCombo.setValue(s);
                    AcademicFilterHelper.loadSubjects(academicService,
                            uploadDepartmentCombo.getValue(), s,
                            uploadSubjectCombo);
                    break;
                }
            }
        } else if (uploadSemesterCombo != null) {
            uploadSemesterCombo.setValue(AcademicFilterHelper.allSemesters());
            AcademicFilterHelper.loadSubjects(academicService,
                    uploadDepartmentCombo.getValue(), uploadSemesterCombo.getValue(),
                    uploadSubjectCombo);
        }
        if (r.getSubjectId() > 0 && uploadSubjectCombo != null) {
            for (Subject s : uploadSubjectCombo.getItems()) {
                if (s.getId() == r.getSubjectId()) { uploadSubjectCombo.setValue(s); break; }
            }
        }
    }

    @FXML public void handleApproveResource() {
        Resource r = selected(); if (r == null) return;
        adminService.updateResourceStatus(r.getId(), "Approved", r.getTitle());
        loadData();
    }

    @FXML public void handleHideResource() {
        Resource r = selected(); if (r == null) return;
        adminService.updateResourceStatus(r.getId(), "Hidden", r.getTitle());
        loadData();
    }

    private void loadData() {
        masterList.setAll(adminService.getResources());
        filteredList = new ArrayList<>(masterList);
        currentPage = 1;
        updateTable();
    }

    @FXML
    public void handleRefresh() {
        loadData();
    }
    // ── Filtering ─────────────────────────────────────────────────────────────

    @FXML
    public void applyFilters() {
        String q       = searchField.getText().trim().toLowerCase();
        Department dept = departmentFilter.getValue();
        Semester sem = semesterFilter.getValue();
        Subject selectedSubject = subjectFilter.getValue();
        String subject = selectedSubject != null ? selectedSubject.getName() : null;
        String status  = statusFilter.getValue();
        List<Subject> allSubjects = academicService.getAllActiveSubjects();
        Map<Integer, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (a, b) -> a));

        filteredList = masterList.stream()
            .filter(r -> q.isEmpty()
                    || nullSafe(r.getTitle()).toLowerCase().contains(q)
                    || nullSafe(r.getSubject()).toLowerCase().contains(q)
                    || nullSafe(r.getSource()).toLowerCase().contains(q))
            .filter(r -> subject == null || subject.isEmpty() || nullSafe(r.getSubject()).equalsIgnoreCase(subject))
            .filter(r -> AcademicFilterHelper.matchesDeptSemFilter(
                    r.getDepartmentId(), r.getSemesterId(), r.getSubjectId(),
                    dept, sem, subjectMap, allSubjects, nullSafe(r.getSubject())))
            .filter(r -> {
                if (status == null || status.isEmpty()) return true;
                if ("Active".equalsIgnoreCase(status)) return r.isActive();
                if ("Inactive".equalsIgnoreCase(status)) return !r.isActive();
                return status.equalsIgnoreCase(nullSafe(r.getStatus()));
            })
            .collect(Collectors.toList());

        currentPage = 1;
        updateTable();
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        AcademicFilterHelper.resetFilters(academicService, departmentFilter, semesterFilter, subjectFilter);
        if (statusFilter != null) statusFilter.getSelectionModel().clearSelection();
        filteredList = new ArrayList<>(masterList);
        currentPage  = 1;
        updateTable();
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @FXML public void handlePrevPage() { if (currentPage > 1) { currentPage--; updateTable(); } }
    @FXML public void handleNextPage() { if (currentPage < maxPages()) { currentPage++; updateTable(); } }

    private void updateTable() {
        int total    = filteredList.size();
        int maxPages = maxPages();
        currentPage  = Math.max(1, Math.min(currentPage, maxPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        resourcesTable.setItems(FXCollections.observableArrayList(
                from < total ? filteredList.subList(from, to) : List.of()));

        if (lblPageNumber != null) lblPageNumber.setText("Page " + currentPage + " of " + maxPages);
        if (lblTotalCount != null) lblTotalCount.setText(total + " resource" + (total != 1 ? "s" : ""));
        if (btnPrevPage   != null) btnPrevPage.setDisable(currentPage == 1);
        if (btnNextPage   != null) btnNextPage.setDisable(currentPage == maxPages);
    }

    private int maxPages() { return Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE)); }

    // ── Resource Actions ──────────────────────────────────────────────────────

    @FXML
    public void handleActivate() {
        Resource r = selected(); if (r == null) return;
        boolean ok = adminService.activateResource(r.getId(), r.getTitle());
        if (ok) { 
            r.setActive(true); 
            resourcesTable.refresh(); 
            info("Resource activated."); 
            
            // Log activity
            if (SessionManager.getCurrentAdmin() != null) {
                try {
                    UserActivity activity = new UserActivity(
                        SessionManager.getCurrentAdmin().getId(),
                        SessionManager.getCurrentAdmin().getFullName() != null ? SessionManager.getCurrentAdmin().getFullName() : SessionManager.getCurrentAdmin().getName(),
                        "Approve Resource",
                        "Resource",
                        r.getTitle()
                    );
                    new UserActivityDAO().logActivity(activity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            // Publish EventBus events
            EventBus.getInstance().publish(new EventBus.ResourcesChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
        }
    }

    @FXML
    public void handleDeactivate() {
        Resource r = selected(); if (r == null) return;
        boolean ok = adminService.deactivateResource(r.getId(), r.getTitle());
        if (ok) { 
            r.setActive(false); 
            resourcesTable.refresh(); 
            
            // Log activity
            if (SessionManager.getCurrentAdmin() != null) {
                try {
                    UserActivity activity = new UserActivity(
                        SessionManager.getCurrentAdmin().getId(),
                        SessionManager.getCurrentAdmin().getFullName() != null ? SessionManager.getCurrentAdmin().getFullName() : SessionManager.getCurrentAdmin().getName(),
                        "Reject Resource",
                        "Resource",
                        r.getTitle()
                    );
                    new UserActivityDAO().logActivity(activity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            // Publish EventBus events
            EventBus.getInstance().publish(new EventBus.ResourcesChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
        }
    }

    @FXML
    public void handleDelete() {
        Resource r = selected(); if (r == null) return;
        if (confirm("Delete resource '" + r.getTitle() + "'? This cannot be undone.")) {
            boolean ok = adminService.deleteResourceWithFile(r.getId(), r.getTitle());
            if (ok) { loadData(); EventBus.getInstance().publish(new EventBus.ResourcesChangedEvent()); }
        }
    }

    /** Open the resource file in the system default application. */
    @FXML
    public void handlePreview() {
        Resource r = selected(); if (r == null) return;
        if (r.getFilePath() == null || r.getFilePath().isBlank()) {
            warn("No file path available for this resource.");
            return;
        }
        try {
            File file = new File(r.getFilePath());
            if (!file.exists()) { warn("File not found:\n" + r.getFilePath()); return; }
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file);
        } catch (IOException e) {
            warn("Could not open file: " + e.getMessage());
        }
    }

    /** Copy/download the resource file using a save dialog. */
    @FXML
    public void handleDownload() {
        Resource r = selected(); if (r == null) return;
        if (r.getFilePath() == null || r.getFilePath().isBlank()) {
            warn("No file path available."); return;
        }
        File source = new File(r.getFilePath());
        if (!source.exists()) { warn("Source file not found."); return; }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Resource As");
        chooser.setInitialFileName(source.getName());
        File dest = chooser.showSaveDialog(resourcesTable.getScene().getWindow());
        if (dest == null) return;
        try {
            java.nio.file.Files.copy(source.toPath(), dest.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            info("Saved to: " + dest.getAbsolutePath());
        } catch (IOException e) {
            warn("Download failed: " + e.getMessage());
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupColumns() {
        if (colId        != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colTitle     != null) colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (colSubject   != null) colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (colUploader != null) { colUploader.setCellValueFactory(new PropertyValueFactory<>("uploaderName"));}
        if (colDownloads != null) colDownloads.setCellValueFactory(new PropertyValueFactory<>("downloads"));
        if (colDate      != null) colDate.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
        if (colStatus    != null) colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isActive() ? "✅ Active" : "⏸ Inactive"));

        // Allow sorting by downloads
        if (colDownloads != null) colDownloads.setSortable(true);
    }

    private void setupFilters() {
        AcademicFilterHelper.setupFilterBar(academicService, departmentFilter, semesterFilter, subjectFilter);
        if (statusFilter != null) statusFilter.setItems(FXCollections.observableArrayList(
                "", "Approved", "Pending", "Hidden", "Active", "Inactive"));
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private Resource selected() {
        Resource r = resourcesTable.getSelectionModel().getSelectedItem();
        if (r == null) { warn("Please select a resource first."); }
        return r;
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void info(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void warn(String msg) { Alert a = new Alert(Alert.AlertType.WARNING, msg); a.setHeaderText(null); a.showAndWait(); }
    private String nullSafe(String s) { return s != null ? s : ""; }
}
