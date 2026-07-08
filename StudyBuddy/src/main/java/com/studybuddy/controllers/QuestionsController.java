package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Answer;
import com.studybuddy.models.Department;
import com.studybuddy.models.Question;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.QuestionService;
import com.studybuddy.utils.EventBus;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
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

    private final QuestionService questionService = new QuestionService();
    private final com.studybuddy.dao.QuestionDAO questionDAO = new com.studybuddy.dao.QuestionDAO();
    private final AcademicService academicService = AcademicService.getInstance();
    private List<Question> allQuestions = new ArrayList<>();
    private Question selectedQuestion;

    @FXML
    public void initialize() {
        // Populate subject filter from canonical Subjects table (not hardcoded)
        loadFilterDepartments();
        loadSubjects();

        // Selection listener: navigate directly to detail view
        questionsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedQuestion = newVal;
                displayQuestionDetails(newVal);
                showDetailPane();
            }
        });

        loadQuestions();
        
        // Subscribe to EventBus events
        EventBus.getInstance().subscribe(EventBus.QuestionsChangedEvent.class, (_event) -> loadQuestions());
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, (_event) -> loadQuestions());
    }
    
    private void loadFilterDepartments() {
        if (departmentFilter != null) {
            semesterFilter.setDisable(true);
            try {
                departmentFilter.setItems(FXCollections.observableArrayList(
                        academicService.getAllActiveDepartments()));
            } catch (Exception e) {
                System.err.println("[QuestionsController] Dept filter load failed: " + e.getMessage());
            }

            departmentFilter.setOnAction(e -> {
                Department dept = departmentFilter.getValue();
                if (semesterFilter != null) {
                    semesterFilter.getItems().clear();
                    semesterFilter.setValue(null);
                    semesterFilter.setDisable(dept == null);
                }
                subjectFilter.getItems().clear();
                subjectFilter.setValue(null);

                if (dept != null && semesterFilter != null) {
                    try {
                        semesterFilter.setItems(FXCollections.observableArrayList(
                                academicService.getSemestersByDepartment(dept.getId())));
                    } catch (Exception ex) {
                        System.err.println("[QuestionsController] Sem filter load failed: " + ex.getMessage());
                    }
                } else {
                    loadSubjects();
                }
            });

            if (semesterFilter != null) {
                semesterFilter.setOnAction(e -> {
                    Semester sem = semesterFilter.getValue();
                    subjectFilter.getItems().clear();
                    subjectFilter.setValue(null);
                    if (sem != null) {
                        try {
                            List<String> semSubjects = academicService
                                    .getSubjectsBySemester(sem.getId())
                                    .stream()
                                    .map(Subject::getName)
                                    .collect(java.util.stream.Collectors.toList());
                            subjectFilter.setItems(FXCollections.observableArrayList(semSubjects));
                        } catch (Exception ex) {
                            System.err.println("[QuestionsController] Sub filter load failed: " + ex.getMessage());
                        }
                    } else {
                        loadSubjects();
                    }
                });
            }
        }
    }
    
    private void loadSubjects() {
        List<String> subjects = questionService.getAvailableSubjects();
        subjectFilter.setItems(FXCollections.observableArrayList(subjects));
    }

    // =========================
    // LOAD QUESTIONS via DAO (MVC — no inline SQL)
    // =========================

    private void loadQuestions() {
        try {
            allQuestions = questionDAO.getAllQuestions();
        } catch (Exception e) {
            allQuestions = new ArrayList<>();
            System.err.println("[QuestionsController] Failed to load questions: " + e.getMessage());
            e.printStackTrace();
        }
        questionsListView.setItems(FXCollections.observableArrayList(allQuestions));
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
            noAnsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #64748b; -fx-padding: 10;");
            answersContainer.getChildren().add(noAnsLabel);
            return;
        }

        int currentUserId = App.getCurrentUser() != null ? App.getCurrentUser().getId() : -1;

        for (Answer ans : q.getAnswers()) {
            VBox ansCard = new VBox(8);
            ansCard.setPadding(new Insets(12));
            ansCard.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 10;");

            HBox header = new HBox(10);
            Label author = new Label(nullSafe(ans.getAuthorName()));
            author.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f2937; -fx-font-size: 13px;");
            Label date = new Label(nullSafe(ans.getCreatedAt()));
            date.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            header.getChildren().addAll(author, date);

            Label text = new Label(nullSafe(ans.getAnswerText()));
            text.setWrapText(true);
            text.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");

            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_RIGHT);

            Label votes = new Label("👍 " + ans.getVotes() + " votes");
            votes.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            Button upvoteBtn = new Button("👍 Upvote");
            upvoteBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #4f46e5; -fx-padding: 2 8; -fx-font-size: 11px; -fx-cursor: hand;");
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
                Button deleteBtn = new Button("🗑️ Delete");
                deleteBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-padding: 2 8; -fx-font-size: 11px; -fx-cursor: hand;");
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
                editBtn.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #22c55e; -fx-padding: 2 8; -fx-font-size: 11px; -fx-cursor: hand;");
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
            label.setStyle("-fx-font-style: italic; -fx-text-fill: #64748b; -fx-font-size: 12px;");
            relatedQuestionsContainer.getChildren().add(label);
            return;
        }

        for (Question q : related) {
            Hyperlink link = new Hyperlink("[" + nullSafe(q.getSubject()).toUpperCase() + "] " + nullSafe(q.getQuestionText()));
            link.setStyle("-fx-font-size: 13px; -fx-text-fill: #3b82f6;");
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
    public void applyFilters() {
        String query = searchField.getText().trim().toLowerCase();
        Department dept = departmentFilter.getValue();
        Semester sem = semesterFilter.getValue();
        String subject = subjectFilter.getValue();
        
        List<Subject> allSubjects = academicService.getAllActiveSubjects();
        java.util.Map<Integer, Subject> subjectMap = allSubjects.stream()
                .collect(java.util.stream.Collectors.toMap(Subject::getId, s -> s, (s1, unused) -> s1));

        List<Question> filtered = allQuestions.stream()
                .filter(q -> (query.isEmpty()
                        || nullSafe(q.getQuestionText()).toLowerCase().contains(query)
                        || nullSafe(q.getTags()).toLowerCase().contains(query))
                        && (subject == null || nullSafe(q.getSubject()).equalsIgnoreCase(subject)))
                .filter(q -> {
                    if (dept == null && sem == null) return true;
                    
                    Subject subModel = subjectMap.get(q.getSubjectId());
                    if (subModel == null) {
                        return allSubjects.stream().anyMatch(s ->
                                s.getName().equalsIgnoreCase(nullSafe(q.getSubject())) &&
                                        (dept == null || s.getDepartmentId() == dept.getId()) &&
                                        (sem == null || s.getSemesterId() == sem.getId())
                        );
                    } else {
                        if (dept != null && subModel.getDepartmentId() != dept.getId()) return false;
                        if (sem != null && subModel.getSemesterId() != sem.getId()) return false;
                        return true;
                    }
                })
                .collect(Collectors.toList());

        questionsListView.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        if (departmentFilter != null) departmentFilter.getSelectionModel().clearSelection();
        if (semesterFilter != null) {
            semesterFilter.getItems().clear();
            semesterFilter.setValue(null);
            semesterFilter.setDisable(true);
        }
        subjectFilter.getSelectionModel().clearSelection();
        loadSubjects();
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

        int userId    = App.getCurrentUser() != null ? App.getCurrentUser().getId()   : 1;
        String author = App.getCurrentUser() != null ? App.getCurrentUser().getName() : "Guest Student";

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
