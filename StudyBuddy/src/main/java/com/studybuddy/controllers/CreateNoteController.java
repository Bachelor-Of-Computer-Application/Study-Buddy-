package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Department;
import com.studybuddy.models.Note;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.models.UserActivity;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.NoteService;
import com.studybuddy.utils.EventBus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for CreateNoteDialog.fxml.
 *
 * fx:id → field mapping (all injections verified against FXML):
 *   titleField          → TextField
 *   departmentComboBox  → ComboBox<Department>   (cascades → semester)
 *   semesterComboBox    → ComboBox<Semester>     (cascades → subject)
 *   subjectComboBox     → ComboBox<Subject>
 *   sourceField         → ComboBox<String>
 *   dateField           → DatePicker
 *   descriptionField    → TextArea
 *   fileTextField       → TextField
 *   privateRadio        → RadioButton
 *   shareRadio          → RadioButton
 *   uploadResourceBtn   → VBox  (wrapper shown/hidden when "Share" is selected)
 */
public class CreateNoteController {

    // ── Injected from FXML ─────────────────────────────────────────────────────

    @FXML private TextField        titleField;

    /** Cascading top-level: selecting a department repopulates semesterComboBox. */
    @FXML private ComboBox<Department> departmentComboBox;

    /** Cascading mid-level: selecting a semester repopulates subjectComboBox. */
    @FXML private ComboBox<Semester> semesterComboBox;

    /** Cascading leaf: populated after a semester is chosen. */
    @FXML private ComboBox<Subject> subjectComboBox;

    @FXML private ComboBox<String> sourceField;

    @FXML private DatePicker       dateField;
    @FXML private TextArea         descriptionField;
    @FXML private TextField        fileTextField;

    @FXML private RadioButton      privateRadio;
    @FXML private RadioButton      shareRadio;

    /**
     * FXML declares this as a VBox that wraps the "Upload Resource" button.
     * The controller shows/hides the whole wrapper rather than the inner button.
     */
    @FXML private VBox             uploadResourceBtn;

    // ── Services ───────────────────────────────────────────────────────────────

    private final NoteService noteService = new NoteService();
    private final AcademicService academicService = AcademicService.getInstance();
    private final com.studybuddy.dao.UserActivityDAO activityDAO = new com.studybuddy.dao.UserActivityDAO();

    // ── State ──────────────────────────────────────────────────────────────────

    private File selectedFile;

    // ── Initialise ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {

        // ── Source options (static) ────────────────────────────────────────────
        sourceField.setItems(FXCollections.observableArrayList(
                "Textbook", "Lecture", "Online Course", "Research Paper",
                "Notes", "Video Tutorial", "Other"
        ));

        // ── Date defaults to today ─────────────────────────────────────────────
        dateField.setValue(LocalDate.now());

        // ── Disable by default ──────────────────────────────────────────────────
        if (semesterComboBox != null) {
            semesterComboBox.setDisable(true);
        }
        if (subjectComboBox != null) {
            subjectComboBox.setDisable(true);
        }

        // ── Load Departments from SQL Server ───────────────────────────────────
        loadDepartments();

        // ── Cascading: Department → Semester → Subject ────────────────────────
        departmentComboBox.setOnAction(e -> {
            Department selectedDept = departmentComboBox.getValue();
            semesterComboBox.getItems().clear();
            subjectComboBox.getItems().clear();
            semesterComboBox.setValue(null);
            subjectComboBox.setValue(null);
            semesterComboBox.setDisable(selectedDept == null);
            subjectComboBox.setDisable(true);
            
            if (selectedDept != null) {
                loadSemesters(selectedDept.getId());
            }
        });

        semesterComboBox.setOnAction(e -> {
            Semester selectedSem = semesterComboBox.getValue();
            subjectComboBox.getItems().clear();
            subjectComboBox.setValue(null);
            subjectComboBox.setDisable(selectedSem == null);
            
            if (selectedSem != null) {
                loadSubjects(selectedSem.getId());
            }
        });

        // ── Visibility toggle: show/hide the Upload Resource VBox wrapper ──────
        ToggleGroup visibilityGroup = new ToggleGroup();
        privateRadio.setToggleGroup(visibilityGroup);
        shareRadio.setToggleGroup(visibilityGroup);

        visibilityGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean share = (newVal == shareRadio);
            if (uploadResourceBtn != null) {
                uploadResourceBtn.setVisible(share);
                uploadResourceBtn.setManaged(share);
            }
        });

        // Start hidden
        if (uploadResourceBtn != null) {
            uploadResourceBtn.setVisible(false);
            uploadResourceBtn.setManaged(false);
        }
    }

    // ── Data Loading Methods ───────────────────────────────────────────────────

    private void loadDepartments() {
        try {
            List<Department> departments = academicService.getAllActiveDepartments();
            departmentComboBox.setItems(FXCollections.observableArrayList(departments));
        } catch (Exception e) {
            System.err.println("Error loading departments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSemesters(int departmentId) {
        try {
            List<Semester> semesters = academicService.getSemestersByDepartment(departmentId);
            semesterComboBox.setItems(FXCollections.observableArrayList(semesters));
        } catch (Exception e) {
            System.err.println("Error loading semesters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSubjects(int semesterId) {
        try {
            List<Subject> subjects = academicService.getSubjectsBySemester(semesterId);
            subjectComboBox.setItems(FXCollections.observableArrayList(subjects));
        } catch (Exception e) {
            System.err.println("Error loading subjects: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── File picker ───────────────────────────────────────────────────────────

    @FXML
    public void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Note File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "Document Files", "*.pdf", "*.doc", "*.docx", "*.txt",
                        "*.pptx", "*.xlsx", "*.png", "*.jpg")
        );
        File file = fileChooser.showOpenDialog(titleField.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            fileTextField.setText(file.getName());
        }
    }

    // ── Save Note ─────────────────────────────────────────────────────────────

    @FXML
    public void saveNote() {
        if (!validateInputs()) {
            return;
        }

        try {
            Note note = new Note();

            note.setId(0);
            note.setTitle(titleField.getText().trim());
            
            // Store the subject name for backward compatibility (legacy subject field)
            Subject selectedSubject = subjectComboBox.getValue();
            if (selectedSubject != null) {
                note.setSubject(selectedSubject.getName());
                note.setSubjectId(selectedSubject.getId());
            }
            
            note.setSource(sourceField.getValue());

            note.setUploadDate(
                    dateField.getValue().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );

            note.setDescription(descriptionField.getText() != null
                    ? descriptionField.getText().trim() : "");
            note.setPrivate(privateRadio.isSelected());

            if (selectedFile != null) {
                note.setFileName(selectedFile.getName());
                note.setFilePath(selectedFile.getAbsolutePath());
                String name = selectedFile.getName();
                String ext  = name.contains(".")
                        ? name.substring(name.lastIndexOf('.') + 1)
                        : "TXT";
                note.setFileType(ext.toUpperCase());
            } else {
                note.setFileName("No File");
                note.setFileType("TXT");
            }

            note.setUserId(App.getCurrentUser() != null
                    ? App.getCurrentUser().getId() : 1);

            boolean isAdmin = App.getCurrentUser() != null && "admin".equalsIgnoreCase(App.getCurrentUser().getRole());
            noteService.createNote(note, isAdmin);

            // Log activity
            if (App.getCurrentUser() != null) {
                UserActivity activity = new UserActivity(
                    App.getCurrentUser().getId(),
                    App.getCurrentUser().getFullName() != null ? App.getCurrentUser().getFullName() : App.getCurrentUser().getName(),
                    "UPLOAD_NOTE",
                    "NOTE",
                    note.getTitle()
                );
                try {
                    activityDAO.logActivity(activity);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // Publish events
            EventBus.getInstance().publish(new EventBus.NotesChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());

            showAlert(Alert.AlertType.INFORMATION, "Success", "Note saved successfully!");
            closeDialog();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save note: " + e.getMessage());
        }
    }

    // ── Upload Resource (inner button inside uploadResourceBtn VBox) ──────────

    @FXML
    public void uploadResource() {
        // Save the note first, then it will be queued for admin moderation
        saveNote();
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @FXML
    public void cancel() {
        closeDialog();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateInputs() {
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Note Title is required.");
            return false;
        }
        if (departmentComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a Department.");
            return false;
        }
        if (semesterComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a Semester.");
            return false;
        }
        if (subjectComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a Subject.");
            return false;
        }
        if (sourceField.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a Source.");
            return false;
        }
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void closeDialog() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
