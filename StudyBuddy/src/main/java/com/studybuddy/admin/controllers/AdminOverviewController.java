package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.models.Note;
import com.studybuddy.models.Question;
import com.studybuddy.models.User;
import com.studybuddy.utils.EventBus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Controller for the AdminDashboardOverview.fxml panel.
 * Shows stat cards driven by real DB queries and recent-item tables.
 */
public class AdminOverviewController {

    private static final Logger logger = Logger.getLogger(AdminOverviewController.class.getName());

    // ══════════════════════════════════════════════════════════════════════════
    // Instance counter – proves how many times this controller is created
    // ══════════════════════════════════════════════════════════════════════════
    private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger(0);
    private final int instanceId;

    public AdminOverviewController() {
        instanceId = INSTANCE_COUNT.incrementAndGet();
        logger.fine("AdminOverviewController CREATED. Total instances so far: " + instanceId);
        logger.fine("Constructor identityHashCode: " + System.identityHashCode(this));
    }

    // ── Stat card labels ──────────────────────────────────────────────────────
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalNotes;
    @FXML private Label lblTotalResources;
    @FXML private Label lblTotalQuestions;
    @FXML private Label lblTotalAnswers;
    @FXML private Label lblTotalTasks;
    @FXML private Label lblNewUsersToday;
    @FXML private Label lblUploadsToday;
    @FXML private Label lblPendingNotes;
    @FXML private Label lblPendingResources;
    @FXML private Label lblPendingRewards;
    @FXML private Label lblCompletedRewards;
    @FXML private Label lblTotalPointsTransferred;
    @FXML private Label lblAverageReward;
    @FXML private Label lblHighestReward;
    @FXML private Label lblRecentReward;

    // ── Chart ─────────────────────────────────────────────────────────────────
    @FXML private PieChart subjectPieChart;

    // ── Recent Users table ────────────────────────────────────────────────────
    @FXML private TableView<User>              recentUsersTable;
    @FXML private TableColumn<User, String>    recentUserName;
    @FXML private TableColumn<User, String>    recentUserEmail;
    @FXML private TableColumn<User, String>    recentUserRole;

    // ── Recent Uploads table ──────────────────────────────────────────────────
    @FXML private TableView<Note>              recentUploadsTable;
    @FXML private TableColumn<Note, String>    recentNoteTitle;
    @FXML private TableColumn<Note, String>    recentNoteSubject;
    @FXML private TableColumn<Note, String>    recentNoteStatus;

    // ── Recent Questions table ────────────────────────────────────────────────
    @FXML private TableView<Question>          recentQuestionsTable;
    @FXML private TableColumn<Question, String> recentQText;
    @FXML private TableColumn<Question, String> recentQSubject;
    @FXML private TableColumn<Question, Integer> recentQVotes;

    private final AdminService adminService = AdminService.getInstance();

    @FXML
    public void initialize() {
        logger.fine("[DEBUG] initialize called on instance #" + instanceId);
        logger.fine("[DEBUG] AdminOverviewController initialized (instance #" + instanceId + ")");
        logger.fine("[DEBUG] initialize identityHashCode: " + System.identityHashCode(this));
        logger.fine("[DEBUG] lblTotalUsers != null: " + (lblTotalUsers != null));
        logger.fine("[DEBUG] lblTotalNotes != null: " + (lblTotalNotes != null));
        logger.fine("[DEBUG] lblTotalResources != null: " + (lblTotalResources != null));
        logger.fine("[DEBUG] lblTotalQuestions != null: " + (lblTotalQuestions != null));
        logger.fine("[DEBUG] lblTotalAnswers != null: " + (lblTotalAnswers != null));
        logger.fine("[DEBUG] lblTotalTasks != null: " + (lblTotalTasks != null));
        logger.fine("[DEBUG] lblNewUsersToday != null: " + (lblNewUsersToday != null));
        logger.fine("[DEBUG] lblUploadsToday != null: " + (lblUploadsToday != null));
        logger.fine("[DEBUG] lblPendingNotes != null: " + (lblPendingNotes != null));
        logger.fine("[DEBUG] lblPendingResources != null: " + (lblPendingResources != null));
        setupTableColumns();
        loadStats();
        loadRecentData();
        loadSubjectChart();
        // Verify labels were actually set — print text AFTER loadStats() finishes
        logger.fine("[DEBUG] POST-LOAD VERIFY lblTotalUsers.getText() = '" + (lblTotalUsers != null ? lblTotalUsers.getText() : "NULL_LABEL") + "'");
        logger.fine("[DEBUG] POST-LOAD VERIFY lblTotalNotes.getText() = '" + (lblTotalNotes != null ? lblTotalNotes.getText() : "NULL_LABEL") + "'");
        
        // Subscribe to EventBus events - ensure UI updates happen on JavaFX Application Thread
        EventBus.getInstance().subscribe(EventBus.NotesChangedEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshStats));
        EventBus.getInstance().subscribe(EventBus.ResourcesChangedEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshStats));
        EventBus.getInstance().subscribe(EventBus.QuestionsChangedEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshStats));
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshStats));
        EventBus.getInstance().subscribe(EventBus.AdminChangesEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshStats));
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void loadStats() {
        logger.fine("[DEBUG] loadStats called on instance #" + instanceId);
        Map<String, Integer> stats = adminService.getDashboardStats();
        logger.fine("[DEBUG] Retrieved Stats Map: " + stats);
        if (stats != null) {
            logger.fine("[DEBUG] Users = " + stats.get("totalUsers"));
            logger.fine("[DEBUG] Notes = " + stats.get("totalNotes"));
            logger.fine("[DEBUG] Resources = " + stats.get("totalResources"));
            logger.fine("[DEBUG] Questions = " + stats.get("totalQuestions"));
            logger.fine("[DEBUG] Answers = " + stats.get("totalAnswers"));
        }
        setTextAndLog(lblTotalUsers,      "lblTotalUsers",      stats.get("totalUsers"));
        setTextAndLog(lblTotalNotes,      "lblTotalNotes",      stats.get("totalNotes"));
        setTextAndLog(lblTotalResources,  "lblTotalResources",  stats.get("totalResources"));
        setTextAndLog(lblTotalQuestions,  "lblTotalQuestions",  stats.get("totalQuestions"));
        setTextAndLog(lblTotalAnswers,    "lblTotalAnswers",    stats.get("totalAnswers"));
        setTextAndLog(lblPendingRewards,         "lblPendingRewards",         stats.get("pendingRewards"));
        setTextAndLog(lblCompletedRewards,       "lblCompletedRewards",       stats.get("completedRewards"));
        setTextAndLog(lblTotalPointsTransferred, "lblTotalPointsTransferred", stats.get("totalPointsTransferred"));
        setTextAndLog(lblAverageReward,          "lblAverageReward",          stats.get("averageReward"));
        setTextAndLog(lblHighestReward,          "lblHighestReward",          stats.get("highestReward"));
        setTextAndLog(lblRecentReward,           "lblRecentReward",           stats.get("recentReward"));
    }

    /** Sets label text AND prints proof that setText() was actually executed. */
    private void setTextAndLog(Label lbl, String name, Integer val) {
        if (lbl == null) {
            logger.fine("[DEBUG] SKIP " + name + " — label is NULL (fx:id mismatch)");
            return;
        }
        String text = val != null ? String.valueOf(val) : "0";
        logger.fine("[DEBUG] " + lbl);
        logger.fine("[DEBUG] Before " + name + ".getText(): " + lbl.getText());
        lbl.setText(text);
        logger.fine("[DEBUG] After " + name + ".getText(): " + lbl.getText());
    }

    // ── Recent data ───────────────────────────────────────────────────────────

    private void loadRecentData() {
        List<User>     users     = adminService.getRecentUsers(5);
        List<Note>     uploads   = adminService.getRecentUploads(5);
        List<Question> questions = adminService.getRecentQuestions(5);

        if (recentUsersTable != null)     recentUsersTable.setItems(FXCollections.observableArrayList(users));
        if (recentUploadsTable != null)   recentUploadsTable.setItems(FXCollections.observableArrayList(uploads));
        if (recentQuestionsTable != null) recentQuestionsTable.setItems(FXCollections.observableArrayList(questions));
    }

    // ── Chart ─────────────────────────────────────────────────────────────────

    private void loadSubjectChart() {
        if (subjectPieChart == null) return;
        Map<String, Integer> bySubject = adminService.getNotesBySubject();
        if (bySubject.isEmpty()) {
            subjectPieChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("No Data", 1)));
            return;
        }
        var data = FXCollections.<PieChart.Data>observableArrayList();
        bySubject.forEach((subject, count) -> data.add(new PieChart.Data(subject + " (" + count + ")", count)));
        subjectPieChart.setData(data);
    }

    // ── Table column setup ────────────────────────────────────────────────────

    private void setupTableColumns() {
        if (recentUserName    != null) recentUserName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (recentUserEmail   != null) recentUserEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (recentUserRole    != null) recentUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        if (recentNoteTitle   != null) recentNoteTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (recentNoteSubject != null) recentNoteSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (recentNoteStatus  != null) recentNoteStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        if (recentQText    != null) recentQText.setCellValueFactory(new PropertyValueFactory<>("questionText"));
        if (recentQSubject != null) recentQSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (recentQVotes   != null) recentQVotes.setCellValueFactory(new PropertyValueFactory<>("votes"));
    }

    // ── Quick Actions ─────────────────────────────────────────────────────────

    @FXML public void refreshStats() {
        loadStats();
        loadRecentData();
        loadSubjectChart();
    }
}