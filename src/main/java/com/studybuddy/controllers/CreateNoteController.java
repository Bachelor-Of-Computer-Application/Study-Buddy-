package com.studybuddy.controllers;


import com.studybuddy.models.Note;
import com.studybuddy.services.NoteService;
import com.studybuddy.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CreateNoteController {

    @FXML private TextField titleField;
    @FXML private ComboBox<String> subjectField;
    @FXML private ComboBox<String> sourceField;
    @FXML private DatePicker dateField;
    @FXML private TextArea descriptionField;
    @FXML private TextField fileTextField;
    @FXML private RadioButton privateRadio;
    @FXML private RadioButton shareRadio;
    @FXML private Button uploadResourceBtn;

    private final NoteService noteService = new NoteService();
    private File selectedFile;

    @FXML
    public void initialize() {
        // Initialize fields
        subjectField.setItems(FXCollections.observableArrayList(
                "Mathematics", "Physics", "Chemistry", "Biology",
                "Computer Science", "English", "History"
        ));

        sourceField.setItems(FXCollections.observableArrayList(
                "Textbook", "Lecture", "Online Course", "Research Paper",
                "Notes", "Video Tutorial", "Other"
        ));

        dateField.setValue(LocalDate.now());

        // Group radio buttons
        ToggleGroup visibilityGroup = new ToggleGroup();
        privateRadio.setToggleGroup(visibilityGroup);
        shareRadio.setToggleGroup(visibilityGroup);

        visibilityGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == shareRadio) {
                if (uploadResourceBtn != null) uploadResourceBtn.setVisible(true);
            } else {
                if (uploadResourceBtn != null) uploadResourceBtn.setVisible(false);
            }
        });
    }

    @FXML
    public void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Note File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Document Files", "*.pdf", "*.doc", "*.docx", "*.txt")
        );
        File file = fileChooser.showOpenDialog(titleField.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            fileTextField.setText(file.getName());
        }
    }

    @FXML
    public void saveNote() {

        if (!validateInputs()) {
            return;
        }

        try {

            Note note = new Note();

            note.setId(0);
            note.setTitle(titleField.getText().trim());
            note.setSubject(subjectField.getValue());
            note.setSource(sourceField.getValue());

            note.setUploadDate(
                    dateField.getValue().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );

            note.setDescription(descriptionField.getText().trim());
            note.setPrivate(privateRadio.isSelected());

            if (selectedFile != null) {
                note.setFileName(selectedFile.getName());
                note.setFilePath(selectedFile.getAbsolutePath());
                String ext = selectedFile.getName()
                        .substring(selectedFile.getName().lastIndexOf('.') + 1);

                note.setFileType(ext.toUpperCase());
            } else {
                note.setFileName("No File");
                note.setFileType("TXT");
            }

            note.setUserId(
                    SessionManager.getInstance().getCurrentUser() != null
                            ? SessionManager.getInstance().getCurrentUser().getId()
                            : 1
            );

            noteService.createNote(note);

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Note saved successfully!"
            );

            closeDialog();

        } catch (Exception e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Failed to save note: " + e.getMessage()
            );
        }
    }
    @FXML
    public void uploadResource() {
        // Save note and mark for admin moderation
        saveNote();
    }

    @FXML
    public void cancel() {
        closeDialog();
    }

    private boolean validateInputs() {
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Title is required.");
            return false;
        }
        if (subjectField.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Subject is required.");
            return false;
        }
        if (sourceField.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Source is required.");
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
