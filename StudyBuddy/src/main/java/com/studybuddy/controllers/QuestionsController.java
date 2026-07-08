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
import java.util.stream.Collectors;

/**
 * Controller for QuestionsView.fxml.
 *
 * All data access goes through QuestionService → QuestionDAO → SQL Server.
 * No raw SQL is written inside this controller (MVC compliance).
 *
 * Fixes applied:
 *  - loadQuestions() now calls questionDAO.getAllQuestions() instead of inline SQL.
 *  - subject filter populated from SubjectDAO (via QuestionService) — no hardcoded list.
 *  - NPE guards on subject, tags, questionText.
 *  - Answer authorship check uses userId (not display name).
 */
public class QuestionsController {

    @FXML private BorderPane rootPane;
    @FXML private StackPane stackContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<Department> departmentFilter;
    @FXML private ComboBox<Semester> semesterFilter;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ListView<Question> questionsListView;

    // View Panel Wrappers
    @FXML private VBox feedPane;
    @FXML private ScrollPane detailPane;

    // Details Fields
    @FXML private Label qSubject;
    @FXML private Label qTags;
    @FXML private Label qDate;
    @FXML private Label qText;
    @FXML private Label qAuthor;
    @FXML private Label qVotes;
    @FXML private Label qViews;
    @FXML private Button btnUpvoteQuestion;
    @FXML private VBox answersContainer;
    @FXML private TextArea answerInput;
    @FXML private VBox relatedQuestionsContainer;

    @FXML private Button btnDeleteQuestion;

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
        qSubject.setText(nullSafe(q.getSubject()).toUpperCase());
        qTags.setText(nullSafe(q.getTags()));
        qDate.setText(nullSafe(q.getCreatedAt()));
        qText.setText(nullSafe(q.getQuestionText()));
        
        StringBuilder authorInfo = new StringBuilder();
        authorInfo.append("Asked by: ");
        if (q.getUserFullName() != null && !q.getUserFullName().isEmpty()) {
            authorInfo.append(q.getUserFullName());
        } else {
            authorInfo.append(nullSafe(q.getAuthorName()));
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

            HBox header = new HBox(10);
            Label author = new Label(nullSafe(ans.getAuthorName()));
            author.getStyleClass().add("answer-author");
            Label date = new Label(nullSafe(ans.getCreatedAt()));
            date.getStyleClass().add("answer-date");
            header.getChildren().addAll(author, date);

            Label text = new Label(nullSafe(ans.getAnswerText()));
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
                    boolean ok = questionDAO.updateAnswerVotes(ans.getId(), 1);
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

        String currentSubject = nullSafe(current.getSubject());

        List<Question> related = allQuestions.stream()
                .filter(q -> q.getId() != current.getId()
                        && !nullSafe(q.getSubject()).isEmpty()
                        && nullSafe(q.getSubject()).equalsIgnoreCase(currentSubject))
                .limit(3)
                .collect(Collectors.toList());

        if (related.isEmpty()) {
            Label label = new Label("No related questions found.");
            label.getStyleClass().add("hint-text-italic");
            relatedQuestionsContainer.getChildren().add(label);
            return;
        }

        for (Question q : related) {
            Hyperlink link = new Hyperlink("[" + nullSafe(q.getSubject()).toUpperCase() + "] " + nullSafe(q.getQuestionText()));
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
        String query = searchField.getText().trim().toLowerCase();
        Department dept = departmentFilter.getValue();
        Semester sem = semesterFilter.getValue();
        String subject = subjectFilter.getValue();

        List<Subject> allSubjects = academicService.getAllActiveSubjects();
        Map<Integer, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (s1, unused) -> s1));

        List<Question> filtered = allQuestions.stream()
                .filter(q -> (query.isEmpty()
                        || nullSafe(q.getQuestionText()).toLowerCase().contains(query)
                        || nullSafe(q.getTags()).toLowerCase().contains(query))
                        && (subject == null || nullSafe(q.getSubject()).equalsIgnoreCase(subject)))
                .filter(q -> AcademicFilterHelper.matchesDeptSemFilter(
                        q.getDepartmentId() > 0 ? q.getDepartmentId() : null,
                        q.getSemesterId() > 0 ? q.getSemesterId() : null,
                        q.getSubjectId(),
                        dept, sem, subjectMap, allSubjects, nullSafe(q.getSubject())))
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
        if (selectedQuestion == null) return;
        try {
            boolean ok = questionDAO.updateQuestionVotes(selectedQuestion.getId(), 1);
            if (ok) {
                selectedQuestion.setVotes(selectedQuestion.getVotes() + 1);
                qVotes.setText("👍 " + selectedQuestion.getVotes() + " votes");
            } else {
                showError("Failed to upvote question.");
            }
        } catch (Exception e) {
            showError("Upvote error: " + e.getMessage());
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

        int userId    = App.getCurrentUser().getId();
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

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
