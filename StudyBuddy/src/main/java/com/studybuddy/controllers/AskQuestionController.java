package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Department;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.models.UserActivity;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.QuestionService;
import com.studybuddy.utils.AcademicFilterHelper;
import com.studybuddy.utils.EventBus;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller for AskQuestionView.fxml - with cascading Department → Semester → Subject.
 */
public class AskQuestionController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private Label pageTitle;
    @FXML private TextArea questionTextArea;
    
    @FXML private ComboBox<Department> departmentComboBox;
    @FXML private ComboBox<Semester> semesterComboBox;
    @FXML private ComboBox<Subject> subjectComboBox;
    
    @FXML private ComboBox<String> rewardPointsComboBox;
    @FXML private Label userPointsLabel;
    @FXML private Button submitButton;
    @FXML private Button toolsButton;
    @FXML private Button filesButton;
    @FXML private Button attachmentButton;
    @FXML private Label attachmentLabel;
    @FXML private Button homeButton;
    @FXML private Button profileButton;

    private QuestionService questionService;
    private AcademicService academicService;
    private final com.studybuddy.dao.UserActivityDAO activityDAO = new com.studybuddy.dao.UserActivityDAO();
    private String selectedAttachmentPath = null;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        questionService = new QuestionService();
        academicService = AcademicService.getInstance();

        departmentComboBox.setItems(AcademicFilterHelper.departmentsForFilter(academicService));
        departmentComboBox.setValue(AcademicFilterHelper.allDepartments());
        semesterComboBox.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
        semesterComboBox.setValue(AcademicFilterHelper.allSemesters());

        AcademicFilterHelper.wireCascade(academicService, departmentComboBox, semesterComboBox, subjectComboBox,
                () -> AcademicFilterHelper.loadSubjects(academicService, departmentComboBox.getValue(), semesterComboBox.getValue(), subjectComboBox));
        AcademicFilterHelper.loadSubjects(academicService, departmentComboBox.getValue(), semesterComboBox.getValue(), subjectComboBox);
        loadUserPoints();

        if (rewardPointsComboBox != null) {
            rewardPointsComboBox.setItems(FXCollections.observableArrayList("0", "10", "20", "50", "100"));
            rewardPointsComboBox.setValue("0");
        }
        setupButtonHoverEffects();
        setupSubmitButtonEffects();
    }


    private void loadUserPoints() {
        int points = questionService.getAchievementPoints();
        if (userPointsLabel != null) {
            userPointsLabel.setText("" + points);
        }
    }

    /**
     * Refreshes the user's points display. Called after point deductions.
     * Requirement 5: Refresh achievement_points after question creation.
     */
    public void refreshUserPoints() {
        loadUserPoints();
    }

    private void setupButtonHoverEffects() {
        // Hover styling handled via CSS (.toolbar-button:hover, .submit-button:hover)
    }

    private void setupSubmitButtonEffects() {
        if (submitButton == null) return;
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
    @FXML
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
    @FXML
    public void handleSymbols(ActionEvent actionEvent) {
        if (questionTextArea == null) return;
        String currentText = questionTextArea.getText();
        int cursorPosition = questionTextArea.getCaretPosition();
        String symbol = "∑ ∆ ∞ ≠ ≤ ≥";
        questionTextArea.setText(currentText.substring(0, cursorPosition) + symbol +
                currentText.substring(cursorPosition));
        questionTextArea.positionCaret(cursorPosition + symbol.length());
    }

    @FXML
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

    @FXML
    public void handleSubmit(ActionEvent actionEvent) {
        if (!validateForm()) {
            return;
        }
        try {
            String questionText = questionTextArea.getText().trim();
            Subject selectedSubject = subjectComboBox.getValue();
            String subject = selectedSubject != null ? selectedSubject.getName() : "General";
            int subjectId = selectedSubject != null ? selectedSubject.getId() : 0;
            Integer deptId = AcademicFilterHelper.resolveDepartmentId(departmentComboBox.getValue());
            Integer semId = AcademicFilterHelper.resolveSemesterId(semesterComboBox.getValue());

            // Requirement 2: Parse reward points
            int rewardPoints = 0;
            String rewardStr = rewardPointsComboBox.getValue();
            if (rewardStr != null) {
                try { rewardPoints = Integer.parseInt(rewardStr); } catch (NumberFormatException ignored) {}
            }

            // Requirement 2: Validate reward > 0 and balance before deducting
            if (rewardPoints > 0) {
                int currentPoints = questionService.getAchievementPoints();
                if (rewardPoints > currentPoints) {
                    showAlert(Alert.AlertType.WARNING, "Insufficient Points",
                            "You don't have enough achievement points.",
                            "You have " + currentPoints + " points but selected " + rewardPoints + " points as reward.");
                    return;
                }
            }

            int currentPoints = questionService.getAchievementPoints();
            // Requirement 2: Deduct points and save question (atomic operation)
            boolean success = questionService.saveQuestionWithDeduction(
                    questionText, subject, subjectId, rewardPoints, selectedAttachmentPath, deptId, semId
            );

            if (success) {
                // Log activity
                if (App.getCurrentUser() != null) {
                    UserActivity activity = new UserActivity(
                            App.getCurrentUser().getId(),
                            App.getCurrentUser().getDisplayFullName(),
                            "ASK_QUESTION",
                            "QUESTION",
                            questionText.length() > 50 ? questionText.substring(0, 47) + "..." : questionText
                    );
                    try {
                        activityDAO.logActivity(activity);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }

                // Publish events
                EventBus.getInstance().publish(new EventBus.QuestionsChangedEvent());
                EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());

                // Refresh user's points in session
                if (App.getCurrentUser() != null) {
                    int newBalance = currentPoints - rewardPoints;
                    App.getCurrentUser().setAchievementPoints(newBalance);
                }
                loadUserPoints();

                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Your question has been submitted successfully!",
                        rewardPoints > 0 ? rewardPoints + " points have been deducted from your balance." : "");
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
        if (departmentComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please choose a department.", "Select a department or All Departments.");
            return false;
        }
        if (semesterComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please choose a semester.", "Select a semester or All Semesters.");
            return false;
        }
        if (!AcademicFilterHelper.isAllSemesters(semesterComboBox.getValue()) && subjectComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please choose a subject.", "Required when a specific semester is selected.");
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
        departmentComboBox.setValue(AcademicFilterHelper.allDepartments());
        semesterComboBox.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
        semesterComboBox.setValue(AcademicFilterHelper.allSemesters());
        semesterComboBox.setDisable(false);
        subjectComboBox.getItems().clear();
        subjectComboBox.setValue(null);
        AcademicFilterHelper.loadSubjects(academicService, departmentComboBox.getValue(), semesterComboBox.getValue(), subjectComboBox);
        rewardPointsComboBox.getSelectionModel().select(0);
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
            Parent homeView = loader.load();
            rootPane.getScene().setRoot(homeView);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to load home page.", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigates back to the main Home view where the user can access ProfileView
     * through the sidebar, or directly loads ProfileView into the scene root.
     */
    public void handleProfile(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/studybuddy/fxml/HomeView.fxml")
            );
            Parent homeView = loader.load();
            rootPane.getScene().setRoot(homeView);

            // After navigation, trigger profile view
            HomeController homeController = loader.getController();
            if (homeController != null) {
                homeController.goToProfile();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to open profile page.", e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}