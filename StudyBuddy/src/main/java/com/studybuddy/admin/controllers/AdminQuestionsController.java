package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.admin.services.NotificationService;
import com.studybuddy.models.Answer;
import com.studybuddy.models.Department;
import com.studybuddy.models.Notification;
import com.studybuddy.models.Question;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.QuestionService;
import com.studybuddy.utils.AcademicFilterHelper;
import com.studybuddy.utils.EventBus;
import com.studybuddy.utils.StringUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Question moderation: TableView with subject, votes, answers, views, status columns.
 * Lock/unlock, delete question, delete answer, search, subject filter.
 */
public class AdminQuestionsController {

    private static final Logger logger = Logger.getLogger(AdminQuestionsController.class.getName());

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
    @FXML private ComboBox<Subject>   subjectFilter;
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
    @FXML private TableColumn<Question, Void>     colActions;

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

        // Double-click to open Question Details dialog
        questionsTable.setRowFactory(tv -> {
            TableRow<Question> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Question selected = row.getItem();
                    openQuestionDetailsDialog(selected);
                }
            });
            return row;
        });

        // EventBus auto refresh
        EventBus.getInstance().subscribe(EventBus.QuestionsChangedEvent.class, event -> {
            javafx.application.Platform.runLater(this::loadData);
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
            if (bankQuestionArea != null) bankQuestionArea.setText(StringUtils.nullSafe(full.getQuestionText()));

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
            if (bankQuestionArea != null) bankQuestionArea.setText(StringUtils.nullSafe(row.getQuestionText()));
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
        Subject selectedSubject = subjectFilter != null ? subjectFilter.getValue() : null;
        String subject = selectedSubject != null ? selectedSubject.getName() : null;
        String status  = statusFilter != null ? statusFilter.getValue() : null;
        List<Subject> allSubjects = academicService.getAllActiveSubjects();
        Map<Integer, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (a, b) -> a));

        filteredList = masterList.stream()
            .filter(q1 -> q.isEmpty()
                    || StringUtils.nullSafe(q1.getQuestionText()).toLowerCase().contains(q)
                    || StringUtils.nullSafe(q1.getTitle()).toLowerCase().contains(q)
                    || StringUtils.nullSafe(q1.getAuthorName()).toLowerCase().contains(q))
            .filter(q1 -> subject == null || subject.isEmpty()
                    || StringUtils.nullSafe(q1.getSubject()).equalsIgnoreCase(subject))
            .filter(q1 -> AcademicFilterHelper.matchesDeptSemFilter(
                    q1.getDepartmentId() > 0 ? q1.getDepartmentId() : null,
                    q1.getSemesterId() > 0 ? q1.getSemesterId() : null,
                    q1.getSubjectId(), dept, sem, subjectMap, allSubjects, StringUtils.nullSafe(q1.getSubject())))
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
                    "Author : " + StringUtils.nullSafe(q.getAuthorName()) + "\n" +
                    "Subject: " + StringUtils.nullSafe(q.getSubject()) + "\n" +
                    "Tags   : " + StringUtils.nullSafe(q.getTags()) + "\n" +
                    "Votes  : " + q.getVotes() + "   Views: " + q.getViews() + "\n" +
                    "Status : " + (q.isLocked() ? "🔒 Locked" : "🔓 Open") + "\n\n" +
                    StringUtils.nullSafe(q.getQuestionText())
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

        if (confirm("Delete this answer by " + StringUtils.nullSafe(a.getAuthorName()) + "?")) {
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

    // =========================
    // MARK AS BEST ANSWER (Requirements 3.1, 3.2, 3.3)
    // =========================

    /**
     * Marks the selected answer as the best answer and transfers reward points.
     * Only administrators can perform this action.
     * Requirements: 3.1, 3.2, 3.3
     */
    @FXML
    public void handleMarkBestAnswer() {
        Question q = selectedQuestion(); if (q == null) return;
        if (answersTable == null) { warn("Answer table not available."); return; }

        Answer a = answersTable.getSelectionModel().getSelectedItem();
        if (a == null) { warn("Please select an answer to mark as best."); return; }

        // Check if already rewarded
        if (a.isRewarded()) {
            warn("This answer has already been marked as best answer.");
            return;
        }

        // Check if question is approved (required for reward transfer)
        boolean isApproved = false;
        try {
            isApproved = questionService.isQuestionApproved(q.getId());
        } catch (Exception e) {
            warn("Failed to check question approval status.");
            return;
        }
        
        if (!isApproved) {
            warn("The question must be approved before rewarding the best answer.");
            return;
        }

        // Build professional confirmation dialog
        int rewardPoints = q.getRewardPoints();
        String questionTitle = q.getTitle() != null ? q.getTitle() : q.getQuestionText();
        String questionAuthor = q.getAuthorName() != null ? q.getAuthorName() : "Unknown";
        String answerAuthor = a.getAuthorName() != null ? a.getAuthorName() : "Unknown";
        String rewardStatus = q.getRewardStatus() != null ? q.getRewardStatus() : "Pending";
        
        // Create custom dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirm Best Answer Selection");
        dialog.setHeaderText("🏆 Select Best Answer & Transfer Reward Points");
        
        // Set dialog owner
        if (questionsTable.getScene() != null && questionsTable.getScene().getWindow() != null) {
            dialog.initOwner(questionsTable.getScene().getWindow());
        }
        
        // Build content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Question details
        Label questionLabel = new Label("Question Details:");
        questionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        GridPane questionGrid = new GridPane();
        questionGrid.setHgap(10);
        questionGrid.setVgap(5);
        
        questionGrid.add(new Label("Title:"), 0, 0);
        questionGrid.add(new Label(questionTitle), 1, 0);
        questionGrid.add(new Label("Author:"), 0, 1);
        questionGrid.add(new Label(questionAuthor), 1, 1);
        questionGrid.add(new Label("Approval Status:"), 0, 2);
        Label approvalLabel = new Label("✅ Approved");
        approvalLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
        questionGrid.add(approvalLabel, 1, 2);
        
        // Answer details
        Label answerLabel = new Label("Answer Details:");
        answerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        GridPane answerGrid = new GridPane();
        answerGrid.setHgap(10);
        answerGrid.setVgap(5);
        
        answerGrid.add(new Label("Answer Author:"), 0, 0);
        answerGrid.add(new Label(answerAuthor), 1, 0);
        answerGrid.add(new Label("Reward Points:"), 0, 1);
        Label pointsLabel = new Label(String.valueOf(rewardPoints));
        pointsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #eab308;");
        answerGrid.add(pointsLabel, 1, 1);
        answerGrid.add(new Label("Current Status:"), 0, 2);
        answerGrid.add(new Label(rewardStatus), 1, 2);
        
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
        
        // Add buttons
        ButtonType confirmButtonType = new ButtonType("Transfer Reward", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dialog.getDialogPane().getButtonTypes().addAll(cancelButtonType, confirmButtonType);
        
        // Style the confirm button
        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold;");
        
        // Show dialog and get result
        java.util.Optional<ButtonType> result = dialog.showAndWait();
        
        if (result.isPresent() && result.get() == confirmButtonType) {
            try {
                boolean success = questionService.markBestAnswer(q.getId(), a.getId());

                if (success) {
                    // Refresh the answers list
                    List<Answer> answers = adminService.getAnswersForQuestion(q.getId());
                    q.setAnswers(answers);
                    answersTable.setItems(FXCollections.observableArrayList(answers));
                    questionsTable.refresh();

                    // Send notification to the student
                    try {
                        NotificationService notificationService = NotificationService.getInstance();
                        String notificationTitle = "🏆 Best Answer Selected";
                        String notificationMessage = String.format(
                            "Congratulations! Your answer has been selected as the Best Answer.\nQuestion: %s\nReward Earned: %d Achievement Points\nYour profile has been updated.",
                            questionTitle, rewardPoints
                        );
                        
                        // Get answer author's user ID from the answer
                        int answerAuthorId = a.getUserId();
                        
                        // Create notification for the specific user
                        Notification notification = new Notification();
                        notification.setTitle(notificationTitle);
                        notification.setMessage(notificationMessage);
                        notification.setRecipientType("USER");
                        notification.setRecipientValue(String.valueOf(answerAuthorId));
                        notification.setPriority("HIGH");
                        notification.setNotificationType("Achievement");
                        
                        notificationService.sendSmartNotification(notification);
                    } catch (Exception e) {
                        logger.warning("Failed to send reward notification: " + e.getMessage());
                    }

                    // Success Dialog
                    Dialog<ButtonType> successDialog = new Dialog<>();
                    successDialog.setTitle("Reward Successfully Sent");
                    successDialog.setHeaderText("✅ Reward Successfully Sent");
                    if (questionsTable.getScene() != null && questionsTable.getScene().getWindow() != null) {
                        successDialog.initOwner(questionsTable.getScene().getWindow());
                    }
                    VBox successContent = new VBox(15);
                    successContent.setPadding(new Insets(20));
                    successContent.setAlignment(javafx.geometry.Pos.CENTER);

                    Label successPointsLabel = new Label(rewardPoints + " Achievement Points transferred");
                    successPointsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #22c55e;");

                    Label flowLabel = new Label(
                        questionAuthor + "\n   ↓   \n" + answerAuthor
                    );
                    flowLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-alignment: center; -fx-text-alignment: center;");

                    successContent.getChildren().addAll(successPointsLabel, flowLabel);
                    successDialog.getDialogPane().setContent(successContent);
                    successDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
                    successDialog.showAndWait();
                } else {
                    warn("Failed to send reward. The answer may have already been rewarded.");
                }
            } catch (Exception e) {
                warn("Error sending reward: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void openQuestionDetailsDialog(Question question) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/studybuddy/admin/fxml/AdminQuestionDetails.fxml"));
            Parent root = loader.load();

            AdminQuestionDetailsController controller = loader.getController();
            controller.setQuestion(question);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Question Details");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root, 800, 600));
            controller.setStage(dialogStage);

            dialogStage.showAndWait();

            // Refresh the questions table after dialog closes
            loadData();

        } catch (Exception e) {
            logger.severe("Failed to open question details dialog: " + e.getMessage());
            warn("Failed to open question details: " + e.getMessage());
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupQuestionsTable() {
        if (colQuestion != null) {
            colQuestion.setCellValueFactory(cellData -> {
                Question item = cellData.getValue();
                String label = item.getTitle() != null && !item.getTitle().isBlank()
                        ? item.getTitle() : item.getQuestionText();
                return new SimpleStringProperty(StringUtils.nullSafe(label));
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

        if (colActions != null) {
            colActions.setCellFactory(param -> new TableCell<>() {
                private final Button btnView = new Button("👁 View Details");
                private final Button btnReward = new Button("⭐ Reward Best Answer");
                private final HBox container = new HBox(8, btnView, btnReward);

                {
                    btnView.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 4 8;");
                    btnReward.setStyle("-fx-background-color: #eab308; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 4 8;");
                    container.setAlignment(javafx.geometry.Pos.CENTER);
                    
                    btnView.setOnAction(e -> {
                        Question q = getTableView().getItems().get(getIndex());
                        openQuestionDetailsDialog(q);
                    });
                    
                    btnReward.setOnAction(e -> {
                        Question q = getTableView().getItems().get(getIndex());
                        // Temporarily select item to load answers table so handleMarkBestAnswer knows which answer to mark if they select one there
                        getTableView().getSelectionModel().select(getIndex());
                        // But we want to reward the best answer. Since table view row itself doesn't contain a specific answer, 
                        // let's open details dialog or directly prompt for best answer.
                        // Actually, since the row only has the question, rewarding a best answer requires selecting which answer.
                        // So the most logical flow is: opening details window so they can select the best answer card.
                        // Let's make btnReward open the details window where they can click "Select as Best Answer" on the specific answer card.
                        openQuestionDetailsDialog(q);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Question q = getTableView().getItems().get(getIndex());
                        btnReward.setVisible(q.isApproved());
                        btnReward.setManaged(q.isApproved());
                        
                        boolean hasAnswers = q.getAnswers() != null && !q.getAnswers().isEmpty();
                        boolean rewardTransferred = "TRANSFERRED".equalsIgnoreCase(q.getRewardStatus());
                        boolean disabled = !q.isApproved() || !hasAnswers || rewardTransferred;
                        btnReward.setDisable(disabled);

                        setGraphic(container);
                    }
                }
            });
        }
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

    private String displayLabel(Question q) {
        if (q.getTitle() != null && !q.getTitle().isBlank()) return q.getTitle();
        String text = StringUtils.nullSafe(q.getQuestionText());
        return text != null && text.length() > 60 ? text.substring(0, 60) + "…" : text;
    }
}
