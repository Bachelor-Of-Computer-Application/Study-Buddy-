package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.models.Answer;
import com.studybuddy.models.Department;
import com.studybuddy.models.Question;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.QuestionService;
import com.studybuddy.utils.AcademicFilterHelper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Question moderation: TableView with subject, votes, answers, views, status columns.
 * Lock/unlock, delete question, delete answer, search, subject filter.
 */
public class AdminQuestionsController {

    // Question Bank form
    @FXML private TextField bankTitleField;
    @FXML private TextArea bankQuestionArea;
    @FXML private ComboBox<com.studybuddy.models.Department> bankDeptCombo;
    @FXML private ComboBox<com.studybuddy.models.Semester> bankSemCombo;
    @FXML private ComboBox<com.studybuddy.models.Subject> bankSubjectCombo;
    @FXML private ComboBox<String> bankDifficultyCombo;
    @FXML private ComboBox<String> bankTypeCombo;
    @FXML private ComboBox<String> bankStatusCombo;
    @FXML private TextField bankAttachmentField;
    private java.io.File bankAttachmentFile;
    private com.studybuddy.models.Question editingBankQuestion;
    private final AcademicService academicService = AcademicService.getInstance();

    // ── Question table ────────────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<Department> departmentFilter;
    @FXML private ComboBox<Semester>   semesterFilter;
    @FXML private ComboBox<String>    subjectFilter;
    @FXML private ComboBox<String>    statusFilter;

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
    @FXML private ScrollPane pageScrollPane;

    private final AdminService adminService = AdminService.getInstance();
    private final QuestionService questionService = new QuestionService();
    private final ObservableList<Question> masterList = FXCollections.observableArrayList();
    private List<Question> filteredList = new ArrayList<>();
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    @FXML
    public void initialize() {
        setupBankForm();
        setupQuestionsTable();
        setupAnswersTable();
        setupFilters();
        loadData();
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        if (questionsTable != null) {
            questionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }

        // When question selected → load detail, answers, and bank edit form
        questionsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                displayQuestion(n);
                loadQuestionIntoForm(n);
            } else {
                clearDetail();
            }
        });
    }

    private void loadData() {
        try {
            masterList.setAll(questionService.getAllQuestionsForAdminPanel());
        } catch (Exception e) {
            masterList.setAll(adminService.getQuestions());
        }
        filteredList = new ArrayList<>(masterList);
        currentPage  = 1;
        updateTable();
    }

    private void setupBankForm() {
        if (bankDifficultyCombo != null) {
            bankDifficultyCombo.setItems(FXCollections.observableArrayList("Easy", "Medium", "Hard"));
        }
        if (bankTypeCombo != null) {
            bankTypeCombo.setItems(FXCollections.observableArrayList("MCQ", "Short Answer", "Long Answer", "True/False", "Essay"));
        }
        if (bankStatusCombo != null) {
            bankStatusCombo.setItems(FXCollections.observableArrayList("Draft", "Published", "Archived"));
            bankStatusCombo.setValue("Draft");
        }
        if (bankDeptCombo != null) {
            bankDeptCombo.setItems(AcademicFilterHelper.departmentsForFilter(academicService));
            bankDeptCombo.setValue(AcademicFilterHelper.allDepartments());
        }
        if (bankSemCombo != null) {
            bankSemCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
            bankSemCombo.setValue(AcademicFilterHelper.allSemesters());
            bankSemCombo.setDisable(false);
        }
        if (bankSubjectCombo != null) bankSubjectCombo.setDisable(false);
        AcademicFilterHelper.wireCascade(academicService, bankDeptCombo, bankSemCombo, bankSubjectCombo,
                () -> AcademicFilterHelper.loadSubjects(academicService,
                        bankDeptCombo.getValue(), bankSemCombo.getValue(),
                        bankSubjectCombo));
        AcademicFilterHelper.loadSubjects(academicService,
                bankDeptCombo.getValue(), bankSemCombo.getValue(),
                bankSubjectCombo);
    }

    @FXML public void handleSelectBankAttachment() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        javafx.scene.Scene scene = pageScrollPane != null ? pageScrollPane.getScene()
                : (bankTitleField != null ? bankTitleField.getScene()
                : (questionsTable != null ? questionsTable.getScene() : null));
        if (scene == null) { warn("Unable to open file chooser."); return; }
        java.io.File f = fc.showOpenDialog(scene.getWindow());
        if (f != null) { bankAttachmentFile = f; bankAttachmentField.setText(f.getName()); }
    }

    @FXML public void handleSaveBankQuestion() {
        if (bankTitleField == null || bankTitleField.getText() == null || bankTitleField.getText().isBlank()) {
            warn("Title required."); return;
        }
        if (bankQuestionArea == null || bankQuestionArea.getText() == null || bankQuestionArea.getText().isBlank()) {
            warn("Question text required."); return;
        }
        if (!validateBankForm()) return;
        try {
            Question q = buildBankQuestion();
            questionService.saveQuestionBankEntry(q);
            info("Question saved.");
            resetBankForm();
            loadData();
        } catch (Exception e) { warn("Save failed: " + e.getMessage()); }
    }

    @FXML public void handleUpdateBankQuestion() {
        if (editingBankQuestion == null) {
            editingBankQuestion = questionsTable.getSelectionModel().getSelectedItem();
        }
        if (editingBankQuestion == null) { warn("Select a question to update."); return; }
        if (bankQuestionArea == null || bankQuestionArea.getText() == null || bankQuestionArea.getText().isBlank()) {
            warn("Question text required."); return;
        }
        try {
            com.studybuddy.models.Question q = buildBankQuestion();
            q.setId(editingBankQuestion.getId());
            if (bankAttachmentFile == null && editingBankQuestion.getAttachmentPath() != null) {
                q.setAttachmentPath(editingBankQuestion.getAttachmentPath());
            }
            questionService.updateQuestionBankEntry(q);
            info("Question updated.");
            resetBankForm();
            loadData();
        } catch (Exception e) { warn("Update failed: " + e.getMessage()); }
    }

    @FXML public void handlePublishBankQuestion() {
        if (bankStatusCombo != null) bankStatusCombo.setValue("Published");
        if (editingBankQuestion != null) handleUpdateBankQuestion();
        else handleSaveBankQuestion();
    }

    @FXML public void handleUnpublishBankQuestion() {
        if (bankStatusCombo != null) bankStatusCombo.setValue("Draft");
        if (editingBankQuestion != null) handleUpdateBankQuestion();
        else warn("Select a question to unpublish.");
    }

    private boolean validateBankForm() {
        Semester sem = bankSemCombo != null ? bankSemCombo.getValue() : null;
        if (!AcademicFilterHelper.isAllSemesters(sem)
                && (bankSubjectCombo == null || bankSubjectCombo.getValue() == null)) {
            warn("Select a subject when a specific semester is chosen.");
            return false;
        }
        return true;
    }

    @FXML public void handleDeleteBankQuestion() {
        com.studybuddy.models.Question q = questionsTable.getSelectionModel().getSelectedItem();
        if (q == null) { warn("Select a question to delete."); return; }
        if (confirm("Delete question '" + displayLabel(q) + "' permanently?")) {
            try {
                com.studybuddy.models.Question full = questionService.getQuestionById(q.getId());
                String attachment = full != null ? full.getAttachmentPath() : q.getAttachmentPath();
                boolean ok = adminService.deleteQuestion(q.getId(), q.getQuestionText());
                if (ok) {
                    if (attachment != null && !attachment.isBlank()) {
                        com.studybuddy.services.FileStorageService.getInstance().deleteFile(attachment);
                    }
                    info("Question deleted.");
                    resetBankForm();
                    loadData();
                } else {
                    warn("Delete failed. Check server logs for details.");
                }
            } catch (Exception e) { warn("Delete failed: " + e.getMessage()); }
        }
    }

    @FXML public void handleResetBankForm() { resetBankForm(); }

    private Question buildBankQuestion() throws Exception {
        Question q = new Question();
        q.setTitle(bankTitleField.getText().trim());
        q.setQuestionText(bankQuestionArea.getText().trim());
        if (bankDifficultyCombo != null) q.setDifficulty(bankDifficultyCombo.getValue());
        if (bankTypeCombo != null) q.setQuestionType(bankTypeCombo.getValue());
        if (bankStatusCombo != null) q.setStatus(bankStatusCombo.getValue());
        int adminId = 1;
        if (com.studybuddy.utils.SessionManager.getCurrentAdmin() != null
                && com.studybuddy.utils.SessionManager.getCurrentAdmin().getId() > 0) {
            adminId = com.studybuddy.utils.SessionManager.getCurrentAdmin().getId();
        }
        q.setUserId(adminId);
        q.setAuthorName("Admin");
        if (bankSubjectCombo != null && bankSubjectCombo.getValue() != null) {
            q.setSubject(bankSubjectCombo.getValue().getName());
            q.setSubjectId(bankSubjectCombo.getValue().getId());
        }
        Integer deptId = AcademicFilterHelper.resolveDepartmentId(bankDeptCombo != null ? bankDeptCombo.getValue() : null);
        Integer semId = AcademicFilterHelper.resolveSemesterId(bankSemCombo != null ? bankSemCombo.getValue() : null);
        q.setDepartmentId(deptId != null ? deptId : 0);
        q.setSemesterId(semId != null ? semId : 0);
        if (bankAttachmentFile != null) {
            q.setAttachmentPath(com.studybuddy.services.FileStorageService.getInstance()
                    .storeFile(bankAttachmentFile, "questions"));
        }
        return q;
    }

    private void loadQuestionIntoForm(Question row) {
        try {
            Question full = questionService.getQuestionById(row.getId());
            if (full == null) full = row;
            editingBankQuestion = full;

            if (bankTitleField != null) {
                bankTitleField.setText(full.getTitle() != null ? full.getTitle() : "");
            }
            if (bankQuestionArea != null) bankQuestionArea.setText(nullSafe(full.getQuestionText()));

            if (bankDeptCombo != null) {
                if (full.getDepartmentId() > 0) {
                    for (Department d : academicService.getAllActiveDepartments()) {
                        if (d.getId() == full.getDepartmentId()) {
                            bankDeptCombo.setValue(d);
                            if (bankSemCombo != null) {
                                bankSemCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, d));
                                bankSemCombo.setDisable(false);
                            }
                            break;
                        }
                    }
                } else {
                    bankDeptCombo.setValue(AcademicFilterHelper.allDepartments());
                    if (bankSemCombo != null) {
                        bankSemCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
                        bankSemCombo.setValue(AcademicFilterHelper.allSemesters());
                        bankSemCombo.setDisable(false);
                    }
                }
            }

            if (bankSemCombo != null) {
                if (full.getSemesterId() > 0) {
                    for (Semester s : bankSemCombo.getItems()) {
                        if (s.getId() == full.getSemesterId()) {
                            bankSemCombo.setValue(s);
                            bankSemCombo.setDisable(false);
                            if (bankSubjectCombo != null) {
                                AcademicFilterHelper.loadSubjects(academicService,
                                        bankDeptCombo.getValue(), s,
                                        bankSubjectCombo);
                                bankSubjectCombo.setDisable(false);
                            }
                            break;
                        }
                    }
                } else {
                    bankSemCombo.setValue(AcademicFilterHelper.allSemesters());
                    if (bankSubjectCombo != null) {
                        AcademicFilterHelper.loadSubjects(academicService,
                                bankDeptCombo.getValue(), bankSemCombo.getValue(),
                                bankSubjectCombo);
                        bankSubjectCombo.setDisable(false);
                    }
                }
            }

            if (bankSubjectCombo != null && full.getSubjectId() > 0) {
                for (com.studybuddy.models.Subject s : bankSubjectCombo.getItems()) {
                    if (s.getId() == full.getSubjectId()) {
                        bankSubjectCombo.setValue(s);
                        break;
                    }
                }
            } else if (bankSubjectCombo != null && full.getSubject() != null) {
                bankSubjectCombo.getSelectionModel().clearSelection();
            }

            if (bankDifficultyCombo != null && full.getDifficulty() != null) {
                bankDifficultyCombo.setValue(full.getDifficulty());
            }
            if (bankTypeCombo != null && full.getQuestionType() != null) {
                bankTypeCombo.setValue(full.getQuestionType());
            }
            if (bankStatusCombo != null) {
                bankStatusCombo.setValue(full.getStatus() != null ? full.getStatus() : "Draft");
            }

            bankAttachmentFile = null;
            if (bankAttachmentField != null) {
                if (full.getAttachmentPath() != null && !full.getAttachmentPath().isBlank()) {
                    bankAttachmentField.setText(new java.io.File(full.getAttachmentPath()).getName());
                } else {
                    bankAttachmentField.clear();
                }
            }
        } catch (Exception e) {
            editingBankQuestion = row;
            if (bankTitleField != null) bankTitleField.setText(row.getTitle() != null ? row.getTitle() : "");
            if (bankQuestionArea != null) bankQuestionArea.setText(nullSafe(row.getQuestionText()));
        }
    }

    private void resetBankForm() {
        editingBankQuestion = null;
        if (bankTitleField != null) bankTitleField.clear();
        if (bankQuestionArea != null) bankQuestionArea.clear();
        if (bankAttachmentField != null) bankAttachmentField.clear();
        bankAttachmentFile = null;
        if (bankDeptCombo != null) bankDeptCombo.setValue(AcademicFilterHelper.allDepartments());
        if (bankSemCombo != null) {
            bankSemCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
            bankSemCombo.setValue(AcademicFilterHelper.allSemesters());
            bankSemCombo.setDisable(false);
        }
        if (bankSubjectCombo != null) {
            bankSubjectCombo.getSelectionModel().clearSelection();
            AcademicFilterHelper.loadSubjects(academicService,
                    bankDeptCombo != null ? bankDeptCombo.getValue() : null,
                    bankSemCombo != null ? bankSemCombo.getValue() : null,
                    bankSubjectCombo);
        }
        if (bankDifficultyCombo != null) bankDifficultyCombo.getSelectionModel().clearSelection();
        if (bankTypeCombo != null) bankTypeCombo.getSelectionModel().clearSelection();
        if (bankStatusCombo != null) bankStatusCombo.setValue("Draft");
    }

    @FXML public void handleRefresh() { loadData(); }

    // ── Filtering ─────────────────────────────────────────────────────────────

    @FXML
    public void applyFilters() {
        String q       = searchField.getText().trim().toLowerCase();
        Department dept = departmentFilter != null ? departmentFilter.getValue() : null;
        Semester sem = semesterFilter != null ? semesterFilter.getValue() : null;
        String subject = subjectFilter != null ? subjectFilter.getValue() : null;
        String status  = statusFilter != null ? statusFilter.getValue() : null;
        List<Subject> allSubjects = academicService.getAllActiveSubjects();
        Map<Integer, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (a, b) -> a));

        filteredList = masterList.stream()
            .filter(q1 -> q.isEmpty()
                    || nullSafe(q1.getQuestionText()).toLowerCase().contains(q)
                    || nullSafe(q1.getTitle()).toLowerCase().contains(q)
                    || nullSafe(q1.getAuthorName()).toLowerCase().contains(q))
            .filter(q1 -> subject == null || subject.isEmpty()
                    || nullSafe(q1.getSubject()).equalsIgnoreCase(subject))
            .filter(q1 -> AcademicFilterHelper.matchesDeptSemFilter(
                    q1.getDepartmentId() > 0 ? q1.getDepartmentId() : null,
                    q1.getSemesterId() > 0 ? q1.getSemesterId() : null,
                    q1.getSubjectId(), dept, sem, subjectMap, allSubjects, nullSafe(q1.getSubject())))
            .filter(q1 -> {
                if (status == null || status.isEmpty()) return true;
                String itemStatus = q1.getStatus();
                if (itemStatus != null && !itemStatus.isBlank()) {
                    return status.equalsIgnoreCase(itemStatus);
                }
                if ("Locked".equalsIgnoreCase(status)) return q1.isLocked();
                if ("Open".equalsIgnoreCase(status)) return !q1.isLocked();
                return true;
            })
            .collect(Collectors.toList());

        currentPage = 1;
        updateTable();
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        AcademicFilterHelper.resetFilters(academicService, departmentFilter, semesterFilter, subjectFilter);
        if (statusFilter != null) statusFilter.getSelectionModel().clearSelection();
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
        if (confirm("Delete question and all its answers?\n\"" + displayLabel(q) + "\"")) {
            try {
                Question full = questionService.getQuestionById(q.getId());
                String attachment = full != null ? full.getAttachmentPath() : q.getAttachmentPath();
                boolean ok = adminService.deleteQuestion(q.getId(), q.getQuestionText());
                if (ok) {
                    if (attachment != null && !attachment.isBlank()) {
                        com.studybuddy.services.FileStorageService.getInstance().deleteFile(attachment);
                    }
                    masterList.remove(q);
                    filteredList.remove(q);
                    updateTable();
                    clearDetail();
                    resetBankForm();
                } else {
                    warn("Delete failed. Check server logs for details.");
                }
            } catch (Exception e) {
                warn("Delete failed: " + e.getMessage());
            }
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
        if (colQuestion != null) {
            colQuestion.setCellValueFactory(cellData -> {
                Question item = cellData.getValue();
                String label = item.getTitle() != null && !item.getTitle().isBlank()
                        ? item.getTitle() : item.getQuestionText();
                return new SimpleStringProperty(nullSafe(label));
            });
        }
        if (colSubject  != null) colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (colAuthor   != null) colAuthor.setCellValueFactory(new PropertyValueFactory<>("authorName"));
        if (colVotes    != null) colVotes.setCellValueFactory(new PropertyValueFactory<>("votes"));
        if (colAnswers  != null) colAnswers.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getAnswers() != null
                        ? cellData.getValue().getAnswers().size() : 0).asObject());
        if (colViews    != null) colViews.setCellValueFactory(new PropertyValueFactory<>("views"));
        if (colDate     != null) colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        if (colStatus   != null) colStatus.setCellValueFactory(cellData -> {
            Question item = cellData.getValue();
            if (item.getStatus() != null && !item.getStatus().isBlank()) {
                return new SimpleStringProperty(item.getStatus());
            }
            return new SimpleStringProperty(item.isLocked() ? "🔒 Locked" : "🔓 Open");
        });
    }

    private void setupAnswersTable() {
        if (colAnswerAuthor != null) colAnswerAuthor.setCellValueFactory(new PropertyValueFactory<>("authorName"));
        if (colAnswerText   != null) colAnswerText.setCellValueFactory(new PropertyValueFactory<>("answerText"));
        if (colAnswerVotes  != null) colAnswerVotes.setCellValueFactory(new PropertyValueFactory<>("votes"));
        if (colAnswerDate   != null) colAnswerDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    private void setupFilters() {
        AcademicFilterHelper.setupFilterBar(academicService, departmentFilter, semesterFilter, subjectFilter);
        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList(
                    "", "Draft", "Published", "Archived", "Locked", "Open"));
        }
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

    private void info(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void warn(String msg) { Alert a = new Alert(Alert.AlertType.WARNING, msg); a.setHeaderText(null); a.showAndWait(); }
    private String nullSafe(String s) { return s != null ? s : ""; }

    private String displayLabel(Question q) {
        if (q.getTitle() != null && !q.getTitle().isBlank()) return q.getTitle();
        String text = nullSafe(q.getQuestionText());
        return text.length() > 60 ? text.substring(0, 60) + "…" : text;
    }
}
