package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Answer;
import com.studybuddy.models.Question;
import com.studybuddy.services.QuestionService;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.studybuddy.utils.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
public class QuestionsController {

    @FXML private TextField searchField;
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
    private List<Question> allQuestions = new ArrayList<>();
    private Question selectedQuestion;

    @FXML
    public void initialize() {
        // Populate subject filter
        List<String> subjects = questionService.getAvailableSubjects();
        subjectFilter.setItems(FXCollections.observableArrayList(subjects));

        // Selection listener: navigate directly to detail view
        questionsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedQuestion = newVal;
                displayQuestionDetails(newVal);
                showDetailPane();
            }
        });

        loadQuestions();
    }

    private void loadQuestions() {

        allQuestions = new ArrayList<>();

        String sql = """
            SELECT *
            FROM Questions
            ORDER BY created_at DESC
            """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Question q = new Question(
                        rs.getInt("question_id"),
                        rs.getInt("user_id"),
                        rs.getString("author_name"),
                        rs.getString("subject"),
                        rs.getString("question_text"),
                        rs.getString("tags"),
                        rs.getString("attachment_path"),
                        rs.getInt("reward_points"),
                        rs.getInt("votes"),
                        rs.getInt("views"),
                        rs.getString("created_at"),
                        rs.getBoolean("is_locked")
                );

                allQuestions.add(q);
            }

            questionsListView.setItems(
                    FXCollections.observableArrayList(allQuestions)
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayQuestionDetails(Question q) {
        qSubject.setText(q.getSubject().toUpperCase());
        qTags.setText(q.getTags());
        qDate.setText(q.getCreatedAt());
        qText.setText(q.getQuestionText());
        qAuthor.setText("Asked by: " + q.getAuthorName() + " (" + q.getRewardPoints() + " pts bounty)");
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

    private void refreshAnswers(Question q) {
        answersContainer.getChildren().clear();

        if (q.getAnswers().isEmpty()) {
            Label noAnsLabel = new Label("No answers posted yet. Be the first to reply!");
            noAnsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #64748b; -fx-padding: 10;");
            answersContainer.getChildren().add(noAnsLabel);
            return;
        }

        for (Answer ans : q.getAnswers()) {
            VBox ansCard = new VBox(8);
            ansCard.setPadding(new Insets(12));
            ansCard.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 10;");

            HBox header = new HBox(10);
            Label author = new Label(ans.getAuthorName());
            author.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f2937; -fx-font-size: 13px;");
            Label date = new Label(ans.getCreatedAt());
            date.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            header.getChildren().addAll(author, date);

            Label text = new Label(ans.getAnswerText());
            text.setWrapText(true);
            text.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");

            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_RIGHT);

            Label votes = new Label("👍 " + ans.getVotes() + " votes");
            votes.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            Button upvoteBtn = new Button("👍 Upvote");
            upvoteBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #4f46e5; -fx-padding: 2 8; -fx-font-size: 11px; -fx-cursor: hand;");
            upvoteBtn.setOnAction(e -> {
                ans.setVotes(ans.getVotes() + 1);
                votes.setText("👍 " + ans.getVotes() + " votes");
            });

            footer.getChildren().addAll(votes, upvoteBtn);

            String currentUserName = App.getCurrentUser() != null ? App.getCurrentUser().getName() : "Guest Student";
            if (ans.getAuthorName().equals(currentUserName)) {
                Button deleteBtn = new Button("🗑️ Delete");
                deleteBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-padding: 2 8; -fx-font-size: 11px; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    q.getAnswers().remove(ans);
                    refreshAnswers(q);
                });

                Button editBtn = new Button("✏️ Edit");
                editBtn.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #22c55e; -fx-padding: 2 8; -fx-font-size: 11px; -fx-cursor: hand;");
                editBtn.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog(ans.getAnswerText());
                    dialog.setTitle("Edit Answer");
                    dialog.setHeaderText("Modify your reply:");
                    dialog.showAndWait().ifPresent(newText -> {
                        if (!newText.trim().isEmpty()) {
                            ans.setAnswerText(newText.trim());
                            refreshAnswers(q);
                        }
                    });
                });

                footer.getChildren().addAll(editBtn, deleteBtn);
            }

            ansCard.getChildren().addAll(header, text, footer);
            answersContainer.getChildren().add(ansCard);
        }
    }

    private void loadRelatedQuestions(Question current) {
        relatedQuestionsContainer.getChildren().clear();

        List<Question> related = allQuestions.stream()
                .filter(q -> q.getId() != current.getId() &&
                             (q.getSubject().equalsIgnoreCase(current.getSubject())))
                .limit(3)
                .collect(Collectors.toList());

        if (related.isEmpty()) {
            Label label = new Label("No related questions found.");
            label.setStyle("-fx-font-style: italic; -fx-text-fill: #64748b; -fx-font-size: 12px;");
            relatedQuestionsContainer.getChildren().add(label);
            return;
        }

        for (Question q : related) {
            Hyperlink link = new Hyperlink("[" + q.getSubject().toUpperCase() + "] " + q.getQuestionText());
            link.setStyle("-fx-font-size: 13px; -fx-text-fill: #3b82f6;");
            link.setOnAction(e -> {
                selectedQuestion = q;
                displayQuestionDetails(q);
            });
            relatedQuestionsContainer.getChildren().add(link);
        }
    }

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

    @FXML
    public void applyFilters() {
        String query = searchField.getText().trim().toLowerCase();
        String subject = subjectFilter.getValue();

        List<Question> filtered = allQuestions.stream()
                .filter(q -> (query.isEmpty() || q.getQuestionText().toLowerCase().contains(query) || q.getTags().toLowerCase().contains(query)) &&
                             (subject == null || q.getSubject().equalsIgnoreCase(subject)))
                .collect(Collectors.toList());

        questionsListView.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        subjectFilter.getSelectionModel().clearSelection();
        questionsListView.setItems(FXCollections.observableArrayList(allQuestions));
    }

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
            
            loadQuestions();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleUpvoteQuestion() {
        if (selectedQuestion != null) {
            selectedQuestion.setVotes(selectedQuestion.getVotes() + 1);
            qVotes.setText("👍 " + selectedQuestion.getVotes() + " votes");
            System.out.println("⚠️ handleUpvoteQuestion() stub - SQL pending");
        }
    }

    @FXML
    public void handleSubmitAnswer() {
        if (selectedQuestion == null) return;
        String text = answerInput.getText().trim();
        if (text.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Answer cannot be empty.");
            alert.showAndWait();
            return;
        }

        String author = App.getCurrentUser() != null ? App.getCurrentUser().getName() : "Guest Student";
        Answer newAns = new Answer(
                selectedQuestion.getAnswers().size() + 1,
                selectedQuestion.getId(),
                App.getCurrentUser() != null ? App.getCurrentUser().getId() : 1,
                author,
                text,
                0,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        selectedQuestion.getAnswers().add(newAns);
        answerInput.clear();
        refreshAnswers(selectedQuestion);

        System.out.println("⚠️ handleSubmitAnswer() stub - SQL pending");
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Your answer has been posted successfully.");
        alert.showAndWait();
    }
}
