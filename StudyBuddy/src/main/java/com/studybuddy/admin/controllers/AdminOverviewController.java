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

/**
 * Controller for the AdminDashboardOverview.fxml panel.
 * Shows stat cards driven by real DB queries and recent-item tables.
 */
public class AdminOverviewController {

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
        setupTableColumns();
        loadStats();
        loadRecentData();
        loadSubjectChart();
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void loadStats() {
        Map<String, Integer> stats = adminService.getDashboardStats();
        setText(lblTotalUsers,      stats.get("totalUsers"));
        setText(lblTotalNotes,      stats.get("totalNotes"));
        setText(lblTotalResources,  stats.get("totalResources"));
        setText(lblTotalQuestions,  stats.get("totalQuestions"));
        setText(lblTotalAnswers,    stats.get("totalAnswers"));
        setText(lblTotalTasks,      stats.get("totalTasks"));
        setText(lblNewUsersToday,   stats.get("newUsersToday"));
        setText(lblUploadsToday,    stats.get("uploadsToday"));
        setText(lblPendingNotes,    stats.get("pendingNotes"));
        setText(lblPendingResources, stats.get("pendingResources"));
    }

    private void setText(Label lbl, Integer val) {
        if (lbl != null) lbl.setText(val != null ? String.valueOf(val) : "0");
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
