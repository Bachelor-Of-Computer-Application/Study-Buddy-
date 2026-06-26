package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.models.Answer;
import com.studybuddy.models.Question;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Question moderation: TableView with subject, votes, answers, views, status columns.
 * Lock/unlock, delete question, delete answer, search, subject filter.
 */
public class AdminQuestionsController {

    // ── Question table ────────────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> subjectFilter;

    @FXML private TableView<Question>             questionsTable;
    @FXML private TableColumn<Question, String>   colQuestion;
    @FXML private TableColumn<Question, String>   colSubject;
    @FXML private TableColumn<Question, String>   colAuthor;
    @FXML private TableColumn<Question, Integer>  colVotes;
    @FXML private TableColumn<Question, Integer>  colAnswers;
    @FXML private TableColumn<Question, Integer>  colViews;
    @FXML private TableColumn<Question, String>   colDate;
    @FXML private TableColumn<Question, String>   colStatus;

    // ── Answer table ──────────────────────────────────────────────────────────
    @FXML private TableView<Answer>               answersTable;
    @FXML private TableColumn<Answer, String>     colAnswerAuthor;
    @FXML private TableColumn<Answer, String>     colAnswerText;
    @FXML private TableColumn<Answer, Integer>    colAnswerVotes;
    @FXML private TableColumn<Answer, String>     colAnswerDate;

    // ── Detail view ───────────────────────────────────────────────────────────
    @FXML private TextArea questionDetailArea;

    @FXML private Label  lblPageNumber;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    private final AdminService adminService = AdminService.getInstance();
    private final ObservableList<Question> masterList = FXCollections.observableArrayList();
    private List<Question> filteredList = new ArrayList<>();
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    @FXML
    public void initialize() {
        setupQuestionsTable();
        setupAnswersTable();
        setupFilters();
        loadData();
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());

        // When question selected → load its answers
        questionsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) displayQuestion(n);
            else           clearDetail();
        });
    }

    private void loadData() {
        masterList.setAll(adminService.getQuestions());
        filteredList = new ArrayList<>(masterList);
        currentPage  = 1;
        updateTable();
    }

    @FXML public void handleRefresh() { loadData(); }

    // ── Filtering ─────────────────────────────────────────────────────────────

    @FXML
    public void applyFilters() {
        String q       = searchField.getText().trim().toLowerCase();
        String subject = subjectFilter.getValue();

        filteredList = masterList.stream()
            .filter(q1 -> q.isEmpty()
                    || nullSafe(q1.getQuestionText()).toLowerCase().contains(q)
                    || nullSafe(q1.getAuthorName()).toLowerCase().contains(q))
            .filter(q1 -> subject == null || subject.isEmpty()
                    || nullSafe(q1.getSubject()).equalsIgnoreCase(subject))
            .collect(Collectors.toList());

        currentPage = 1;
        updateTable();
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        subjectFilter.getSelectionModel().clearSelection();
        filteredList = new ArrayList<>(masterList);
        currentPage  = 1;
        updateTable();
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @FXML public void handlePrevPage() { if (currentPage > 1) { currentPage--; updateTable(); } }
    @FXML public void handleNextPage() { if (currentPage < maxPages()) { currentPage++; updateTable(); } }

    private void updateTable() {
        int total    = filteredList.size();
        int maxPages = maxPages();
        currentPage  = Math.max(1, Math.min(currentPage, maxPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        questionsTable.setItems(FXCollections.observableArrayList(
                from < total ? filteredList.subList(from, to) : List.of()));
        if (lblPageNumber != null) lblPageNumber.setText("Page " + currentPage + " of " + maxPages);
        if (btnPrevPage   != null) btnPrevPage.setDisable(currentPage == 1);
        if (btnNextPage   != null) btnNextPage.setDisable(currentPage == maxPages);
    }

    private int maxPages() { return Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE)); }

    // ── Detail display ────────────────────────────────────────────────────────

    private void displayQuestion(Question q) {
        if (questionDetailArea != null) {
            questionDetailArea.setText(
                    "Author : " + nullSafe(q.getAuthorName()) + "\n" +
                    "Subject: " + nullSafe(q.getSubject()) + "\n" +
                    "Tags   : " + nullSafe(q.getTags()) + "\n" +
                    "Votes  : " + q.getVotes() + "   Views: " + q.getViews() + "\n" +
                    "Status : " + (q.isLocked() ? "🔒 Locked" : "🔓 Open") + "\n\n" +
                    nullSafe(q.getQuestionText())
            );
        }
        // Load real answers from DB
        List<Answer> answers = adminService.getAnswersForQuestion(q.getId());
        q.setAnswers(answers);
        if (answersTable != null)
            answersTable.setItems(FXCollections.observableArrayList(answers));
    }

    private void clearDetail() {
        if (questionDetailArea != null) questionDetailArea.clear();
        if (answersTable != null) answersTable.getItems().clear();
    }

    // ── Question Actions ──────────────────────────────────────────────────────

    @FXML
    public void handleLock() {
        Question q = selectedQuestion(); if (q == null) return;
        boolean ok = adminService.lockQuestion(q.getId(), q.getQuestionText());
        if (ok) { q.setLocked(true); questionsTable.refresh(); displayQuestion(q); }
    }

    @FXML
    public void handleUnlock() {
        Question q = selectedQuestion(); if (q == null) return;
        boolean ok = adminService.unlockQuestion(q.getId(), q.getQuestionText());
        if (ok) { q.setLocked(false); questionsTable.refresh(); displayQuestion(q); }
    }

    @FXML
    public void handleToggleLock() {
        Question q = selectedQuestion(); if (q == null) return;
        if (q.isLocked()) handleUnlock(); else handleLock();
    }

    @FXML
    public void handleDeleteQuestion() {
        Question q = selectedQuestion(); if (q == null) return;
        String preview = q.getQuestionText().length() > 60
                ? q.getQuestionText().substring(0, 60) + "…" : q.getQuestionText();
        if (confirm("Delete question and all its answers?\n\"" + preview + "\"")) {
            boolean ok = adminService.deleteQuestion(q.getId(), q.getQuestionText());
            if (ok) { masterList.remove(q); filteredList.remove(q); updateTable(); clearDetail(); }
        }
    }

    // ── Answer Actions ────────────────────────────────────────────────────────

    @FXML
    public void handleDeleteAnswer() {
        Question q = selectedQuestion(); if (q == null) return;
        if (answersTable == null) return;
        Answer a = answersTable.getSelectionModel().getSelectedItem();
        if (a == null) { warn("Please select an answer to delete."); return; }

        if (confirm("Delete this answer by " + nullSafe(a.getAuthorName()) + "?")) {
            boolean ok = adminService.deleteAnswer(a.getId(), q.getQuestionText());
            if (ok) {
                q.getAnswers().remove(a);
                answersTable.getItems().remove(a);
                // Refresh answer-count placeholder in master list
                List<Answer> remaining = new ArrayList<>(q.getAnswers());
                q.setAnswers(remaining);
                questionsTable.refresh();
            }
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupQuestionsTable() {
        if (colQuestion != null) colQuestion.setCellValueFactory(new PropertyValueFactory<>("questionText"));
        if (colSubject  != null) colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (colAuthor   != null) colAuthor.setCellValueFactory(new PropertyValueFactory<>("authorName"));
        if (colVotes    != null) colVotes.setCellValueFactory(new PropertyValueFactory<>("votes"));
        if (colAnswers  != null) colAnswers.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getAnswers() != null
                        ? cellData.getValue().getAnswers().size() : 0).asObject());
        if (colViews    != null) colViews.setCellValueFactory(new PropertyValueFactory<>("views"));
        if (colDate     != null) colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        if (colStatus   != null) colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isLocked() ? "🔒 Locked" : "🔓 Open"));
    }

    private void setupAnswersTable() {
        if (colAnswerAuthor != null) colAnswerAuthor.setCellValueFactory(new PropertyValueFactory<>("authorName"));
        if (colAnswerText   != null) colAnswerText.setCellValueFactory(new PropertyValueFactory<>("answerText"));
        if (colAnswerVotes  != null) colAnswerVotes.setCellValueFactory(new PropertyValueFactory<>("votes"));
        if (colAnswerDate   != null) colAnswerDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    private void setupFilters() {
        if (subjectFilter != null) subjectFilter.setItems(FXCollections.observableArrayList(
                "", "Mathematics", "Physics", "Chemistry", "Computer Science",
                "Biology", "English", "History", "Geography", "Other"));
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private Question selectedQuestion() {
        Question q = questionsTable.getSelectionModel().getSelectedItem();
        if (q == null) { warn("Please select a question first."); }
        return q;
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void warn(String msg) { Alert a = new Alert(Alert.AlertType.WARNING, msg); a.setHeaderText(null); a.showAndWait(); }
    private String nullSafe(String s) { return s != null ? s : ""; }
}
