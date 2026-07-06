package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.models.Note;
import com.studybuddy.models.Question;
import com.studybuddy.models.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller for the AdminDashboardOverview.fxml panel.
 * Shows stat cards driven by real DB queries and recent-item tables.
 */
public class AdminOverviewController {

    // ── Instance counter — proves how many times this controller is created ──
    private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger(0);
    private final int instanceId;

    public AdminOverviewController() {
        instanceId = INSTANCE_COUNT.incrementAndGet();
        System.out.println("[DEBUG] AdminOverviewController CREATED. Total instances so far: " + instanceId);
        System.out.println("[DEBUG] Constructor identityHashCode: " + System.identityHashCode(this));
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
        System.out.println("[DEBUG] initialize called on instance #" + instanceId);
        System.out.println("[DEBUG] AdminOverviewController initialized (instance #" + instanceId + ")");
        System.out.println("[DEBUG] initialize identityHashCode: " + System.identityHashCode(this));
        System.out.println("[DEBUG] lblTotalUsers != null: " + (lblTotalUsers != null));
        System.out.println("[DEBUG] lblTotalNotes != null: " + (lblTotalNotes != null));
        System.out.println("[DEBUG] lblTotalResources != null: " + (lblTotalResources != null));
        System.out.println("[DEBUG] lblTotalQuestions != null: " + (lblTotalQuestions != null));
        System.out.println("[DEBUG] lblTotalAnswers != null: " + (lblTotalAnswers != null));
        System.out.println("[DEBUG] lblTotalTasks != null: " + (lblTotalTasks != null));
        System.out.println("[DEBUG] lblNewUsersToday != null: " + (lblNewUsersToday != null));
        System.out.println("[DEBUG] lblUploadsToday != null: " + (lblUploadsToday != null));
        System.out.println("[DEBUG] lblPendingNotes != null: " + (lblPendingNotes != null));
        System.out.println("[DEBUG] lblPendingResources != null: " + (lblPendingResources != null));
        setupTableColumns();
        loadStats();
        loadRecentData();
        loadSubjectChart();
        // Verify labels were actually set — print text AFTER loadStats() finishes
        System.out.println("[DEBUG] POST-LOAD VERIFY lblTotalUsers.getText() = '" + (lblTotalUsers != null ? lblTotalUsers.getText() : "NULL_LABEL") + "'");
        System.out.println("[DEBUG] POST-LOAD VERIFY lblTotalNotes.getText() = '" + (lblTotalNotes != null ? lblTotalNotes.getText() : "NULL_LABEL") + "'");
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void loadStats() {
        System.out.println("[DEBUG] loadStats called on instance #" + instanceId);
        Map<String, Integer> stats = adminService.getDashboardStats();
        System.out.println("[DEBUG] Retrieved Stats Map: " + stats);
        if (stats != null) {
            System.out.println("[DEBUG] Users = " + stats.get("totalUsers"));
            System.out.println("[DEBUG] Notes = " + stats.get("totalNotes"));
            System.out.println("[DEBUG] Resources = " + stats.get("totalResources"));
            System.out.println("[DEBUG] Questions = " + stats.get("totalQuestions"));
            System.out.println("[DEBUG] Answers = " + stats.get("totalAnswers"));
        }
        setTextAndLog(lblTotalUsers,      "lblTotalUsers",      stats.get("totalUsers"));
        setTextAndLog(lblTotalNotes,      "lblTotalNotes",      stats.get("totalNotes"));
        setTextAndLog(lblTotalResources,  "lblTotalResources",  stats.get("totalResources"));
        setTextAndLog(lblTotalQuestions,  "lblTotalQuestions",  stats.get("totalQuestions"));
        setTextAndLog(lblTotalAnswers,    "lblTotalAnswers",    stats.get("totalAnswers"));
        setTextAndLog(lblTotalTasks,      "lblTotalTasks",      stats.get("totalTasks"));
        setTextAndLog(lblNewUsersToday,   "lblNewUsersToday",   stats.get("newUsersToday"));
        setTextAndLog(lblUploadsToday,    "lblUploadsToday",    stats.get("uploadsToday"));
        setTextAndLog(lblPendingNotes,    "lblPendingNotes",    stats.get("pendingNotes"));
        setTextAndLog(lblPendingResources,"lblPendingResources",stats.get("pendingResources"));
    }

    /** Sets label text AND prints proof that setText() was actually executed. */
    private void setTextAndLog(Label lbl, String name, Integer val) {
        if (lbl == null) {
            System.out.println("[DEBUG] SKIP " + name + " — label is NULL (fx:id mismatch)");
            return;
        }
        String text = val != null ? String.valueOf(val) : "0";
        System.out.println("[DEBUG] " + lbl);
        System.out.println("[DEBUG] Before " + name + ".getText(): " + lbl.getText());
        lbl.setText(text);
        System.out.println("[DEBUG] After " + name + ".getText(): " + lbl.getText());
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
