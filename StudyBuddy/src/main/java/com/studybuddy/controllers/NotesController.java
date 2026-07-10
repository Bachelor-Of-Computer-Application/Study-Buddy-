package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Department;
import com.studybuddy.models.Note;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.models.User;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.AuthorizationService;
import com.studybuddy.services.NoteService;
import com.studybuddy.utils.AcademicFilterHelper;
import com.studybuddy.utils.EventBus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class NotesController implements Initializable {

    @FXML private BorderPane mainBorderPane;
    @FXML private TabPane notesTabPane;
    @FXML private ComboBox<Department> departmentFilter;
    @FXML private ComboBox<Semester> semesterFilter;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ComboBox<String> sourceFilter;
    @FXML private VBox myNotesContainer;
    @FXML private VBox communityNotesContainer;
    @FXML private VBox emptyState;

    private NoteService noteService;
    private AcademicService academicService;
    private AuthorizationService authService;
    private List<Note> allNotes = new ArrayList<>();
    private int currentUserId = -1;
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        noteService = new NoteService();
        academicService = AcademicService.getInstance();
        authService = AuthorizationService.getInstance();

        currentUser = App.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
        } else {
            currentUserId = -1;
        }

        AcademicFilterHelper.setupFilterBar(academicService, departmentFilter, semesterFilter, subjectFilter);
        loadSources();
        loadNotes();

        EventBus.getInstance().subscribe(EventBus.NotesChangedEvent.class, (event) -> loadNotes());
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, (event) -> loadNotes());
    }

    private void loadSources() {
        sourceFilter.setItems(FXCollections.observableArrayList(
            "Textbook", "Lecture", "Online Course", "Research Paper",
            "Notes", "Video Tutorial", "Other"
        ));
    }

    private void loadNotes() {
        if (currentUserId <= 0) {
            allNotes = new ArrayList<>();
            displayNotes(allNotes);
            return;
        }
        try {
            allNotes = noteService.getNotesByUserId(currentUserId);
            if (allNotes == null) {
                allNotes = new ArrayList<>();
            }
            displayNotes(allNotes);
        } catch (Exception e) {
            showError("Failed to load notes: " + e.getMessage());
        }
    }

    private void displayNotes(List<Note> notes) {
        myNotesContainer.getChildren().clear();
        communityNotesContainer.getChildren().clear();

        boolean hasPersonal = false;
        boolean hasCommunity = false;

        for (Note note : notes) {
            if (note.isPrivate()) {
                myNotesContainer.getChildren().add(createPersonalNoteCard(note));
                hasPersonal = true;
            } else {
                communityNotesContainer.getChildren().add(createCommunityNoteCard(note));
                hasCommunity = true;
            }
        }

        emptyState.setVisible(!hasPersonal && !hasCommunity);
        emptyState.setManaged(!hasPersonal && !hasCommunity);
    }

    private VBox createPersonalNoteCard(Note note) {
        VBox card = new VBox(10);
        card.getStyleClass().add("note-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(note.getTitle());
        titleLabel.getStyleClass().add("note-title");
        titleLabel.setWrapText(true);

        Label metaLabel = new Label("📅 " + note.getUploadDate() + " | " + fileTypeIcon(note.getFileType()) + " " + nullSafe(note.getFileType()));
        metaLabel.getStyleClass().add("note-date");

        HBox metaBox = new HBox(15);
        Label subjectLabel = new Label("📚 " + nullSafe(note.getSubject()));
        subjectLabel.getStyleClass().add("note-subject");
        Label sourceLabel = new Label("🔗 " + nullSafe(note.getSource()));
        sourceLabel.getStyleClass().add("note-desc");
        metaBox.getChildren().addAll(subjectLabel, sourceLabel);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button openBtn = new Button("📖 View");
        openBtn.getStyleClass().add("btn-action-view");
        openBtn.setOnAction(e -> openNote(note));

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.getStyleClass().add("btn-action-delete");
        deleteBtn.setOnAction(e -> deleteNote(note));

        buttonBox.getChildren().addAll(openBtn, deleteBtn);
        card.getChildren().addAll(titleLabel, metaLabel, metaBox, buttonBox);
        return card;
    }

    private VBox createCommunityNoteCard(Note note) {
        VBox card = new VBox(10);
        card.getStyleClass().add("note-card");
        card.setMaxWidth(Double.MAX_VALUE);

        String status = note.getStatus() != null ? note.getStatus() : "Pending";
        Label statusBadge = new Label(status);
        statusBadge.getStyleClass().add(switch (status) {
            case "Approved" -> "badge-approved";
            case "Rejected" -> "badge-rejected";
            default -> "badge-pending";
        });

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(note.getTitle());
        titleLabel.getStyleClass().add("note-title");
        titleLabel.setWrapText(true);
        titleRow.getChildren().addAll(titleLabel, statusBadge);

        Label authorLabel = new Label(buildAuthorText(note));
        authorLabel.getStyleClass().add("note-desc");
        authorLabel.setWrapText(true);

        Label metaLabel = new Label("📅 " + note.getUploadDate() + " | " + fileTypeIcon(note.getFileType()) + " " + nullSafe(note.getFileType()));
        metaLabel.getStyleClass().add("note-date");

        HBox metaBox = new HBox(15);
        Label subjectLabel = new Label("📚 " + nullSafe(note.getSubject()));
        subjectLabel.getStyleClass().add("note-subject");
        Label sourceLabel = new Label("🔗 " + nullSafe(note.getSource()));
        sourceLabel.getStyleClass().add("note-desc");
        metaBox.getChildren().addAll(subjectLabel, sourceLabel);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button openBtn = new Button("📖 View");
        openBtn.getStyleClass().add("btn-action-view");
        openBtn.setOnAction(e -> openNote(note));
        buttonBox.getChildren().add(openBtn);

        if (authService.canDeleteNote(currentUser, note)) {
            Button deleteBtn = new Button("🗑 Delete");
            deleteBtn.getStyleClass().add("btn-action-delete");
            deleteBtn.setOnAction(e -> deleteNote(note));
            buttonBox.getChildren().add(deleteBtn);
        }

        card.getChildren().addAll(titleRow, authorLabel, metaLabel, metaBox, buttonBox);
        return card;
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

    private String buildAuthorText(Note note) {
        StringBuilder authorText = new StringBuilder("👤 Uploaded by: ");
        String fullName = note.getUserFullName();
        authorText.append(fullName != null && !fullName.isEmpty() ? fullName : "User " + note.getUserId());
        String dept = note.getUserDepartment();
        String sem = note.getUserSemester();
        if (dept != null || sem != null) {
            authorText.append(" (");
            if (dept != null) authorText.append(dept);
            if (dept != null && sem != null) authorText.append(" • ");
            if (sem != null) authorText.append(sem);
            authorText.append(")");
        }
        return authorText.toString();
    }

    @FXML
    public void showCreateNoteDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/studybuddy/fxml/CreateNoteDialog.fxml"));
            VBox dialogContent = loader.load();

            Stage dialog = new Stage();
            dialog.setTitle("Create New Note");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(dialogContent, 520, 620));
            dialog.setMinWidth(480);
            dialog.setMinHeight(500);
            dialog.showAndWait();

            loadNotes();
        } catch (IOException e) {
            showError("Failed to open create note dialog: " + e.getMessage());
        }
    }

    private void deleteNote(Note note) {
        if (currentUser == null) {
            showError("You must be logged in to delete notes.");
            return;
        }
        if (!authService.canDeleteNote(currentUser, note)) {
            showError("You can only delete your own notes.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Note");
        alert.setHeaderText("Are you sure you want to delete this note?");
        alert.setContentText(note.getTitle());
        alert.getButtonTypes().setAll(new ButtonType("Yes, Delete", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == alert.getButtonTypes().get(0)) {
            try {
                noteService.deleteNoteWithFile(note.getId(), currentUser);
                loadNotes();
                EventBus.getInstance().publish(new EventBus.NotesChangedEvent());
                EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
                showSuccess("Note deleted successfully!");
            } catch (Exception e) {
                showError("Failed to delete note: " + e.getMessage());
            }
        }
    }

    private void openNote(Note note) {
        if (note == null || note.getFilePath() == null || note.getFilePath().isBlank()) {
            showError("No file attached to this note.");
            return;
        }

        File file = new File(note.getFilePath());
        if (!file.exists()) {
            showError("Note file not found.");
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showError("Unable to open file on this system.");
            }
        } catch (IOException e) {
            showError("Failed to open file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void applyFilters() {
        Department dept = departmentFilter.getValue();
        Semester sem = semesterFilter.getValue();
        String subject = subjectFilter.getValue();
        String source = sourceFilter.getValue();

        List<Subject> allSubjects = academicService.getAllActiveSubjects();
        Map<Integer, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (s1, unused) -> s1));

        List<Note> filtered = new ArrayList<>();
        for (Note note : allNotes) {
            String noteSubject = nullSafe(note.getSubject());
            String noteSource = nullSafe(note.getSource());
            if (subject != null && !noteSubject.equalsIgnoreCase(subject)) continue;
            if (source != null && !noteSource.equalsIgnoreCase(source)) continue;

            if (!AcademicFilterHelper.matchesDeptSemFilter(
                    note.getDepartmentId(), note.getSemesterId(), note.getSubjectId(),
                    dept, sem, subjectMap, allSubjects, noteSubject)) {
                continue;
            }
            filtered.add(note);
        }
        displayNotes(filtered);
    }

    @FXML
    public void clearFilters() {
        AcademicFilterHelper.resetFilters(academicService, departmentFilter, semesterFilter, subjectFilter);
        sourceFilter.getSelectionModel().clearSelection();
        displayNotes(allNotes);
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
