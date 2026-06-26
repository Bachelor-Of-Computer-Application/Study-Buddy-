package com.studybuddy.admin.controllers;

import com.studybuddy.models.Resource;
import com.studybuddy.admin.services.AdminService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminOverviewController {

    @FXML private Label lblPendingNotes;
    @FXML private Label lblPendingResources;
    @FXML private Label lblPendingQuestions;
    @FXML private Label lblTotalUsers;

    @FXML private PieChart resourcePieChart;
    @FXML private ProgressBar approvedBar;
    @FXML private ProgressBar pendingBar;
    @FXML private ProgressBar rejectedBar;
    @FXML private ListView<String> moderationLogsList;

    private final AdminService adminService = AdminService.getInstance();

    @FXML
    public void initialize() {
        loadOverviewStats();
        setupPieChart();
        setupProgressBars();
        setupLogs();
    }

    private void loadOverviewStats() {
        int usersCount = adminService.getUsers().size();
        lblTotalUsers.setText(String.valueOf(usersCount));

        // Mock counts for pending items
        lblPendingNotes.setText("1");
        lblPendingResources.setText("1");
        lblPendingQuestions.setText("0");
    }

    private void setupPieChart() {
        List<Resource> resources = adminService.getResources();
        int mathCount = 0;
        int csCount = 0;
        int physicsCount = 0;
        int otherCount = 0;

        for (Resource r : resources) {
            String sub = r.getSubject().toLowerCase();
            if (sub.contains("math")) mathCount++;
            else if (sub.contains("computer") || sub.contains("cs")) csCount++;
            else if (sub.contains("physics")) physicsCount++;
            else otherCount++;
        }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("Math (" + mathCount + ")", mathCount),
                new PieChart.Data("Computer Science (" + csCount + ")", csCount),
                new PieChart.Data("Physics (" + physicsCount + ")", physicsCount),
                new PieChart.Data("Others (" + otherCount + ")", otherCount)
        );
        resourcePieChart.setData(data);
    }

    private void setupProgressBars() {
        List<Resource> resources = adminService.getResources();
        long total = resources.size();
        if (total == 0) {
            approvedBar.setProgress(0.0);
            pendingBar.setProgress(0.0);
            rejectedBar.setProgress(0.0);
            return;
        }

        long approved = resources.stream().filter(Resource::isActive).count();
        long pending = total - approved;
        long rejected = 0;

        approvedBar.setProgress((double) approved / total);
        pendingBar.setProgress((double) pending / total);
        rejectedBar.setProgress((double) rejected / total);
    }

    private void setupLogs() {
        ObservableList<String> logs = FXCollections.observableArrayList();
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logs.add("[" + ts + "] Admin session established.");
        logs.add("[" + ts + "] Synced statistics from database.");
        logs.add("[" + ts + "] Resource Moderation check: 1 item pending.");
        moderationLogsList.setItems(logs);
    }
}
