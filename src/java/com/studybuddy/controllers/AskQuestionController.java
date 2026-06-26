package com.studybuddy.controllers;

import com.studybuddy.services.QuestionService;
import com.studybuddy.services.ResourceService;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.stage.FileChooser;

public class AskQuestionController implements Initializable {

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label pageTitle;

    @FXML
    private TextArea questionTextArea;

    @FXML
    private ComboBox<String> subjectComboBox;

    @FXML
    private ComboBox<Integer> rewardPointsComboBox;

    @FXML
    private Label userPointsLabel;

    @FXML
    private Button submitButton;

    @FXML
    private Button mathButton;

    @FXML
    private Button symbolsButton;

    @FXML
    private Button attachmentButton;

    @FXML
    private Label attachmentLabel;

    @FXML
    private Button logoutButton;

    @FXML
    private Button homeButton;

    @FXML
    private Button profileButton;

    private QuestionService questionService;
    private ResourceService resourceService;
    private String selectedAttachmentPath = null;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        questionService = new QuestionService();
        resourceService = new ResourceService();

        initializeComboBoxes();
        setupButtonHoverEffects();
        setupSubmitButtonEffects();

        loadSubjects();
        loadUserPoints();
    }

    private void initializeComboBoxes() {
        // Reward Points ComboBox
        ArrayList<Integer> points = new ArrayList<>();
        points.add(5);
        points.add(10);
        points.add(15);
        points.add(20);
        points.add(25);
        points.add(30);

        rewardPointsComboBox.setItems(FXCollections.observableArrayList(points));
        rewardPointsComboBox.getSelectionModel().select(1); // Default: 10
    }

    private void loadSubjects() {
        List<String> subjects = questionService.getAvailableSubjects();
        subjectComboBox.setItems(FXCollections.observableArrayList(subjects));
    }

    private void loadUserPoints() {
        int points = questionService.getUserPoints();
        userPointsLabel.setText("You have " + points + " points");
    }

    private void setupButtonHoverEffects() {
        setupHoverEffect(mathButton);
        setupHoverEffect(symbolsButton);
        setupHoverEffect(attachmentButton);
        setupHoverEffect(logoutButton);
        setupHoverEffect(homeButton);
        setupHoverEffect(profileButton);
    }

    private void setupHoverEffect(Button button) {
        button.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            button.setStyle(button.getStyle() + " -fx-background-color: #FFFFFF; -fx-cursor: hand;");
        });

        button.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            button.setStyle(button.getStyle().replace(" -fx-background-color: #FFFFFF;", ""));
        });
    }

    private void setupSubmitButtonEffects() {
        submitButton.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            submitButton.setStyle(submitButton.getStyle() + " -fx-background-color: #333333;");
        });

        submitButton.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            submitButton.setStyle(submitButton.getStyle().replace(" -fx-background-color: #333333;", ""));
        });

        submitButton.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), submitButton);
            scale.setToX(0.98);
            scale.setToY(0.98);
            scale.play();

            scale.setOnFinished(e2 -> {
                ScaleTransition restore = new ScaleTransition(Duration.millis(100), submitButton);
                restore.setToX(1.0);
                restore.setToY(1.0);
                restore.play();
            });
        });
    }

    public void handleMath(ActionEvent actionEvent) {
        // Insert math symbols at cursor position
        String currentText = questionTextArea.getText();
        int cursorPosition = questionTextArea.getCaretPosition();

        String mathSymbol = "⁠x² + y² = z²";
        questionTextArea.setText(currentText.substring(0, cursorPosition) + mathSymbol +
                currentText.substring(cursorPosition));
        questionTextArea.positionCaret(cursorPosition + mathSymbol.length());
    }

    public void handleSymbols(ActionEvent actionEvent) {
        // Insert mathematical symbols
        String currentText = questionTextArea.getText();
        int cursorPosition = questionTextArea.getCaretPosition();

        String symbol = "∑ ∆ ∞ ≠ ≤ ≥";
        questionTextArea.setText(currentText.substring(0, cursorPosition) + symbol +
                currentText.substring(cursorPosition));
        questionTextArea.positionCaret(cursorPosition + symbol.length());
    }

    public void handleAttachment(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Attach File");
        fileChooser.setInitialFileName("");

        // Set file extensions
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                "Document & Image Files (*.pdf, *.doc, *.docx, *.png, *.jpg)",
                "*.pdf", "*.doc", "*.docx", "*.png", "*.jpg"
        );
        fileChooser.getExtensionFilters().add(filter);

        File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

        if (selectedFile != null) {
            selectedAttachmentPath = selectedFile.getAbsolutePath();
            attachmentLabel.setText("📎 " + selectedFile.getName());
        }
    }

    public void handleSubmit(ActionEvent actionEvent) {
        if (!validateForm()) {
            return;
        }

        try {
            String questionText = questionTextArea.getText().trim();
            String subject = subjectComboBox.getValue();
            int rewardPoints = rewardPointsComboBox.getValue();

            boolean success = questionService.saveQuestion(
                    questionText,
                    subject,
                    rewardPoints,
                    selectedAttachmentPath
            );

            if (success) {
                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Success",
                        "Your question has been submitted successfully!",
                        "Question ID will be generated in the database."
                );
                clearForm();
            } else {
                showAlert(
                        Alert.AlertType.ERROR,
                        "Error",
                        "Failed to submit your question.",
                        "Please try again or contact support."
                );
            }
        } catch (Exception e) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "An unexpected error occurred.",
                    e.getMessage()
            );
        }
    }

    private boolean validateForm() {
        // Check if question is empty
        if (questionTextArea.getText().trim().isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Validation Error",
                    "Please enter your question.",
                    "The question field cannot be empty."
            );
            return false;
        }

        // Check if subject is selected
        if (subjectComboBox.getValue() == null) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Validation Error",
                    "Please choose a subject.",
                    "Select a subject from the dropdown menu."
            );
            return false;
        }

        // Check if reward points are selected
        if (rewardPointsComboBox.getValue() == null) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Validation Error",
                    "Please select reward points.",
                    "Choose the number of points you want to offer."
            );
            return false;
        }

        return true;
    }

    private void clearForm() {
        questionTextArea.clear();
        subjectComboBox.getSelectionModel().clearSelection();
        rewardPointsComboBox.getSelectionModel().select(1); // Reset to 10
        selectedAttachmentPath = null;
        attachmentLabel.setText("");
    }

    public void handleLogout(ActionEvent actionEvent) {
        showAlert(
                Alert.AlertType.INFORMATION,
                "Logout",
                "You have been logged out.",
                "Thank you for using Study Buddy!"
        );
    }

    public void handleHome(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/studybuddy/view/HomeView.fxml")
            );
            BorderPane homeView = loader.load();
            rootPane.getScene().setRoot(homeView);
        } catch (IOException e) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Failed to load home page.",
                    e.getMessage()
            );
        }
    }

    public void handleProfile(ActionEvent actionEvent) {
        showAlert(
                Alert.AlertType.INFORMATION,
                "Profile",
                "Profile page will be opened.",
                "View and edit your account settings."
        );
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setStyle("-fx-background-color: #FFFFFF;");
        alert.showAndWait();
    }
}