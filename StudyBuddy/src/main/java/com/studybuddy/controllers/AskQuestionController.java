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

/**
 * Controller for AskQuestionView.fxml.
 *
 * Key fixes applied:
 * 1. rewardPointsComboBox changed to ComboBox<String> to match FXML (which uses String values).
 * 2. mathButton / symbolsButton / logoutButton fx:ids removed — FXML uses toolsButton / filesButton.
 *    setupButtonHoverEffects() now uses the correct fx:ids from the FXML.
 * 3. handleHome() fixed — old path /com/studybuddy/view/HomeView.fxml was wrong.
 *    Correct path is /com/studybuddy/fxml/HomeView.fxml.
 * 4. userPointsLabel is present in the controller but has no fx:id in the FXML —
 *    guarded with null check to prevent NPE.
 */
public class AskQuestionController implements Initializable {

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label pageTitle;

    @FXML
    private TextArea questionTextArea;

    @FXML
    private ComboBox<String> subjectComboBox;

    /**
     * NOTE: FXML defines rewardPointsComboBox items as String values ("0", "10", "20", "50", "100").
     * Changed from ComboBox<Integer> to ComboBox<String> to match.
     */
    @FXML
    private ComboBox<String> rewardPointsComboBox;

    /** May be null if fx:id is absent in FXML — always guarded with null check. */
    @FXML
    private Label userPointsLabel;

    @FXML
    private Button submitButton;

    /**
     * FXML uses fx:id="toolsButton" — maps to handleMath action.
     */
    @FXML
    private Button toolsButton;

    /**
     * FXML uses fx:id="filesButton" — maps to handleSymbols action.
     */
    @FXML
    private Button filesButton;

    @FXML
    private Button attachmentButton;

    @FXML
    private Label attachmentLabel;

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

        loadSubjects();
        loadUserPoints();
        setupButtonHoverEffects();
        setupSubmitButtonEffects();
    }

    private void loadSubjects() {
        List<String> subjects = questionService.getAvailableSubjects();
        subjectComboBox.setItems(FXCollections.observableArrayList(subjects));
    }

    private void loadUserPoints() {
        int points = questionService.getUserPoints();
        // userPointsLabel has no fx:id in the current FXML — guard against null
        if (userPointsLabel != null) {
            userPointsLabel.setText("You have " + points + " points");
        }
    }

    private void setupButtonHoverEffects() {
        // Only set up effects on buttons that are actually wired from FXML
        setupHoverEffect(toolsButton);
        setupHoverEffect(filesButton);
        setupHoverEffect(attachmentButton);
        setupHoverEffect(homeButton);
        setupHoverEffect(profileButton);
    }

    /**
     * Null-safe hover effect setup — skips buttons that were not injected from FXML.
     */
    private void setupHoverEffect(Button button) {
        if (button == null) return;
        button.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            button.setStyle(button.getStyle() + " -fx-background-color: #FFFFFF; -fx-cursor: hand;");
        });
        button.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            button.setStyle(button.getStyle().replace(" -fx-background-color: #FFFFFF;", ""));
        });
    }

    private void setupSubmitButtonEffects() {
        if (submitButton == null) return;
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

    /** Bound to toolsButton (fx:id="toolsButton") — inserts math formula example. */
    public void handleMath(ActionEvent actionEvent) {
        if (questionTextArea == null) return;
        String currentText = questionTextArea.getText();
        int cursorPosition = questionTextArea.getCaretPosition();
        String mathSymbol = "x² + y² = z²";
        questionTextArea.setText(currentText.substring(0, cursorPosition) + mathSymbol +
                currentText.substring(cursorPosition));
        questionTextArea.positionCaret(cursorPosition + mathSymbol.length());
    }

    /** Bound to filesButton (fx:id="filesButton") — inserts math symbols. */
    public void handleSymbols(ActionEvent actionEvent) {
        if (questionTextArea == null) return;
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
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                "Document & Image Files (*.pdf, *.doc, *.docx, *.png, *.jpg)",
                "*.pdf", "*.doc", "*.docx", "*.png", "*.jpg"
        );
        fileChooser.getExtensionFilters().add(filter);
        File selectedFile = fileChooser.showOpenDialog(
                rootPane != null ? rootPane.getScene().getWindow() : null
        );
        if (selectedFile != null) {
            selectedAttachmentPath = selectedFile.getAbsolutePath();
            if (attachmentLabel != null) {
                attachmentLabel.setText("📎 " + selectedFile.getName());
            }
        }
    }

    public void handleSubmit(ActionEvent actionEvent) {
        if (!validateForm()) {
            return;
        }
        try {
            String questionText = questionTextArea.getText().trim();
            String subject = subjectComboBox.getValue();
            // rewardPointsComboBox now holds String values; parse safely
            int rewardPoints = 0;
            String rewardStr = rewardPointsComboBox.getValue();
            if (rewardStr != null) {
                try { rewardPoints = Integer.parseInt(rewardStr); } catch (NumberFormatException ignored) {}
            }

            boolean success = questionService.saveQuestion(
                    questionText,
                    subject,
                    rewardPoints,
                    selectedAttachmentPath
            );

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Your question has been submitted successfully!",
                        "Question ID will be generated in the database.");
                clearForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to submit your question.",
                        "Please try again or contact support.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "An unexpected error occurred.", e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateForm() {
        if (questionTextArea == null || questionTextArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please enter your question.", "The question field cannot be empty.");
            return false;
        }
        if (subjectComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please choose a subject.", "Select a subject from the dropdown menu.");
            return false;
        }
        if (rewardPointsComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please select reward points.", "Choose the number of points you want to offer.");
            return false;
        }
        return true;
    }

    private void clearForm() {
        if (questionTextArea != null) questionTextArea.clear();
        subjectComboBox.getSelectionModel().clearSelection();
        rewardPointsComboBox.getSelectionModel().select(1); // Reset to "10"
        selectedAttachmentPath = null;
        if (attachmentLabel != null) attachmentLabel.setText("");
    }

    public void handleLogout(ActionEvent actionEvent) {
        showAlert(Alert.AlertType.INFORMATION, "Logout",
                "You have been logged out.", "Thank you for using Study Buddy!");
    }

    /**
     * FIXED: old path "/com/studybuddy/view/HomeView.fxml" was incorrect.
     * Correct path is "/com/studybuddy/fxml/HomeView.fxml".
     */
    public void handleHome(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/studybuddy/fxml/HomeView.fxml")
            );
            BorderPane homeView = loader.load();
            rootPane.getScene().setRoot(homeView);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to load home page.", e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleProfile(ActionEvent actionEvent) {
        showAlert(Alert.AlertType.INFORMATION, "Profile",
                "Profile page will be opened.",
                "View and edit your account settings.");
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}