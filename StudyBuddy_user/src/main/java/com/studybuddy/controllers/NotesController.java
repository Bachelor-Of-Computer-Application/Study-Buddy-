package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Note;
import com.studybuddy.models.User;
import com.studybuddy.services.NoteService;
import com.studybuddy.services.ResourceService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class NotesController implements Initializable {

    @FXML private ComboBox<String> subjectFilter;
    @FXML private ComboBox<String> sourceFilter;
    @FXML private VBox myNotesContainer;
    @FXML private VBox communityNotesContainer;
    @FXML private VBox emptyState;

    private NoteService noteService;
    private ResourceService resourceService;
    private List<Note> allNotes = Collections.emptyList();
    private int currentUserId = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        noteService = new NoteService();
        resourceService = new ResourceService();

        User currentUser = App.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
        } else {
            showError("No logged-in user. Database operations require an authenticated user.");
        }

        loadSubjects();
        loadSources();
        loadNotes();
    }

    private void loadSubjects() {
        subjectFilter.setItems(FXCollections.observableArrayList(
            "Mathematics", "Physics", "Chemistry", "Biology",
            "Computer Science", "English", "History"
        ));
    }

    private void loadSources() {
        sourceFilter.setItems(FXCollections.observableArrayList(
            "Textbook", "Lecture", "Online Course", "Research Paper",
            "Notes", "Video Tutorial", "Other"
        ));
    }

    private void loadNotes() {
        try {
            allNotes = noteService.getNotesByUserId(currentUserId);
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
                VBox card = createPersonalNoteCard(note);
                myNotesContainer.getChildren().add(card);
                hasPersonal = true;
            } else {
                VBox card = createCommunityNoteCard(note);
                communityNotesContainer.getChildren().add(card);
                hasCommunity = true;
            }
        }

        emptyState.setVisible(!hasPersonal && !hasCommunity);
    }

    private VBox createPersonalNoteCard(Note note) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 10;");
        card.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(note.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label metaLabel = new Label("📅 " + note.getUploadDate() + " | 📄 " + note.getFileType());
        metaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        HBox metaBox = new HBox(15);
        Label subjectLabel = new Label("📚 " + note.getSubject());
        subjectLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4f46e5; -fx-font-weight: bold;");
        Label sourceLabel = new Label("🔗 " + note.getSource());
        sourceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #0284c7;");
        metaBox.getChildren().addAll(subjectLabel, sourceLabel);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteNote(note));

        Button openBtn = new Button("📖 Open");
        openBtn.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-font-size: 11px; -fx-cursor: hand;");
        openBtn.setOnAction(e -> openNote(note));

        buttonBox.getChildren().addAll(deleteBtn, openBtn);
        card.getChildren().addAll(titleLabel, metaLabel, metaBox, buttonBox);

        return card;
    }

    private VBox createCommunityNoteCard(Note note) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 10;");
        card.setMaxWidth(Double.MAX_VALUE);

        // Status Badge
        Label statusBadge = new Label();
        String id = note.getId().toLowerCase();
        if (id.contains("pending")) {
            statusBadge.setText("Pending Approval");
            statusBadge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #d97706; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 5;");
        } else if (id.contains("rejected")) {
            statusBadge.setText("Rejected");
            statusBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 5;");
        } else {
            statusBadge.setText("Approved & Live");
            statusBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 5;");
        }

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(note.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        titleRow.getChildren().addAll(titleLabel, statusBadge);

        Label metaLabel = new Label("📅 " + note.getUploadDate() + " | 📄 " + note.getFileType());
        metaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        HBox metaBox = new HBox(15);
        Label subjectLabel = new Label("📚 " + note.getSubject());
        subjectLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4f46e5; -fx-font-weight: bold;");
        Label sourceLabel = new Label("🔗 " + note.getSource());
        sourceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #0284c7;");
        metaBox.getChildren().addAll(subjectLabel, sourceLabel);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button openBtn = new Button("📖 View File");
        openBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 11px; -fx-cursor: hand;");
        openBtn.setOnAction(e -> openNote(note));
        buttonBox.getChildren().add(openBtn);

        card.getChildren().addAll(titleRow, metaLabel, metaBox, buttonBox);

        return card;
    }

    public void showCreateNoteDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/studybuddy/fxml/CreateNoteDialog.fxml"));
            VBox dialogContent = loader.load();

            Stage dialog = new Stage();
            dialog.setTitle("Create New Note");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(dialogContent));
            dialog.setResizable(false);
            dialog.showAndWait();
            
            loadNotes(); // Refresh
        } catch (IOException e) {
            showError("Failed to open create note dialog: " + e.getMessage());
        }
    }

    private void deleteNote(Note note) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Note");
        alert.setHeaderText("Are you sure you want to delete this note?");
        alert.setContentText(note.getTitle());

        ButtonType yes = new ButtonType("Yes, Delete");
        ButtonType no = new ButtonType("No, Keep");
        alert.getButtonTypes().setAll(yes, no);

        if (alert.showAndWait().orElse(no) == yes) {
            try {
                noteService.deleteNote(note.getId());
                loadNotes();
                showSuccess("Saved to database successfully");
            } catch (Exception e) {
                showError("Failed to delete note: " + e.getMessage());
            }
        }
    }

    private void openNote(Note note) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Note File Preview");
        alert.setHeaderText(note.getTitle());
        alert.setContentText("Format: " + note.getFileType() + "\nSource: " + note.getSource() + "\nDescription: " + note.getDescription());
        alert.showAndWait();
    }

    public void applyFilters() {
        String subject = subjectFilter.getValue();
        String source = sourceFilter.getValue();

        displayNotes(allNotes.stream()
                .filter(note -> subject == null || note.getSubject().equalsIgnoreCase(subject))
                .filter(note -> source == null || note.getSource().equalsIgnoreCase(source))
                .toList());
    }

    public void clearFilters() {
        subjectFilter.getSelectionModel().clearSelection();
        sourceFilter.getSelectionModel().clearSelection();
        displayNotes(allNotes);
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
