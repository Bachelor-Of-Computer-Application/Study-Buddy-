package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Reports & Analytics controller.
 * All charts use real DB data. Provides CSV export for each dataset.
 */
public class AdminReportsController {

    // ── Charts ────────────────────────────────────────────────────────────────
    @FXML private BarChart<String, Number>   uploadsChart;
    @FXML private CategoryAxis               uploadsX;
    @FXML private NumberAxis                 uploadsY;

    @FXML private LineChart<String, Number>  registrationsChart;
    @FXML private CategoryAxis               registrationsX;
    @FXML private NumberAxis                 registrationsY;

    @FXML private PieChart                   subjectPieChart;

    @FXML private BarChart<String, Number>   topResourcesChart;
    @FXML private CategoryAxis               topResourcesX;
    @FXML private NumberAxis                 topResourcesY;

    // ── Tables ────────────────────────────────────────────────────────────────
    @FXML private TableView<Map<String, Object>>              topUsersTable;
    @FXML private TableColumn<Map<String, Object>, String>    colUserName;
    @FXML private TableColumn<Map<String, Object>, Integer>   colUserScore;

    @FXML private TableView<Map<String, Object>>              topResourcesTable;
    @FXML private TableColumn<Map<String, Object>, String>    colResTitle;
    @FXML private TableColumn<Map<String, Object>, Integer>   colResDownloads;

    private final AdminService adminService = AdminService.getInstance();

    @FXML
    public void initialize() {
        loadAllCharts();
        setupTables();
        loadTableData();
    }

    @FXML
    public void handleRefresh() {
        clearCharts();
        loadAllCharts();
        loadTableData();
    }

    // ── Chart Loading ─────────────────────────────────────────────────────────

    private void loadAllCharts() {
        loadUploadsChart();
        loadRegistrationsChart();
        loadSubjectChart();
        loadTopResourcesChart();
    }

    private void clearCharts() {
        if (uploadsChart       != null) uploadsChart.getData().clear();
        if (registrationsChart != null) registrationsChart.getData().clear();
        if (subjectPieChart    != null) subjectPieChart.getData().clear();
        if (topResourcesChart  != null) topResourcesChart.getData().clear();
    }

    private void loadUploadsChart() {
        if (uploadsChart == null) return;
        Map<String, Integer> data = adminService.getMonthlyUploads();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Notes Uploaded");
        data.forEach((month, count) -> series.getData().add(new XYChart.Data<>(month, count)));
        uploadsChart.getData().add(series);
        uploadsChart.setLegendVisible(false);
    }

    private void loadRegistrationsChart() {
        if (registrationsChart == null) return;
        Map<String, Integer> data = adminService.getMonthlyRegistrations();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("New Users");
        data.forEach((month, count) -> series.getData().add(new XYChart.Data<>(month, count)));
        registrationsChart.getData().add(series);
        registrationsChart.setLegendVisible(false);
    }

    private void loadSubjectChart() {
        if (subjectPieChart == null) return;
        Map<String, Integer> data = adminService.getNotesBySubject();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (data.isEmpty()) {
            pieData.add(new PieChart.Data("No Data", 1));
        } else {
            data.forEach((subject, count) -> pieData.add(new PieChart.Data(subject + " (" + count + ")", count)));
        }
        subjectPieChart.setData(pieData);
    }

    private void loadTopResourcesChart() {
        if (topResourcesChart == null) return;
        List<Map<String, Object>> data = adminService.getTopDownloadedResources(8);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Downloads");
        for (Map<String, Object> row : data) {
            String title = (String) row.get("title");
            int    dl    = row.get("downloads") != null ? ((Number) row.get("downloads")).intValue() : 0;
            if (title != null && title.length() > 20) title = title.substring(0, 20) + "…";
            series.getData().add(new XYChart.Data<>(title, dl));
        }
        topResourcesChart.getData().add(series);
        topResourcesChart.setLegendVisible(false);
    }

    // ── Table Setup & Data ────────────────────────────────────────────────────

    private void setupTables() {
        if (colUserName  != null) colUserName.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("name")));
        if (colUserScore != null) colUserScore.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty((int) data.getValue().get("score")).asObject());

        if (colResTitle     != null) colResTitle.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("title")));
        if (colResDownloads != null) colResDownloads.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty((int) data.getValue().get("downloads")).asObject());
    }

    private void loadTableData() {
        if (topUsersTable != null) {
            topUsersTable.setItems(FXCollections.observableArrayList(
                    adminService.getTopActiveUsers(10)));
        }
        if (topResourcesTable != null) {
            topResourcesTable.setItems(FXCollections.observableArrayList(
                    adminService.getTopDownloadedResources(10)));
        }
    }

    // ── Export ────────────────────────────────────────────────────────────────

    @FXML
    public void handleExportUploads() {
        exportToCSV("monthly_uploads.csv", "Month,Uploads",
                adminService.getMonthlyUploads());
    }

    @FXML
    public void handleExportRegistrations() {
        exportToCSV("monthly_registrations.csv", "Month,New Users",
                adminService.getMonthlyRegistrations());
    }

    @FXML
    public void handleExportSubjects() {
        exportToCSV("subject_stats.csv", "Subject,Notes Count",
                adminService.getNotesBySubject());
    }

    @FXML
    public void handleExportTopUsers() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Top Active Users");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("top_active_users.csv");
        var file = chooser.showSaveDialog(null);
        if (file == null) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Name,Activity Score");
            for (Map<String, Object> row : adminService.getTopActiveUsers(50)) {
                pw.printf("\"%s\",%d%n", row.get("name"), row.get("score"));
            }
            info("Exported to: " + file.getAbsolutePath());
        } catch (IOException e) {
            warn("Export failed: " + e.getMessage());
        }
    }

    @FXML
    public void handleExportTopResources() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Top Downloaded Resources");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("top_resources.csv");
        var file = chooser.showSaveDialog(null);
        if (file == null) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Title,Downloads");
            for (Map<String, Object> row : adminService.getTopDownloadedResources(50)) {
                pw.printf("\"%s\",%d%n", row.get("title"), row.get("downloads"));
            }
            info("Exported to: " + file.getAbsolutePath());
        } catch (IOException e) {
            warn("Export failed: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void exportToCSV(String defaultName, String header, Map<String, Integer> data) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export to CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName(defaultName);
        var file = chooser.showSaveDialog(null);
        if (file == null) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println(header);
            data.forEach((k, v) -> pw.printf("\"%s\",%d%n", k, v));
            info("Exported to: " + file.getAbsolutePath());
        } catch (IOException e) {
            warn("Export failed: " + e.getMessage());
        }
    }

    private void info(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void warn(String msg) { Alert a = new Alert(Alert.AlertType.WARNING, msg); a.setHeaderText(null); a.showAndWait(); }
}
