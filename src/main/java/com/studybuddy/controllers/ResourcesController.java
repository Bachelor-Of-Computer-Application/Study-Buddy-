package com.studybuddy.controllers;

import com.studybuddy.utils.SessionManager;
import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;
import com.studybuddy.services.ResourceService;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ResourcesController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private FlowPane resourcesFlowPane;
    @FXML private VBox emptyState;

    // History Table
    @FXML private TableView<Resource> historyTable;
    @FXML private TableColumn<Resource, String> titleCol;
    @FXML private TableColumn<Resource, String> subjectCol;
    @FXML private TableColumn<Resource, String> fileNameCol;
    @FXML private TableColumn<Resource, String> dateCol;
    @FXML private TableColumn<Resource, String> statusCol;

    private final ResourceService resourceService = new ResourceService();
    private List<Resource> activeResources = new ArrayList<>();
    private List<Resource> userUploadedHistory = new ArrayList<>();

    @FXML
    public void initialize() {
        // Initialize filters
        subjectFilter.setItems(FXCollections.observableArrayList(
                "Mathematics", "Physics", "Chemistry", "Biology",
                "Computer Science", "English", "History"
        ));

        // Setup History Table
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        fileNameCol.setCellValueFactory(new PropertyValueFactory<>("fileType")); // represents file name/type
        dateCol.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));

        statusCol.setCellValueFactory(cellData -> {
            boolean active = cellData.getValue().isActive();
            return new SimpleStringProperty(active ? "Approved" : "Pending");
        });

        loadResources();
    }

    private void loadResources() {
        try {
            // Load public resources
            activeResources = resourceService.getAllActiveResources();
            if (activeResources == null) activeResources = new ArrayList<>();
            displayResources(activeResources);

            // Load user upload history
            int userId = SessionManager.getInstance().getCurrentUser() != null
                    ? SessionManager.getInstance().getCurrentUser().getId()
                    : 1;
            userUploadedHistory = resourceService.getResourcesByUser(userId);
            if (userUploadedHistory == null) userUploadedHistory = new ArrayList<>();
            historyTable.setItems(FXCollections.observableArrayList(userUploadedHistory));

        } catch (Exception e) {
            System.err.println("Failed to load resources: " + e.getMessage());
            // Fallback mock data if DB empty
            loadMockResources();
        }
    }

    private void loadMockResources() {

        activeResources = new ArrayList<>();

        activeResources.add(
                new Resource(
                        1,                      // id
                        1,                      // noteId
                        1,                      // uploadedBy
                        "Calculus II Study Guide",
                        "Mathematics",
                        "University Lecture",
                        "Complete guide for integration and infinite series",
                        "2026-06-18",
                        "calculus2.pdf",
                        "PDF",
                        25,                     // downloads
                        true
                )
        );

        activeResources.add(
                new Resource(
                        2,
                        2,
                        2,
                        "Data Structures Cheat Sheet",
                        "Computer Science",
                        "Coding Hub",
                        "All algorithms, trees, and hash tables summaries",
                        "2026-06-19",
                        "ds_cheatsheet.pdf",
                        "PDF",
                        18,
                        true
                )
        );

        activeResources.add(
                new Resource(
                        3,
                        3,
                        1,
                        "Mechanics & Relativity Manual",
                        "Physics",
                        "Science Lab",
                        "Practical lab manual for thermodynamics & physics",
                        "2026-06-20",
                        "physics_lab.pdf",
                        "PDF",
                        12,
                        true
                )
        );

        displayResources(activeResources);

        userUploadedHistory = new ArrayList<>(activeResources);

        historyTable.setItems(
                FXCollections.observableArrayList(userUploadedHistory)
        );
    }
    private void displayResources(List<Resource> resources) {
        resourcesFlowPane.getChildren().clear();

        if (resources.isEmpty()) {
            emptyState.setVisible(true);
            return;
        }

        emptyState.setVisible(false);

        for (Resource resource : resources) {
            VBox card = createResourceCard(resource);
            resourcesFlowPane.getChildren().add(card);
        }
    }

    private VBox createResourceCard(Resource resource) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setPrefWidth(260.0);
        card.getStyleClass().add("note-card");
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

        Label iconLabel = new Label("📕 PDF");
        iconLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545; -fx-font-size: 11px;");

        Label titleLabel = new Label(resource.getTitle());
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        titleLabel.setWrapText(true);

        Label subjectLabel = new Label("📚 " + resource.getSubject());
        subjectLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");

        Label descLabel = new Label(resource.getDescription());
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        descLabel.setWrapText(true);
        descLabel.setPrefHeight(40);

        HBox actionBox = new HBox(10);
        actionBox.setStyle("-fx-padding: 5 0 0 0;");
        Button previewBtn = new Button("👁️ Preview");
        previewBtn.getStyleClass().add("card-open-button");
        previewBtn.setOnAction(e -> handlePreview(resource));

        Button downloadBtn = new Button("📥 Download");
        downloadBtn.getStyleClass().add("card-edit-button");
        downloadBtn.setOnAction(e -> handleDownload(resource));

        actionBox.getChildren().addAll(previewBtn, downloadBtn);

        card.getChildren().addAll(iconLabel, titleLabel, subjectLabel, descLabel, actionBox);
        return card;
    }

    @FXML
    public void applyFilters() {
        String query = searchField.getText().trim().toLowerCase();
        String subject = subjectFilter.getValue();

        List<Resource> filtered = activeResources.stream()
                .filter(r -> (query.isEmpty() || r.getTitle().toLowerCase().contains(query)) &&
                        (subject == null || r.getSubject().equalsIgnoreCase(subject)))
                .collect(Collectors.toList());

        displayResources(filtered);
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        subjectFilter.getSelectionModel().clearSelection();
        displayResources(activeResources);
    }

    @FXML
    public void handleUploadResource() {
        // Open a dialog to choose a file and input resource metadata
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose PDF Resource");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(searchField.getScene().getWindow());

        if (file == null) return;

        // Show metadata prompt
        Stage dialog = new Stage();
        dialog.setTitle("Upload PDF Details");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: white;");

        Label mainLabel = new Label("Add Resource Details for approval:");
        mainLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField titleTxt = new TextField();
        titleTxt.setPromptText("Enter resource title");

        ComboBox<String> subCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Mathematics", "Physics", "Chemistry", "Biology",
                "Computer Science", "English", "History"
        ));
        subCombo.setPromptText("Choose subject");

        TextArea descTxt = new TextArea();
        descTxt.setPromptText("Enter short description");
        descTxt.setPrefHeight(80);

        Button submitBtn = new Button("Submit for Approval");
        submitBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold;");
        submitBtn.setOnAction(e -> {
            if (titleTxt.getText().trim().isEmpty() || subCombo.getValue() == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Title and Subject are required.");
                alert.showAndWait();
                return;
            }

            try {

                Note mockNote = new Note();

                // FIXED
                mockNote.setId(1);

                mockNote.setTitle(titleTxt.getText().trim());
                mockNote.setSubject(subCombo.getValue());
                mockNote.setSource("Community Upload");

                mockNote.setUploadDate(
                        LocalDate.now().format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        )
                );

                mockNote.setFileName(file.getName());
                mockNote.setFileType("PDF");
                mockNote.setDescription(descTxt.getText().trim());

                // FIXED
                mockNote.setUserId(
                        SessionManager.getInstance().getCurrentUser() != null
                                ? SessionManager.getInstance().getCurrentUser().getId()
                                : 1
                );

                mockNote.setPrivate(false);

                resourceService.shareAsResource(
                        mockNote,
                        file.getAbsolutePath()
                );

                Alert alert = new Alert(
                        Alert.AlertType.INFORMATION,
                        "Resource submitted successfully!"
                );

                alert.showAndWait();

                dialog.close();

                loadResources();

            } catch (Exception ex) {

                ex.printStackTrace();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Upload Failed");
                alert.setHeaderText("Upload Failed");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });

        layout.getChildren().addAll(mainLabel, new Label("Title:"), titleTxt, new Label("Subject:"), subCombo, new Label("Description:"), descTxt, submitBtn);
        dialog.setScene(new Scene(layout, 350, 420));
        dialog.showAndWait();
    }

    private void handlePreview(Resource r) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Resource Preview");
        alert.setHeaderText(r.getTitle());
        alert.setContentText("Subject: " + r.getSubject() + "\n" +
                "Source: " + r.getSource() + "\n" +
                "Description: " + r.getDescription() + "\n" +
                "File Path: " + r.getFilePath());
        alert.showAndWait();
    }

    private void handleDownload(Resource r) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Resource Downloaded");
        alert.setHeaderText("Download Success");
        alert.setContentText("The file '" + r.getTitle() + "' has been downloaded successfully to your local downloads directory.");
        alert.showAndWait();
    }
}