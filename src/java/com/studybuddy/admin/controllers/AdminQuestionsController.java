package com.studybuddy.admin.controllers;

import com.studybuddy.models.Question;
import com.studybuddy.models.Answer;
import com.studybuddy.admin.services.AdminService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminQuestionsController {

    @FXML private TextField searchField;
    @FXML private ListView<Question> questionsListView;
    @FXML private TextArea questionDetailsArea;
    @FXML private ListView<Answer> answersListView;

    private final AdminService adminService = AdminService.getInstance();
    private ObservableList<Question> masterQuestionList = FXCollections.observableArrayList();
    private Question selectedQuestion;

    @FXML
    public void initialize() {
        // Master list selection listener
        questionsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedQuestion = newVal;
                displayQuestionDetails(newVal);
            } else {
                selectedQuestion = null;
                clearQuestionDetails();
            }
        });

        // Search filtering listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(newVal));

        loadQuestionsData();
    }

    private void loadQuestionsData() {
        masterQuestionList.setAll(adminService.getQuestions());
        questionsListView.setItems(masterQuestionList);
        if (!masterQuestionList.isEmpty()) {
            questionsListView.getSelectionModel().select(0);
        }
    }

    private void displayQuestionDetails(Question q) {
        questionDetailsArea.setText("Posted by: " + q.getAuthorName() + "\n" +
                                   "Subject: " + q.getSubject() + "\n" +
                                   "Tags: " + q.getTags() + "\n" +
                                   "Status: " + (q.isLocked() ? "🔒 Locked" : "🔓 Open") + "\n\n" +
                                   q.getQuestionText());

        answersListView.getItems().setAll(q.getAnswers());
    }

    private void clearQuestionDetails() {
        questionDetailsArea.clear();
        answersListView.getItems().clear();
    }

    private void applyFilters(String query) {
        if (query == null || query.trim().isEmpty()) {
            questionsListView.setItems(masterQuestionList);
            return;
        }

        String lowerQuery = query.toLowerCase();
        List<Question> filtered = masterQuestionList.stream()
                .filter(q -> q.getQuestionText().toLowerCase().contains(lowerQuery) || q.getSubject().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());

        questionsListView.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void handleDeleteQuestion() {
        Question selected = questionsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a question to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete selected question and all its answers?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                adminService.deleteQuestion(selected.getId());
                masterQuestionList.remove(selected);
                loadQuestionsData();
            }
        });
    }

    @FXML
    public void handleToggleLockDiscussion() {
        Question selected = questionsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a question to lock/unlock.");
            return;
        }

        adminService.toggleLockDiscussion(selected.getId());
        selected.setLocked(!selected.isLocked());
        questionsListView.refresh();
        displayQuestionDetails(selected);
    }

    @FXML
    public void handleDeleteAnswer() {
        Question q = questionsListView.getSelectionModel().getSelectedItem();
        Answer ans = answersListView.getSelectionModel().getSelectedItem();

        if (q == null || ans == null) {
            showAlert("No selection", "Please select an answer to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete the selected answer?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                adminService.deleteAnswer(ans.getId());
                q.getAnswers().remove(ans);
                displayQuestionDetails(q);
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
