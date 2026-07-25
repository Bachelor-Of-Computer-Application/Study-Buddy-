package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Department;
import com.studybuddy.models.Note;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.models.UserActivity;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.FileStorageService;
import com.studybuddy.services.NoteService;
import com.studybuddy.utils.AcademicFilterHelper;
import com.studybuddy.utils.EventBus;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class CreateNoteController {

    @FXML private TextField titleField;
    @FXML private ComboBox<Department> departmentComboBox;
    @FXML private ComboBox<Semester> semesterComboBox;
    @FXML private ComboBox<Subject> subjectComboBox;
    @FXML private ComboBox<String> sourceField;
    @FXML private DatePicker dateField;
    @FXML private TextArea descriptionField;
    @FXML private TextField fileTextField;
    @FXML private RadioButton privateRadio;
    @FXML private RadioButton shareRadio;
    @FXML private VBox uploadResourceBtn;

    private final NoteService noteService = new NoteService();
    private final AcademicService academicService = AcademicService.getInstance();
    private final com.studybuddy.dao.UserActivityDAO activityDAO = new com.studybuddy.dao.UserActivityDAO();
    private File selectedFile;

    @FXML
    public void initialize() {
        sourceField.setItems(FXCollections.observableArrayList(
                "Textbook", "Lecture", "Online Course", "Research Paper",
                "Notes", "Video Tutorial", "Other"
        ));
        dateField.setValue(LocalDate.now());

        departmentComboBox.setItems(AcademicFilterHelper.departmentsForFilter(academicService));
        departmentComboBox.setValue(AcademicFilterHelper.allDepartments());
        semesterComboBox.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
        semesterComboBox.setValue(AcademicFilterHelper.allSemesters());
        semesterComboBox.setDisable(false);

        AcademicFilterHelper.wireCascade(academicService, departmentComboBox, semesterComboBox, subjectComboBox,
                () -> AcademicFilterHelper.loadSubjects(academicService, departmentComboBox.getValue(), semesterComboBox.getValue(), subjectComboBox));
        AcademicFilterHelper.loadSubjects(academicService, departmentComboBox.getValue(), semesterComboBox.getValue(), subjectComboBox);

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
        if (uploadResourceBtn != null) {
            uploadResourceBtn.setVisible(false);
            uploadResourceBtn.setManaged(false);
        }
    }

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

    @FXML
    public void saveNote() {
        if (!validateInputs()) return;
        if (App.getCurrentUser() == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please log in to upload notes.");
            return;
        }

        try {
            Note note = new Note();
            note.setTitle(titleField.getText().trim());

            Department dept = departmentComboBox.getValue();
            Semester sem = semesterComboBox.getValue();
            Subject selectedSubject = subjectComboBox.getValue();

            note.setDepartmentId(AcademicFilterHelper.resolveDepartmentId(dept));
            note.setSemesterId(AcademicFilterHelper.resolveSemesterId(sem));

            if (selectedSubject != null) {
                note.setSubject(selectedSubject.getName());
                note.setSubjectId(selectedSubject.getId());
            } else if (!AcademicFilterHelper.isAllSemesters(sem)) {
                note.setSubject("General");
            } else {
                note.setSubject("All Departments");
            }

            note.setSource(sourceField.getValue());
            note.setUploadDate(dateField.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            note.setDescription(descriptionField.getText() != null ? descriptionField.getText().trim() : "");
            note.setPrivate(privateRadio.isSelected());

            if (selectedFile != null) {
                FileStorageService storage = FileStorageService.getInstance();
                storage.validateFile(selectedFile);
                note.setFilePath(storage.storeFile(selectedFile, "notes"));
                note.setFileName(selectedFile.getName());
                String ext = selectedFile.getName().contains(".")
                        ? selectedFile.getName().substring(selectedFile.getName().lastIndexOf('.') + 1).toUpperCase()
                        : "TXT";
                note.setFileType(ext);
            } else {
                note.setFileName("No File");
                note.setFileType("TXT");
            }

            note.setUserId(App.getCurrentUser().getId());
            boolean isAdmin = "admin".equalsIgnoreCase(App.getCurrentUser().getRole());
            noteService.createNote(note, isAdmin);

            if (App.getCurrentUser() != null) {
                UserActivity activity = new UserActivity(
                    App.getCurrentUser().getId(),
                    App.getCurrentUser().getDisplayFullName(),
                    "UPLOAD_NOTE", "NOTE", note.getTitle()
                );
                try {
                    activityDAO.logActivity(activity);
                } catch (SQLException e) {
                    java.util.logging.Logger.getLogger(CreateNoteController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
                }
            }

            EventBus.getInstance().publish(new EventBus.NotesChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
            showAlert(Alert.AlertType.INFORMATION, "Success", "Note saved successfully!");
            closeDialog();
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(CreateNoteController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save note: " + e.getMessage());
        }
    }

    @FXML
    public void uploadResource() {
        saveNote();
    }

    @FXML
    public void cancel() {
        closeDialog();
    }

    private boolean validateInputs() {
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Note Title is required.");
            return false;
        }
        if (departmentComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a Department (or All Departments).");
            return false;
        }
        if (semesterComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a Semester (or All Semesters).");
            return false;
        }
        if (!AcademicFilterHelper.isAllSemesters(semesterComboBox.getValue()) && subjectComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a Subject when a specific semester is chosen.");
            return false;
        }
        if (sourceField.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a Source.");
            return false;
        }
        return true;
    }

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
