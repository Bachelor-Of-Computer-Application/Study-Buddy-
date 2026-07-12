package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.models.Department;
import com.studybuddy.models.Note;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.FileStorageService;
import com.studybuddy.services.NoteService;
import com.studybuddy.utils.AcademicFilterHelper;
import com.studybuddy.utils.EventBus;
import com.studybuddy.utils.SessionManager;
import com.studybuddy.utils.StringUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Notes Management: upload form + moderation table with full CRUD.
 */
public class AdminNotesController {

    // Upload form
    @FXML private TextField uploadTitleField;
    @FXML private TextArea uploadDescriptionField;
    @FXML private ComboBox<Department> uploadDepartmentCombo;
    @FXML private ComboBox<Semester> uploadSemesterCombo;
    @FXML private ComboBox<Subject> uploadSubjectCombo;
    @FXML private TextField uploadTagsField;
    @FXML private TextField uploadFileField;
    @FXML private ComboBox<String> uploadVisibilityCombo;
    @FXML private ComboBox<String> uploadStatusCombo;

    // Table & filters
    @FXML private TextField searchField;
    @FXML private ComboBox<Department> departmentFilter;
    @FXML private ComboBox<Semester> semesterFilter;
    @FXML private ComboBox<Subject> subjectFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> visibilityFilter;

    @FXML private TableView<Note> notesTable;
    @FXML private TableColumn<Note, String> colFileName;
    @FXML private TableColumn<Note, String> colTitle;
    @FXML private TableColumn<Note, String> colSubject;
    @FXML private TableColumn<Note, String> colDepartment;
    @FXML private TableColumn<Note, String> colSemester;
    @FXML private TableColumn<Note, String> colUploader;
    @FXML private TableColumn<Note, String> colDate;
    @FXML private TableColumn<Note, Number> colDownloads;
    @FXML private TableColumn<Note, String> colStatus;

    @FXML private Label lblPageNumber;
    @FXML private Label lblTotalCount;
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatPending;
    @FXML private Label lblStatApproved;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    private final AdminService adminService = AdminService.getInstance();
    private final NoteService noteService = new NoteService();
    private final AcademicService academicService = AcademicService.getInstance();
    private final FileStorageService fileStorage = FileStorageService.getInstance();
    private final ObservableList<Note> masterList = FXCollections.observableArrayList();
    private List<Note> filteredList = new ArrayList<>();
    private File selectedFile;
    private Note editingNote;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    @FXML
    public void initialize() {
        setupUploadForm();
        setupColumns();
        setupFilters();
        loadData();
        if (searchField != null) {
            searchField.textProperty().addListener((o, a, b) -> applyFilters());
        }
    }

    private void setupUploadForm() {
        if (uploadVisibilityCombo != null) {
            uploadVisibilityCombo.setItems(FXCollections.observableArrayList("Public", "Private"));
            uploadVisibilityCombo.setValue("Public");
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
            uploadSemesterCombo.setDisable(true);
        }
        if (uploadSubjectCombo != null) uploadSubjectCombo.setDisable(true);

        AcademicFilterHelper.wireCascade(academicService, uploadDepartmentCombo, uploadSemesterCombo, uploadSubjectCombo,
                () -> AcademicFilterHelper.loadSubjectsForSemester(academicService, uploadSemesterCombo.getValue(), uploadSubjectCombo));
        if (uploadSemesterCombo != null) {
            uploadSemesterCombo.setOnAction(e -> AcademicFilterHelper.loadSubjectsForSemester(
                    academicService, uploadSemesterCombo.getValue(), uploadSubjectCombo));
        }
    }

    @FXML
    public void handleSelectFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Note File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documents",
                "*.pdf", "*.doc", "*.docx", "*.txt", "*.pptx", "*.xlsx", "*.png", "*.jpg"));
        File f = fc.showOpenDialog(notesTable.getScene().getWindow());
        if (f != null) {
            selectedFile = f;
            uploadFileField.setText(f.getName());
        }
    }

    @FXML
    public void handleUpload() {
        if (!validateUploadForm()) return;
        try {
            Note note = buildNoteFromForm();
            note.setUserId(SessionManager.getCurrentAdmin() != null
                    ? SessionManager.getCurrentAdmin().getId() : 1);
            note.setSource(SessionManager.getCurrentAdmin() != null
                    ? SessionManager.getCurrentAdmin().getFullName() : "Admin");
            note.setUploadDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            note.setPrivate("Private".equalsIgnoreCase(uploadVisibilityCombo.getValue()));
            note.setStatus(uploadStatusCombo.getValue());

            if (selectedFile != null) {
                fileStorage.validateFile(selectedFile);
                String path = fileStorage.storeFile(selectedFile, "notes");
                note.setFilePath(path);
                note.setFileName(selectedFile.getName());
                note.setFileType(extension(selectedFile.getName()));
            }

            boolean autoApprove = "Approved".equalsIgnoreCase(note.getStatus());
            noteService.createNote(note, autoApprove);
            info("Note uploaded successfully.");
            resetUploadForm();
            loadData();
            EventBus.getInstance().publish(new EventBus.NotesChangedEvent());
        } catch (Exception e) {
            error("Upload failed: " + e.getMessage());
        }
    }

    @FXML
    public void handleUpdate() {
        Note selected = notesTable.getSelectionModel().getSelectedItem();
        if (selected == null) { warn("Select a note to update."); return; }
        if (!validateUploadForm()) return;
        try {
            Note note = buildNoteFromForm();
            note.setId(selected.getId());
            note.setUserId(selected.getUserId());
            note.setPrivate("Private".equalsIgnoreCase(uploadVisibilityCombo.getValue()));
            note.setStatus(uploadStatusCombo.getValue());
            if (selectedFile != null) {
                fileStorage.validateFile(selectedFile);
                fileStorage.deleteFile(selected.getFilePath());
                note.setFilePath(fileStorage.storeFile(selectedFile, "notes"));
                note.setFileName(selectedFile.getName());
                note.setFileType(extension(selectedFile.getName()));
            } else {
                note.setFilePath(selected.getFilePath());
                note.setFileName(selected.getFileName());
                note.setFileType(selected.getFileType());
            }
            noteService.updateNote(note);
            info("Note updated successfully.");
            resetUploadForm();
            loadData();
        } catch (Exception e) {
            error("Update failed: " + e.getMessage());
        }
    }

    @FXML
    public void handleResetForm() {
        resetUploadForm();
    }

    private void resetUploadForm() {
        if (uploadTitleField != null) uploadTitleField.clear();
        if (uploadDescriptionField != null) uploadDescriptionField.clear();
        if (uploadTagsField != null) uploadTagsField.clear();
        if (uploadFileField != null) uploadFileField.clear();
        if (uploadDepartmentCombo != null) {
            uploadDepartmentCombo.setValue(AcademicFilterHelper.allDepartments());
        }
        if (uploadSemesterCombo != null) {
            uploadSemesterCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
            uploadSemesterCombo.setValue(AcademicFilterHelper.allSemesters());
            uploadSemesterCombo.setDisable(true);
        }
        if (uploadSubjectCombo != null) { uploadSubjectCombo.getItems().clear(); uploadSubjectCombo.setValue(null); uploadSubjectCombo.setDisable(true); }
        if (uploadVisibilityCombo != null) uploadVisibilityCombo.setValue("Public");
        if (uploadStatusCombo != null) uploadStatusCombo.setValue("Approved");
        selectedFile = null;
        editingNote = null;
    }

    private Note buildNoteFromForm() {
        Note note = new Note();
        note.setTitle(uploadTitleField.getText().trim());
        note.setDescription(uploadDescriptionField.getText() != null ? uploadDescriptionField.getText().trim() : "");
        note.setTags(uploadTagsField.getText() != null ? uploadTagsField.getText().trim() : "");
        note.setDepartmentId(AcademicFilterHelper.resolveDepartmentId(uploadDepartmentCombo.getValue()));
        note.setSemesterId(AcademicFilterHelper.resolveSemesterId(uploadSemesterCombo.getValue()));
        Subject sub = uploadSubjectCombo.getValue();
        if (sub != null) {
            note.setSubject(sub.getName());
            note.setSubjectId(sub.getId());
        } else if (!AcademicFilterHelper.isAllSemesters(uploadSemesterCombo.getValue())) {
            note.setSubject("General");
        } else {
            note.setSubject("All Departments");
        }
        return note;
    }

    private boolean validateUploadForm() {
        if (uploadTitleField.getText() == null || uploadTitleField.getText().trim().isEmpty()) {
            warn("Title is required."); return false;
        }
        if (uploadDepartmentCombo.getValue() == null) { warn("Select a department or All Departments."); return false; }
        if (uploadSemesterCombo.getValue() == null) { warn("Select a semester or All Semesters."); return false; }
        if (!AcademicFilterHelper.isAllSemesters(uploadSemesterCombo.getValue()) && uploadSubjectCombo.getValue() == null) {
            warn("Select a subject when a specific semester is chosen."); return false;
        }
        if (editingNote == null && selectedFile == null) { warn("Select a file to upload."); return false; }
        return true;
    }

    private void loadData() {
        masterList.setAll(adminService.getNotes());
        filteredList = new ArrayList<>(masterList);
        currentPage = 1;
        updateStats();
        updateTable();
    }

    private void updateStats() {
        long total = masterList.size();
        long pending = masterList.stream().filter(n -> "Pending".equalsIgnoreCase(n.getStatus())).count();
        long approved = masterList.stream().filter(n -> "Approved".equalsIgnoreCase(n.getStatus())).count();
        if (lblStatTotal != null) lblStatTotal.setText(String.valueOf(total));
        if (lblStatPending != null) lblStatPending.setText(String.valueOf(pending));
        if (lblStatApproved != null) lblStatApproved.setText(String.valueOf(approved));
    }

    @FXML public void handleRefresh() { loadData(); }

    @FXML
    public void applyFilters() {
        String q = searchField.getText().trim().toLowerCase();
        Department dept = departmentFilter.getValue();
        Semester sem = semesterFilter.getValue();
        Subject selectedSubject = subjectFilter != null ? subjectFilter.getValue() : null;
        String subjectName = selectedSubject != null ? selectedSubject.getName() : null;
        String status = statusFilter.getValue();
        String visibility = visibilityFilter != null ? visibilityFilter.getValue() : null;

        filteredList = masterList.stream()
            .filter(n -> q.isEmpty() || contains(q, n.getTitle(), n.getSubject(), n.getFileName(), n.getSource()))
            .filter(n -> subjectName == null || subjectName.isEmpty() || StringUtils.nullSafe(n.getSubject()).equalsIgnoreCase(subjectName))
            .filter(n -> status == null || status.isEmpty() || StringUtils.nullSafe(n.getStatus()).equalsIgnoreCase(status))
            .filter(n -> {
                List<Subject> allSubjects = academicService.getAllActiveSubjects();
                Map<Integer, Subject> subjectMap = allSubjects.stream()
                        .collect(Collectors.toMap(Subject::getId, s -> s, (a, b) -> a));
                return AcademicFilterHelper.matchesDeptSemFilter(
                        n.getDepartmentId(), n.getSemesterId(), n.getSubjectId(),
                        dept, sem, subjectMap, allSubjects, StringUtils.nullSafe(n.getSubject()));
            })
            .filter(n -> {
                if (visibility == null || visibility.isEmpty()) return true;
                if ("Public".equalsIgnoreCase(visibility)) return !n.isPrivate();
                if ("Private".equalsIgnoreCase(visibility)) return n.isPrivate();
                return true;
            })
            .collect(Collectors.toList());
        currentPage = 1;
        updateTable();
    }

    @FXML public void clearFilters() {
        searchField.clear();
        AcademicFilterHelper.resetFilters(academicService, departmentFilter, semesterFilter, subjectFilter);
        if (statusFilter != null) statusFilter.getSelectionModel().clearSelection();
        if (visibilityFilter != null) visibilityFilter.getSelectionModel().clearSelection();
        filteredList = new ArrayList<>(masterList);
        currentPage = 1;
        updateTable();
    }

    @FXML public void handlePrevPage() { if (currentPage > 1) { currentPage--; updateTable(); } }
    @FXML public void handleNextPage() { if (currentPage < maxPages()) { currentPage++; updateTable(); } }

    private void updateTable() {
        int total = filteredList.size();
        int maxPages = maxPages();
        currentPage = Math.max(1, Math.min(currentPage, maxPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        notesTable.setItems(FXCollections.observableArrayList(
                from < total ? filteredList.subList(from, Math.min(from + PAGE_SIZE, total)) : List.of()));
        if (lblPageNumber != null) lblPageNumber.setText("Page " + currentPage + " of " + maxPages);
        if (lblTotalCount != null) lblTotalCount.setText(total + " note(s)");
        if (btnPrevPage != null) btnPrevPage.setDisable(currentPage == 1);
        if (btnNextPage != null) btnNextPage.setDisable(currentPage >= maxPages);
    }

    private int maxPages() { return Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE)); }

    @FXML public void handleView() { openSelectedFile(false); }
    @FXML public void handleDownload() {
        Note n = selected(); if (n == null) return;
        openSelectedFile(true);
        try { noteService.incrementDownloads(n.getId()); loadData(); } catch (Exception ignored) {}
    }

    @FXML public void handleEdit() {
        Note n = selected(); if (n == null) return;
        editingNote = n;
        uploadTitleField.setText(n.getTitle());
        uploadDescriptionField.setText(n.getDescription());
        uploadTagsField.setText(n.getTags());
        uploadFileField.setText(n.getFileName());
        uploadVisibilityCombo.setValue(n.isPrivate() ? "Private" : "Public");
        uploadStatusCombo.setValue(n.getStatus());
        populateUploadCombosFromNote(n);
    }

    private void populateUploadCombosFromNote(Note n) {
        if (uploadDepartmentCombo == null) return;
        Integer deptId = n.getDepartmentId();
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
            uploadSemesterCombo.setDisable(true);
        }
        Integer semId = n.getSemesterId();
        if (semId != null && semId > 0) {
            for (Semester s : uploadSemesterCombo.getItems()) {
                if (s.getId() == semId) {
                    uploadSemesterCombo.setValue(s);
                    AcademicFilterHelper.loadSubjectsForSemester(academicService, s, uploadSubjectCombo);
                    break;
                }
            }
        } else {
            uploadSemesterCombo.setValue(AcademicFilterHelper.allSemesters());
        }
        if (n.getSubjectId() > 0 && uploadSubjectCombo != null) {
            for (Subject s : uploadSubjectCombo.getItems()) {
                if (s.getId() == n.getSubjectId()) {
                    uploadSubjectCombo.setValue(s);
                    break;
                }
            }
        }
    }

    @FXML public void handleApprove() { setStatus("Approved"); }
    @FXML public void handleReject() { setStatus("Rejected"); }
    @FXML public void handleHide() { setStatus("Hidden"); }

    @FXML public void handleDelete() {
        Note n = selected(); if (n == null) return;
        if (!confirm("Delete note '" + n.getTitle() + "' permanently?\nLinked resources will also be removed.")) return;
        String error = adminService.deleteNoteWithFile(n.getId(), n.getTitle());
        if (error == null) {
            info("Note deleted successfully.");
            loadData();
            EventBus.getInstance().publish(new EventBus.NotesChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
        } else if (error.startsWith("Note was removed from the database")) {
            warn(error);
            loadData();
            EventBus.getInstance().publish(new EventBus.NotesChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
        } else {
            error(error);
        }
    }

    private void setStatus(String status) {
        Note n = selected(); if (n == null) return;
        adminService.updateNoteStatus(n.getId(), status, n.getTitle());
        n.setStatus(status);
        notesTable.refresh();
        updateStats();
    }

    private void setupColumns() {
        notesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        if (colFileName != null) colFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        if (colTitle != null) colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (colSubject != null) colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (colDepartment != null) colDepartment.setCellValueFactory(new PropertyValueFactory<>("userDepartment"));
        if (colSemester != null) colSemester.setCellValueFactory(new PropertyValueFactory<>("userSemester"));
        if (colUploader != null) colUploader.setCellValueFactory(new PropertyValueFactory<>("source"));
        if (colDate != null) colDate.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
        if (colDownloads != null) colDownloads.setCellValueFactory(new PropertyValueFactory<>("downloads"));
        if (colStatus != null) colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        notesTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) handleEdit();
        });
    }

    private void setupFilters() {
        AcademicFilterHelper.setupFilterBar(academicService, departmentFilter, semesterFilter, subjectFilter);
        if (statusFilter != null) statusFilter.setItems(FXCollections.observableArrayList(
                "", "Pending", "Approved", "Rejected", "Hidden"));
        if (visibilityFilter != null) visibilityFilter.setItems(FXCollections.observableArrayList("", "Public", "Private"));
    }

    private void openSelectedFile(boolean download) {
        Note n = selected(); if (n == null) return;
        String path = n.getFilePath();
        if (path == null || path.isBlank()) { warn("No file attached."); return; }
        File f = new File(path);
        if (!f.exists()) { warn("File not found on disk."); return; }
        try {
            if (download) Desktop.getDesktop().open(f);
            else Desktop.getDesktop().open(f);
        } catch (Exception e) {
            error("Cannot open file: " + e.getMessage());
        }
    }

    private Note selected() {
        Note n = notesTable.getSelectionModel().getSelectedItem();
        if (n == null) warn("Please select a note.");
        return n;
    }

    private static boolean contains(String q, String... fields) {
        for (String f : fields) {
            if (f != null && f.toLowerCase().contains(q)) return true;
        }
        return false;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toUpperCase() : "TXT";
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
    private void info(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void warn(String msg) { Alert a = new Alert(Alert.AlertType.WARNING, msg); a.setHeaderText(null); a.showAndWait(); }
    private void error(String msg) { Alert a = new Alert(Alert.AlertType.ERROR, msg); a.setHeaderText(null); a.showAndWait(); }
}
