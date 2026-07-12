package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Answer;
import com.studybuddy.models.Department;
import com.studybuddy.models.Question;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.models.User;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.AuthorizationService;
import com.studybuddy.services.QuestionService;
import com.studybuddy.utils.AcademicFilterHelper;
import com.studybuddy.utils.EventBus;
import com.studybuddy.utils.StringUtils;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for QuestionsView.fxml.
 * <p>
 * All data access goes through QuestionService → QuestionDAO → SQL Server.
 * No raw SQL is written inside this controller (MVC compliance).
 * <p>
 * Fixes applied:
 * - loadQuestions() now calls questionDAO.getAllQuestions() instead of inline SQL.
 * - subject filter populated from SubjectDAO (via QuestionService) — no hardcoded list.
 * - NPE guards on subject, tags, questionText.
 * - Answer authorship check uses userId (not display name).
 */
public class QuestionsController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private StackPane stackContainer;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Department> departmentFilter;
    @FXML
    private ComboBox<Semester> semesterFilter;
    @FXML
    private ComboBox<Subject> subjectFilter;
    @FXML
    private ListView<Question> questionsListView;

    // View Panel Wrappers
    @FXML
    private VBox feedPane;
    @FXML
    private ScrollPane detailPane;

    // Details Fields
    @FXML
    private Label qSubject;
    @FXML
    private Label qTags;
    @FXML
    private Label qDate;
    @FXML
    private Label qText;
    @FXML
    private Label qAuthor;
    @FXML
    private Label qVotes;
    @FXML
    private Label qViews;
    @FXML
    private Button btnUpvoteQuestion;
    @FXML
    private VBox answersContainer;
    @FXML
    private TextArea answerInput;
    @FXML
    private VBox relatedQuestionsContainer;

    @FXML
    private Button btnDeleteQuestion;
    @FXML
    private Button btnBestAnswer;

    private final QuestionService questionService = new QuestionService();
    private final com.studybuddy.dao.QuestionDAO questionDAO = new com.studybuddy.dao.QuestionDAO();
    private final AcademicService academicService = AcademicService.getInstance();
    private final AuthorizationService authService = AuthorizationService.getInstance();
    private User currentUser;
    private List<Question> allQuestions = new ArrayList<>();
    private Question selectedQuestion;

    @FXML
    public void initialize() {
        currentUser = App.getCurrentUser();
        AcademicFilterHelper.setupFilterBar(academicService, departmentFilter, semesterFilter, subjectFilter);

        questionsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedQuestion = newVal;
                displayQuestionDetails(newVal);
                showDetailPane();
            }
        });

        loadQuestions();
        EventBus.getInstance().subscribe(EventBus.QuestionsChangedEvent.class, (_event) -> loadQuestions());
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, (_event) -> loadQuestions());
    }

    private void loadQuestions() {
        try {
            allQuestions = questionDAO.getAllQuestions();
            allQuestions = filterVisibleQuestions(allQuestions);
        } catch (Exception e) {
            allQuestions = new ArrayList<>();
            System.err.println("[QuestionsController] Failed to load questions: " + e.getMessage());
        }
        questionsListView.setItems(FXCollections.observableArrayList(allQuestions));
    }

    private List<Question> filterVisibleQuestions(List<Question> questions) {
        List<Subject> allSubjects = academicService.getAllActiveSubjects();
        Map<Integer, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (a, b) -> a));
        return questions.stream()
                .filter(q -> AcademicFilterHelper.isVisibleToUser(
                        q.getDepartmentId() > 0 ? q.getDepartmentId() : null,
                        q.getSemesterId() > 0 ? q.getSemesterId() : null,
                        q.getSubjectId() > 0 ? q.getSubjectId() : null,
                        subjectMap, currentUser, academicService))
                .collect(Collectors.toList());
    }

    // =========================
    // DISPLAY QUESTION DETAILS
    // =========================

    private void displayQuestionDetails(Question q) {
        // NPE-safe: subject, tags, createdAt may be null in legacy rows
        qSubject.setText(StringUtils.nullSafe(q.getSubject()).toUpperCase());
        qTags.setText(StringUtils.nullSafe(q.getTags()));
        qDate.setText(StringUtils.nullSafe(q.getCreatedAt()));
        qText.setText(StringUtils.nullSafe(q.getQuestionText()));

        StringBuilder authorInfo = new StringBuilder();
        authorInfo.append("Asked by: ");
        if (q.getUserFullName() != null && !q.getUserFullName().isEmpty()) {
            authorInfo.append(q.getUserFullName());
        } else {
            authorInfo.append(StringUtils.nullSafe(q.getAuthorName()));
        }
        if (q.getUserDepartment() != null || q.getUserSemester() != null) {
            authorInfo.append(" (");
            if (q.getUserDepartment() != null) {
                authorInfo.append(q.getUserDepartment());
            }
            if (q.getUserSemester() != null) {
                if (q.getUserDepartment() != null) authorInfo.append(" • ");
                authorInfo.append(q.getUserSemester());
            }
            authorInfo.append(")");
        }
        authorInfo.append(" (" + q.getRewardPoints() + " pts bounty)");
        qAuthor.setText(authorInfo.toString());

        qVotes.setText("👍 " + q.getVotes() + " votes");
        qViews.setText("👁️ " + q.getViews() + " views");

        if (btnDeleteQuestion != null) {
            boolean canDelete = authService.canDeleteQuestion(currentUser, q);
            btnDeleteQuestion.setVisible(canDelete);
            btnDeleteQuestion.setManaged(canDelete);
        }

        // Show "Mark as Best Answer" button only for question author
        // when the question has a reward and no best answer yet (Requirements 3.1, 3.2)
        if (btnBestAnswer != null) {
            boolean isQuestionAuthor = currentUser != null && currentUser.getId() == q.getUserId();
            boolean hasNoBestAnswer = !q.hasBestAnswer();
            boolean hasAnswers = !q.getAnswers().isEmpty();
            btnBestAnswer.setVisible(isQuestionAuthor && hasNoBestAnswer && hasAnswers);
            btnBestAnswer.setManaged(isQuestionAuthor && hasNoBestAnswer && hasAnswers);
        }

        if (q.isLocked()) {
            btnUpvoteQuestion.setDisable(true);
            answerInput.setDisable(true);
            answerInput.setPromptText("🔒 Discussion is locked by administration.");
        } else {
            btnUpvoteQuestion.setDisable(false);
            answerInput.setDisable(false);
            answerInput.setPromptText("Provide a clear, helpful solution...");
        }

        refreshAnswers(q);
        loadRelatedQuestions(q);
    }

    // =========================
    // ANSWERS
    // =========================

    /**
     * Reloads answers from DB and refreshes the UI panel.
     */
    private void reloadAnswersFromDb(Question q) {
        try {
            List<Answer> fresh = questionDAO.getAnswersByQuestionId(q.getId());
            q.getAnswers().clear();
            q.getAnswers().addAll(fresh);
        } catch (Exception e) {
            System.err.println("[QuestionsController] Failed to reload answers: " + e.getMessage());
        }
        refreshAnswers(q);
    }

    private void refreshAnswers(Question q) {
        answersContainer.getChildren().clear();

        if (q.getAnswers().isEmpty()) {
            Label noAnsLabel = new Label("No answers posted yet. Be the first to reply!");
            noAnsLabel.getStyleClass().add("hint-text");
            answersContainer.getChildren().add(noAnsLabel);
            return;
        }

        int currentUserId = App.getCurrentUser() != null ? App.getCurrentUser().getId() : -1;

        for (Answer ans : q.getAnswers()) {
            VBox ansCard = new VBox(8);
            ansCard.setPadding(new Insets(12));
            ansCard.getStyleClass().add("answer-card");

            // Check if this answer is the best answer (Requirement 3.1, 3.2)
            boolean isBestAnswer = ans.getId() == q.getBestAnswerId();

            HBox header = new HBox(10);
            Label author = new Label(StringUtils.nullSafe(ans.getAuthorName()));
            author.getStyleClass().add("answer-author");
            Label date = new Label(StringUtils.nullSafe(ans.getCreatedAt()));
            date.getStyleClass().add("answer-date");

            // Add "Best Answer" badge for rewarded answers (Requirement 3.2)
            if (isBestAnswer) {
                Label bestBadge = new Label("⭐ BEST ANSWER");
                bestBadge.getStyleClass().add("best-answer-badge");
                header.getChildren().addAll(author, date, bestBadge);
            } else {
                header.getChildren().addAll(author, date);
            }

            Label text = new Label(StringUtils.nullSafe(ans.getAnswerText()));
            text.setWrapText(true);
            text.getStyleClass().add("answer-body");

            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_RIGHT);

            Label votes = new Label("👍 " + ans.getVotes() + " votes");
            votes.getStyleClass().add("answer-votes");

            Button upvoteBtn = new Button("👍 Upvote");
            upvoteBtn.getStyleClass().add("btn-action-upvote");
            upvoteBtn.setOnAction(e -> {
                try {
                    int userId = App.getCurrentUser().getId();

                    boolean ok = questionDAO.updateAnswerVotes(ans.getId(), userId);
                    if (ok) {
                        ans.setVotes(ans.getVotes() + 1);
                        votes.setText("👍 " + ans.getVotes() + " votes");
                    }
                } catch (Exception ex) {
                    showError("Failed to upvote: " + ex.getMessage());
                }
            });

            footer.getChildren().addAll(votes, upvoteBtn);

            // FIXED: authorship check uses userId, NOT display name
            // This prevents any user from seeing edit/delete buttons on another
            // user's answer simply because they share the same display name.
            if (ans.getUserId() > 0 && ans.getUserId() == currentUserId) {
                Button deleteBtn = new Button("🗑 Delete");
                deleteBtn.getStyleClass().add("btn-action-delete");
                deleteBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete this answer?", ButtonType.YES, ButtonType.NO);
                    confirm.setHeaderText(null);
                    if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        try {
                            boolean ok = questionDAO.deleteAnswer(ans.getId(), currentUserId);
                            if (ok) {
                                reloadAnswersFromDb(q);
                            } else {
                                showError("Could not delete answer.");
                            }
                        } catch (Exception ex) {
                            showError("Delete failed: " + ex.getMessage());
                        }
                    }
                });

                Button editBtn = new Button("✏️ Edit");
                editBtn.getStyleClass().add("btn-action-edit");
                editBtn.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog(ans.getAnswerText());
                    dialog.setTitle("Edit Answer");
                    dialog.setHeaderText("Modify your reply:");
                    dialog.showAndWait().ifPresent(newText -> {
                        if (newText == null || newText.trim().isEmpty()) return;
                        try {
                            boolean ok = questionDAO.updateAnswerText(ans.getId(), currentUserId, newText.trim());
                            if (ok) {
                                reloadAnswersFromDb(q);
                            } else {
                                showError("Could not edit answer.");
                            }
                        } catch (Exception ex) {
                            showError("Edit failed: " + ex.getMessage());
                        }
                    });
                });

                footer.getChildren().addAll(editBtn, deleteBtn);
            }

            ansCard.getChildren().addAll(header, text, footer);
            answersContainer.getChildren().add(ansCard);
        }
    }

    // =========================
    // RELATED QUESTIONS
    // =========================

    private void loadRelatedQuestions(Question current) {
        relatedQuestionsContainer.getChildren().clear();

        String currentSubject = StringUtils.nullSafe(current.getSubject());

        List<Question> related = allQuestions.stream()
                .filter(q -> q.getId() != current.getId()
                        && !StringUtils.nullSafe(q.getSubject()).isEmpty()
                        && StringUtils.nullSafe(q.getSubject()).equalsIgnoreCase(currentSubject))
                .limit(3)
                .collect(Collectors.toList());

        if (related.isEmpty()) {
            Label label = new Label("No related questions found.");
            label.getStyleClass().add("hint-text-italic");
            relatedQuestionsContainer.getChildren().add(label);
            return;
        }

        for (Question q : related) {
            Hyperlink link = new Hyperlink("[" + StringUtils.nullSafe(q.getSubject()).toUpperCase() + "] " + StringUtils.nullSafe(q.getQuestionText()));
            link.getStyleClass().add("link-label");
            link.setOnAction(e -> {
                selectedQuestion = q;
                displayQuestionDetails(q);
            });
            relatedQuestionsContainer.getChildren().add(link);
        }
    }

    // =========================
    // NAVIGATION
    // =========================

    @FXML
    public void handleBackToFeed() {
        showFeedPane();
        questionsListView.getSelectionModel().clearSelection();
    }

    private void showFeedPane() {
        feedPane.setVisible(true);
        feedPane.setManaged(true);
        detailPane.setVisible(false);
        detailPane.setManaged(false);
    }

    private void showDetailPane() {
        feedPane.setVisible(false);
        feedPane.setManaged(false);
        detailPane.setVisible(true);
        detailPane.setManaged(true);
    }

    // =========================
    // FILTERS
    // =========================

    @FXML
    public void handleDeleteQuestion() {
        if (selectedQuestion == null || currentUser == null) return;
        if (!authService.canDeleteQuestion(currentUser, selectedQuestion)) {
            showError("You can only delete your own questions.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this question permanently?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            questionService.deleteQuestion(selectedQuestion.getId(), currentUser);
            EventBus.getInstance().publish(new EventBus.QuestionsChangedEvent());
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
            handleBackToFeed();
            loadQuestions();
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Question deleted successfully.");
            ok.setHeaderText(null);
            ok.showAndWait();
        } catch (Exception e) {
            showError("Delete failed: " + e.getMessage());
        }
    }

    @FXML
    public void applyFilters() {
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        Department dept = departmentFilter != null ? departmentFilter.getValue() : null;
        Semester sem = semesterFilter != null ? semesterFilter.getValue() : null;
        Subject selectedSubject = subjectFilter != null ? subjectFilter.getValue() : null;
        String subject = selectedSubject != null ? selectedSubject.getName() : null;

        List<Subject> allSubjects = academicService.getAllActiveSubjects();
        Map<Integer, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (s1, unused) -> s1));

        List<Question> filtered = allQuestions.stream()
                .filter(q -> (query.isEmpty()
                        || StringUtils.nullSafe(q.getQuestionText()).toLowerCase().contains(query)
                        || StringUtils.nullSafe(q.getTags()).toLowerCase().contains(query))
                        && (subject == null || StringUtils.nullSafe(q.getSubject()).equalsIgnoreCase(subject)))
                .filter(q -> AcademicFilterHelper.matchesDeptSemFilter(
                        q.getDepartmentId() > 0 ? q.getDepartmentId() : null,
                        q.getSemesterId() > 0 ? q.getSemesterId() : null,
                        q.getSubjectId(),
                        dept, sem, subjectMap, allSubjects, StringUtils.nullSafe(q.getSubject())))
                .collect(Collectors.toList());

        questionsListView.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        AcademicFilterHelper.resetFilters(academicService, departmentFilter, semesterFilter, subjectFilter);
        questionsListView.setItems(FXCollections.observableArrayList(allQuestions));
    }

    // =========================
    // ACTIONS
    // =========================

    @FXML
    public void handleAskQuestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/studybuddy/fxml/AskQuestionView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ask a Question");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadQuestions(); // Refresh after new question is submitted
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleUpvoteQuestion() {
        if (selectedQuestion == null) {
            return;
        }

        try {
            int userId = App.getCurrentUser().getId();

            System.out.println("Current User ID = " + userId);

            boolean ok = questionDAO.updateQuestionVotes(
                    selectedQuestion.getId(),
                    userId
            );

            if (ok) {
                selectedQuestion.setVotes(selectedQuestion.getVotes() + 1);
                qVotes.setText("👍 " + selectedQuestion.getVotes() + " votes");
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Vote");
                alert.setHeaderText(null);
                alert.setContentText("You have already voted for this question.");
                alert.showAndWait();
            }

        } catch (Exception e) {
            showError("Failed to upvote question: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSubmitAnswer() {
        if (selectedQuestion == null) return;

        if (answerInput == null) return;
        String text = answerInput.getText();
        if (text == null || text.trim().isEmpty()) {
            Alert warn = new Alert(Alert.AlertType.WARNING, "Answer cannot be empty.");
            warn.setHeaderText(null);
            warn.showAndWait();
            return;
        }

        if (App.getCurrentUser() == null) {
            Alert warn = new Alert(Alert.AlertType.WARNING, "Please log in to post an answer.");
            warn.setHeaderText(null);
            warn.showAndWait();
            return;
        }

        int userId = App.getCurrentUser().getId();
        String author = App.getCurrentUser().getName();

        try {
            boolean saved = questionDAO.submitAnswer(
                    selectedQuestion.getId(),
                    userId,
                    author,
                    text.trim()
            );

            if (!saved) {
                showError("Your answer could not be saved. Please try again.");
                return;
            }

            answerInput.clear();
            reloadAnswersFromDb(selectedQuestion);
            
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());

            Alert success = new Alert(Alert.AlertType.INFORMATION,
                    "Your answer has been posted successfully.");
            success.setHeaderText(null);
            success.showAndWait();

        } catch (Exception e) {
            showError("Failed to post answer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================
    // HELPERS
    // =========================


    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // =========================
    // MARK AS BEST ANSWER (Requirements 3.1, 3.2, 3.3)
    // =========================

    /**
     * Shows a dialog to select which answer to mark as best answer.
     * This is called when the user clicks "Mark as Best Answer" button.
     */
    @FXML
    public void handleMarkBestAnswer() {
        if (selectedQuestion == null) return;

        // Check if already has best answer
        if (selectedQuestion.hasBestAnswer()) {
            showError("This question already has a best answer marked.");
            return;
        }

        // Get list of answers to display in dialog
        List<Answer> answers = selectedQuestion.getAnswers();
        if (answers.isEmpty()) {
            showError("There are no answers to mark as best.");
            return;
        }

        // Filter out own answers (can't mark own answer as best)
        List<Answer> validAnswers = answers.stream()
                .filter(ans -> ans.getUserId() != currentUser.getId())
                .toList();

        if (validAnswers.isEmpty()) {
            showError("You can only mark answers from other users as best answer.");
            return;
        }

        // Create a dialog to select the best answer
        ChoiceDialog<Answer> dialog = new ChoiceDialog<>(validAnswers.get(0), validAnswers);
        dialog.setTitle("Mark as Best Answer");
        dialog.setHeaderText("Select the answer that best solves your question:");
        dialog.setContentText("Choose an answer:");

        // Customize the display of items in the dialog (ChoiceDialog uses an internal ComboBox)
        @SuppressWarnings("unchecked")
        ComboBox<Answer> comboBox = (ComboBox<Answer>) dialog.getDialogPane().lookup(".combo-box");
        if (comboBox != null) {
            comboBox.setConverter(new javafx.util.StringConverter<Answer>() {
                @Override
                public String toString(Answer answer) {
                    if (answer == null) {
                        return "";
                    }
                    String text = answer.getAnswerText();
                    String preview = text != null && text.length() > 60 ? text.substring(0, 57) + "..." : text;
                    return String.format("[%s] %s (%d votes)",
                            StringUtils.nullSafe(answer.getAuthorName()),
                            StringUtils.nullSafe(preview),
                            answer.getVotes());
                }

                @Override
                public Answer fromString(String string) {
                    return null;
                }
            });
        }

        Optional<Answer> result = dialog.showAndWait();

        result.ifPresent(selectedAnswer -> {
            // Mark the selected answer as best
            int rewardPoints = selectedQuestion.getRewardPoints();
            String message = "Are you sure you want to mark this answer as the best answer?\n\n";
            if (rewardPoints > 0) {
                message += "This will transfer " + rewardPoints + " achievement points to the answer author.";
            } else {
                message += "No points will be transferred as this question has no reward points.";
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirm Best Answer");
            confirm.setHeaderText(null);
            confirm.showAndWait();

            if (confirm.getResult() == ButtonType.YES) {
                try {
                    boolean success = questionDAO.markBestAnswerAndTransferPoints(
                            selectedQuestion.getId(),
                            selectedAnswer.getId(),
                            currentUser.getId()
                    );

                    if (success) {
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION,
                                "Answer marked as best! " + (rewardPoints > 0 ? rewardPoints + " points transferred to the answer author." : ""));
                        successAlert.setTitle("Success");
                        successAlert.setHeaderText(null);
                        successAlert.showAndWait();

                        // Refresh
                        reloadAnswersFromDb(selectedQuestion);
                        displayQuestionDetails(selectedQuestion);
                    } else {
                        showError("Failed to mark answer as best.");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
}
