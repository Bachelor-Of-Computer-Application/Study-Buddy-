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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class QuestionsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ListView<Question> questionsListView;
    @FXML private VBox feedPane;
    @FXML private ScrollPane detailPane;
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
    private List<Question> allQuestions = Collections.emptyList();
    private Question selectedQuestion;

    @FXML
    public void initialize() {
        loadSubjects();

        questionsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedQuestion = newVal;
                displayQuestionDetails(newVal);
                showDetailPane();
            }
        });

        loadQuestions();
    }

    private void loadSubjects() {
        try {
            subjectFilter.setItems(FXCollections.observableArrayList(questionService.getAvailableSubjects()));
        } catch (Exception e) {
            showError("Failed to load subjects: " + e.getMessage());
            subjectFilter.setItems(FXCollections.emptyObservableList());
        }
    }

    private void loadQuestions() {
        try {
            allQuestions = questionService.getAllQuestions();
            questionsListView.setItems(FXCollections.observableArrayList(allQuestions));
        } catch (Exception e) {
            allQuestions = Collections.emptyList();
            questionsListView.setItems(FXCollections.emptyObservableList());
            showError("Failed to load questions: " + e.getMessage());
        }
    }

    private void displayQuestionDetails(Question q) {
        qSubject.setText(q.getSubject().toUpperCase());
        qTags.setText(q.getTags());
        qDate.setText(q.getCreatedAt());
        qText.setText(q.getQuestionText());
        qAuthor.setText("Asked by: " + q.getAuthorName() + " (" + q.getRewardPoints() + " pts bounty)");
        qVotes.setText(q.getVotes() + " votes");
        qViews.setText(q.getViews() + " views");

        btnUpvoteQuestion.setDisable(q.isLocked());
        answerInput.setDisable(q.isLocked());
        answerInput.setPromptText(q.isLocked()
                ? "Discussion is locked by administration."
                : "Provide a clear, helpful solution...");

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

            Label votes = new Label(ans.getVotes() + " votes");
            votes.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            Button upvoteBtn = new Button("Upvote");
            upvoteBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #4f46e5; -fx-padding: 2 8; -fx-font-size: 11px; -fx-cursor: hand;");
            upvoteBtn.setOnAction(e -> handleUpvoteAnswer(ans));

            footer.getChildren().addAll(votes, upvoteBtn);
            ansCard.getChildren().addAll(header, text, footer);
            answersContainer.getChildren().add(ansCard);
        }
    }

    private void loadRelatedQuestions(Question current) {
        relatedQuestionsContainer.getChildren().clear();

        try {
            List<Question> related = questionService.getRelatedQuestions(current.getId(), current.getSubject(), 3);
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
        } catch (Exception e) {
            showError("Failed to load related questions: " + e.getMessage());
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
        try {
            String query = searchField.getText().trim();
            String subject = subjectFilter.getValue();
            allQuestions = questionService.searchQuestions(query, subject);
            questionsListView.setItems(FXCollections.observableArrayList(allQuestions));
        } catch (Exception e) {
            showError("Failed to filter questions: " + e.getMessage());
        }
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        subjectFilter.getSelectionModel().clearSelection();
        loadQuestions();
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
            loadSubjects();
            loadQuestions();
        } catch (IOException e) {
            showError("Failed to open question form: " + e.getMessage());
        }
    }

    @FXML
    public void handleUpvoteQuestion() {
        if (selectedQuestion == null) {
            return;
        }

        try {
            if (questionService.upvoteQuestion(selectedQuestion.getId())) {
                selectedQuestion.setVotes(selectedQuestion.getVotes() + 1);
                qVotes.setText(selectedQuestion.getVotes() + " votes");
                showInfo("Saved to database successfully");
            }
        } catch (Exception e) {
            showError("Failed to upvote question: " + e.getMessage());
        }
    }

    private void handleUpvoteAnswer(Answer answer) {
        try {
            if (questionService.upvoteAnswer(answer.getId())) {
                answer.setVotes(answer.getVotes() + 1);
                refreshAnswers(selectedQuestion);
                showInfo("Saved to database successfully");
            }
        } catch (Exception e) {
            showError("Failed to upvote answer: " + e.getMessage());
        }
    }

    @FXML
    public void handleSubmitAnswer() {
        if (selectedQuestion == null) {
            return;
        }

        String text = answerInput.getText().trim();
        if (text.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Answer cannot be empty.").showAndWait();
            return;
        }

        try {
            if (questionService.saveAnswer(selectedQuestion.getId(), text)) {
                selectedQuestion.setAnswers(questionService.getAnswersByQuestionId(selectedQuestion.getId()));
                answerInput.clear();
                refreshAnswers(selectedQuestion);
                showInfo("Saved to database successfully");
            }
        } catch (Exception e) {
            showError("Failed to submit answer: " + e.getMessage());
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Database Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
