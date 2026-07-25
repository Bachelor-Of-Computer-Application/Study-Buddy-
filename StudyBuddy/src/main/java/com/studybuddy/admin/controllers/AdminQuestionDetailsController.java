package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.admin.services.NotificationService;
import com.studybuddy.models.Answer;
import com.studybuddy.models.Notification;
import com.studybuddy.models.Question;
import com.studybuddy.services.QuestionService;
import com.studybuddy.utils.EventBus;
import com.studybuddy.utils.StringUtils;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for the Question Details view.
 * Displays question information and answers with Best Answer selection functionality.
 */
public class AdminQuestionDetailsController {

    private static final Logger logger = Logger.getLogger(AdminQuestionDetailsController.class.getName());

    @FXML private Label lblQuestionTitle;
    @FXML private Label lblQuestionAuthor;
    @FXML private Label lblTitle;
    @FXML private Label lblDescription;
    @FXML private Label lblOwner;
    @FXML private Label lblRewardPoints;
    @FXML private Label lblApprovalStatus;
    @FXML private Label lblRewardStatus;
    @FXML private Label lblDepartment;
    @FXML private Label lblSemester;
    @FXML private Label lblSubject;
    @FXML private Label lblDate;
    @FXML private VBox answersContainer;
    @FXML private Button btnApproveQuestion;

    private final AdminService adminService = AdminService.getInstance();
    private final QuestionService questionService = new QuestionService();
    private Question currentQuestion;
    private Stage stage;

    @FXML
    public void initialize() {
        // Subscribe to events for auto-refresh
        EventBus.getInstance().subscribe(EventBus.QuestionsChangedEvent.class, (event) -> {
            if (currentQuestion != null) {
                loadQuestionDetails(currentQuestion.getId());
            }
        });
    }

    public void setQuestion(Question question) {
        this.currentQuestion = question;
        loadQuestionDetails(question.getId());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void loadQuestionDetails(int questionId) {
        try {
            currentQuestion = questionService.getQuestionById(questionId);
            if (currentQuestion == null) {
                showError("Question not found");
                return;
            }

            // Load question info
            String title = currentQuestion.getTitle() != null ? currentQuestion.getTitle() : 
                          (currentQuestion.getQuestionText() != null && currentQuestion.getQuestionText().length() > 50 ?
                           currentQuestion.getQuestionText().substring(0, 50) + "..." : currentQuestion.getQuestionText());
            
            lblQuestionTitle.setText(title);
            lblQuestionAuthor.setText("by " + StringUtils.nullSafe(currentQuestion.getAuthorName()));
            lblTitle.setText(StringUtils.nullSafe(currentQuestion.getTitle()));
            lblDescription.setText(StringUtils.nullSafe(currentQuestion.getQuestionText()));
            lblOwner.setText(StringUtils.nullSafe(currentQuestion.getAuthorName()));
            lblRewardPoints.setText(String.valueOf(currentQuestion.getRewardPoints()));

            // Check approval status
            boolean isApproved = questionService.isQuestionApproved(questionId);
            lblApprovalStatus.setText(isApproved ? "✅ Approved" : "⏳ Pending");
            lblApprovalStatus.setStyle(isApproved ? "-fx-text-fill: #22c55e;" : "-fx-text-fill: #f59e0b;");

            // Sync approve button state — disable once approved
            if (btnApproveQuestion != null) {
                if (isApproved) {
                    btnApproveQuestion.setDisable(true);
                    btnApproveQuestion.setText("✅ Approved");
                } else {
                    btnApproveQuestion.setDisable(false);
                    btnApproveQuestion.setText("✅ Approve Question");
                }
            }

            // Show reward status with human-readable labels and colour
            String rewardStatus = currentQuestion.getRewardStatus() != null
                    ? currentQuestion.getRewardStatus() : "PENDING";
            if ("REWARDED".equalsIgnoreCase(rewardStatus)
                    || "TRANSFERRED".equalsIgnoreCase(rewardStatus)) {
                lblRewardStatus.setText("✔ Rewarded");
                lblRewardStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            } else if ("ACCEPTED".equalsIgnoreCase(rewardStatus)) {
                lblRewardStatus.setText("✔ Accepted (no points)");
                lblRewardStatus.setStyle("-fx-text-fill: #22c55e;");
            } else {
                lblRewardStatus.setText("⏳ Pending");
                lblRewardStatus.setStyle("-fx-text-fill: #f59e0b;");
            }

            // Populate department, semester, subject, date
            if (lblDepartment != null) lblDepartment.setText(StringUtils.nullSafe(currentQuestion.getUserDepartment() != null ? currentQuestion.getUserDepartment() : currentQuestion.getDepartmentName()));
            if (lblSemester != null) lblSemester.setText(StringUtils.nullSafe(currentQuestion.getUserSemester() != null ? currentQuestion.getUserSemester() : currentQuestion.getSemesterName()));
            if (lblSubject != null) lblSubject.setText(StringUtils.nullSafe(currentQuestion.getSubject()));
            if (lblDate != null) lblDate.setText(StringUtils.nullSafe(currentQuestion.getCreatedAt()));

            // Load answers
            loadAnswers();

        } catch (Exception e) {
            logger.severe("Failed to load question details: " + e.getMessage());
            showError("Failed to load question details: " + e.getMessage());
        }
    }

    private void loadAnswers() {
        answersContainer.getChildren().clear();

        try {
            List<Answer> answers = adminService.getAnswersForQuestion(currentQuestion.getId());
            currentQuestion.setAnswers(answers);

            if (answers.isEmpty()) {
                Label noAnswers = new Label("No answers yet.");
                noAnswers.setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic;");
                answersContainer.getChildren().add(noAnswers);
                return;
            }

            for (Answer answer : answers) {
                VBox answerCard = createAnswerCard(answer);
                answersContainer.getChildren().add(answerCard);
            }

        } catch (Exception e) {
            logger.severe("Failed to load answers: " + e.getMessage());
            showError("Failed to load answers: " + e.getMessage());
        }
    }

    private VBox createAnswerCard(Answer answer) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-border-width: 1;");
        card.setPadding(new Insets(16));

        // Header with avatar and name
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Avatar placeholder
        ImageView avatar = new ImageView();
        avatar.setFitHeight(40);
        avatar.setFitWidth(40);
        avatar.setStyle("-fx-background-radius: 20; -fx-background-color: #d1d5db;");
        // In a real app, you would load the actual user avatar here

        VBox nameBox = new VBox(4);
        Label nameLabel = new Label(StringUtils.nullSafe(answer.getAuthorName()));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label dateLabel = new Label(StringUtils.nullSafe(answer.getCreatedAt()));
        dateLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        nameBox.getChildren().addAll(nameLabel, dateLabel);
        header.getChildren().addAll(avatar, nameBox);

        // Answer text
        Label answerText = new Label(StringUtils.nullSafe(answer.getAnswerText()));
        answerText.setWrapText(true);
        answerText.setStyle("-fx-font-size: 14px;");

        // Status badge
        Label statusBadge = new Label();
        boolean isBestAnswer = currentQuestion != null && currentQuestion.getBestAnswerId() == answer.getId();
        boolean isRewarded = isBestAnswer && "REWARDED".equalsIgnoreCase(currentQuestion.getRewardStatus());

        if (isRewarded) {
            statusBadge.setText("🏆 Rewarded");
            statusBadge.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");
        } else if (isBestAnswer) {
            statusBadge.setText("⭐ Best Answer");
            statusBadge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #d97706; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");
        } else if (answer.isApproved()) {
            statusBadge.setText("✅ Approved");
            statusBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");
        } else {
            statusBadge.setText("Pending");
            statusBadge.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-padding: 4 8; -fx-background-radius: 4;");
        }

        // Buttons container
        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        // Approve button for answers that are not approved yet
        if (!answer.isApproved()) {
            Button approveBtn = new Button("✅ Approve Answer");
            approveBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold;");
            approveBtn.setOnAction(e -> {
                boolean ok = questionService.approveAnswer(answer.getId());
                if (ok) {
                    loadQuestionDetails(currentQuestion.getId());
                    showSuccess("Answer approved successfully!");
                } else {
                    showError("Failed to approve answer.");
                }
            });
            actionButtons.getChildren().add(approveBtn);
        }

        // Action button (Reward)
        Button actionButton = new Button();
        boolean canReward = canRewardAnswer() && answer.isApproved() && 
                            (currentQuestion.getBestAnswerId() <= 0 || currentQuestion.getBestAnswerId() == answer.getId());
        if (answer.isRewarded() || !canReward) {
            actionButton.setText("Reward Sent");
            actionButton.setDisable(true);
            actionButton.setStyle("-fx-background-color: #d1d5db; -fx-text-fill: #6b7280;");
        } else {
            actionButton.setText("Select as Best Answer");
            actionButton.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold;");
            actionButton.setOnAction(e -> handleSelectBestAnswer(answer));
        }
        actionButtons.getChildren().add(actionButton);

        HBox footer = new HBox(10);
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.getChildren().addAll(statusBadge, actionButtons);

        card.getChildren().addAll(header, answerText, footer);

        return card;
    }

    private boolean canRewardAnswer() {
        if (currentQuestion == null) return false;

        try {
            boolean isApproved = questionService.isQuestionApproved(currentQuestion.getId());
            String rewardStatus = currentQuestion.getRewardStatus() != null
                    ? currentQuestion.getRewardStatus() : "PENDING";

            // Reward is only possible when approved and not already finalised
            boolean alreadyRewarded =
                    "REWARDED".equalsIgnoreCase(rewardStatus) ||
                    "TRANSFERRED".equalsIgnoreCase(rewardStatus) ||
                    "ACCEPTED".equalsIgnoreCase(rewardStatus);

            return isApproved && !alreadyRewarded;
        } catch (Exception e) {
            logger.warning("Failed to check reward eligibility: " + e.getMessage());
            return false;
        }
    }

    private void handleSelectBestAnswer(Answer answer) {
        if (currentQuestion == null) return;

        boolean confirmed = showConfirmationDialog(answer);

        if (confirmed) {
            try {
                boolean success = questionService.markBestAnswer(currentQuestion.getId(), answer.getId());

                if (success) {
                    // Send notification to the student (best-effort, fire-and-forget)
                    sendRewardNotification(answer);

                    // Reload the dialog so reward status label and button state update immediately
                    loadQuestionDetails(currentQuestion.getId());

                    // Events already published by QuestionDAO.markBestAnswer(); broadcast once more
                    // to ensure the questions table in the parent window also refreshes
                    EventBus.getInstance().publish(new EventBus.QuestionsChangedEvent());
                    EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());

                    showSuccessDialog(answer);
                } else {
                    showError("Failed to transfer reward. The question may already have been rewarded, " +
                              "or the question must be approved first.");
                }
            } catch (Exception e) {
                logger.severe("Failed to mark best answer: " + e.getMessage());
                showError("Failed to transfer reward: " + e.getMessage());
            }
        }
    }

    private boolean showConfirmationDialog(Answer answer) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirm Best Answer Selection");
        dialog.setHeaderText("🏆 Select Best Answer & Transfer Reward Points");

        if (stage != null) {
            dialog.initOwner(stage);
        }

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Question details
        Label questionLabel = new Label("Question Details:");
        questionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        javafx.scene.layout.GridPane questionGrid = new javafx.scene.layout.GridPane();
        questionGrid.setHgap(10);
        questionGrid.setVgap(5);

        String questionTitle = currentQuestion.getTitle() != null ? currentQuestion.getTitle() : currentQuestion.getQuestionText();
        questionGrid.add(new Label("Title:"), 0, 0);
        questionGrid.add(new Label(questionTitle), 1, 0);
        questionGrid.add(new Label("Author:"), 0, 1);
        questionGrid.add(new Label(StringUtils.nullSafe(currentQuestion.getAuthorName())), 1, 1);
        questionGrid.add(new Label("Approval Status:"), 0, 2);
        Label approvalLabel = new Label("✅ Approved");
        approvalLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
        questionGrid.add(approvalLabel, 1, 2);

        // Answer details
        Label answerLabel = new Label("Answer Details:");
        answerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        javafx.scene.layout.GridPane answerGrid = new javafx.scene.layout.GridPane();
        answerGrid.setHgap(10);
        answerGrid.setVgap(5);

        answerGrid.add(new Label("Answer Author:"), 0, 0);
        answerGrid.add(new Label(StringUtils.nullSafe(answer.getAuthorName())), 1, 0);
        answerGrid.add(new Label("Reward Points:"), 0, 1);
        Label pointsLabel = new Label(String.valueOf(currentQuestion.getRewardPoints()));
        pointsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #eab308;");
        answerGrid.add(pointsLabel, 1, 1);
        answerGrid.add(new Label("Current Status:"), 0, 2);
        answerGrid.add(new Label("Pending"), 1, 2);

        // Warning and explanation of actions
        Label warningLabel = new Label("This action will:\n" +
                "• Mark this answer as the Best Answer\n" +
                "• Transfer the reward points immediately\n" +
                "• Notify the student\n" +
                "• Update achievements and refresh dashboards");
        warningLabel.setStyle("-fx-text-fill: #475569; -fx-wrap-text: true; -fx-font-size: 13px;");
        warningLabel.setMaxWidth(400);

        Label permanentLabel = new Label("⚠️ Note: Reward transfer is permanent and cannot be undone.");
        permanentLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-wrap-text: true;");
        permanentLabel.setMaxWidth(400);

        content.getChildren().addAll(questionLabel, questionGrid, answerLabel, answerGrid, warningLabel, permanentLabel);

        dialog.getDialogPane().setContent(content);

        ButtonType confirmButtonType = new ButtonType("Transfer Reward", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(cancelButtonType, confirmButtonType);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold;");

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == confirmButtonType;
    }

    private void showSuccessDialog(Answer answer) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reward Successfully Sent");
        dialog.setHeaderText("✅ Reward Successfully Sent");

        if (stage != null) {
            dialog.initOwner(stage);
        }

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(javafx.geometry.Pos.CENTER);

        Label pointsLabel = new Label(currentQuestion.getRewardPoints() + " Achievement Points transferred");
        pointsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #22c55e;");

        Label flowLabel = new Label(
            StringUtils.nullSafe(currentQuestion.getAuthorName()) + 
            "\n   ↓   \n" + 
            StringUtils.nullSafe(answer.getAuthorName())
        );
        flowLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-alignment: center; -fx-text-alignment: center;");

        content.getChildren().addAll(pointsLabel, flowLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    private void sendRewardNotification(Answer answer) {
        try {
            NotificationService notificationService = NotificationService.getInstance();
            String notificationTitle = "🏆 Best Answer Selected";
            String notificationMessage = String.format(
                "Congratulations! You earned %d Achievement Points. Your answer was selected as the Best Answer for the question: %s",
                currentQuestion.getRewardPoints(),
                currentQuestion.getTitle() != null ? currentQuestion.getTitle() : currentQuestion.getQuestionText()
            );

            // Look up the answer author's email so NotificationDAO can resolve the user
            // (NotificationDAO resolveUserIds for type=USER queries: WHERE email = ?)
            String recipientEmail = null;
            try {
                com.studybuddy.models.User author = adminService.getUserById(answer.getUserId());
                if (author != null) recipientEmail = author.getEmail();
            } catch (Exception ex) {
                logger.warning("Could not look up answer author email: " + ex.getMessage());
            }

            if (recipientEmail == null || recipientEmail.isBlank()) {
                logger.warning("Skipping notification: answer author email not found for userId=" + answer.getUserId());
                return;
            }

            Notification notification = new Notification();
            notification.setTitle(notificationTitle);
            notification.setMessage(notificationMessage);
            notification.setRecipientType("USER");
            notification.setRecipientValue(recipientEmail);
            notification.setPriority("HIGH");
            notification.setNotificationType("Achievement");

            notificationService.sendSmartNotification(notification);
        } catch (Exception e) {
            logger.warning("Failed to send reward notification: " + e.getMessage());
        }
    }

    @FXML
    public void handleApproveQuestion() {
        if (currentQuestion == null) return;

        // Check if already approved
        try {
            boolean alreadyApproved = questionService.isQuestionApproved(currentQuestion.getId());
            if (alreadyApproved) {
                showSuccess("This question is already approved.");
                return;
            }
        } catch (Exception e) {
            logger.warning("Could not check approval status: " + e.getMessage());
        }

        // Confirm with admin
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Approve this question? This will enable the Reward button for eligible answers.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Question Approval");
        if (stage != null) confirm.initOwner(stage);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        try {
            boolean ok = questionService.approveQuestion(currentQuestion.getId());
            if (ok) {
                // Immediately reload fresh data from DB so labels and buttons update
                loadQuestionDetails(currentQuestion.getId());
                // Disable approve button since question is now approved
                if (btnApproveQuestion != null) {
                    btnApproveQuestion.setDisable(true);
                    btnApproveQuestion.setText("✅ Approved");
                }
                showSuccess("Question approved! The Reward button is now enabled.");
            } else {
                showError("Failed to approve question. Check server logs.");
            }
        } catch (Exception e) {
            logger.severe("Failed to approve question: " + e.getMessage());
            showError("Error approving question: " + e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        if (stage != null) {
            stage.close();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
