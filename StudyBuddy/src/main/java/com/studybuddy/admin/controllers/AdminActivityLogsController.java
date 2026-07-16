package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.ActivityLogService;
import com.studybuddy.models.ActivityLog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Activity logs viewer: search, date filter, export to CSV.
 * All admin actions throughout the app flow here automatically.
 */
public class AdminActivityLogsController {

    @FXML private TextField   searchField;
    @FXML private DatePicker  fromDatePicker;
    @FXML private DatePicker  toDatePicker;

    @FXML private TableView<ActivityLog>             logsTable;
    @FXML private TableColumn<ActivityLog, Integer>  colId;
    @FXML private TableColumn<ActivityLog, String>   colAdmin;
    @FXML private TableColumn<ActivityLog, String>   colAction;
    @FXML private TableColumn<ActivityLog, String>   colTargetType;
    @FXML private TableColumn<ActivityLog, String>   colTargetName;
    @FXML private TableColumn<ActivityLog, String>   colStatus;
    @FXML private TableColumn<ActivityLog, String>   colTimestamp;

    @FXML private Label lblTotalCount;

    private final ActivityLogService logService = ActivityLogService.getInstance();
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        setupTable();
        loadAll();
        searchField.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.trim().isEmpty()) loadAll();
        });
        
        // Subscribe to EventBus for real-time activity log updates
        com.studybuddy.utils.EventBus.getInstance().subscribe(
            com.studybuddy.utils.EventBus.AdminChangesEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::loadAll)
        );
        com.studybuddy.utils.EventBus.getInstance().subscribe(
            com.studybuddy.utils.EventBus.ActivityChangedEvent.class,
            (_event) -> javafx.application.Platform.runLater(this::loadAll)
        );
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private void loadAll() {
        List<ActivityLog> logs = logService.getLogs();
        setTableData(logs);
    }

    @FXML public void handleRefresh() { loadAll(); }

    // ── Search & Filter ───────────────────────────────────────────────────────

    @FXML
    public void handleSearch() {
        String q = searchField != null ? searchField.getText().trim() : "";
        if (!q.isEmpty()) {
            setTableData(logService.searchLogs(q));
        } else {
            loadAll();
        }
    }

    @FXML
    public void handleDateFilter() {
        LocalDate from = fromDatePicker != null ? fromDatePicker.getValue() : null;
        LocalDate to   = toDatePicker  != null ? toDatePicker.getValue()   : null;

        if (from == null && to == null) { loadAll(); return; }
        if (from == null) from = LocalDate.of(2000, 1, 1);
        if (to   == null) to   = LocalDate.now();

        setTableData(logService.filterByDate(from, to));
    }

    @FXML
    public void handleClearFilters() {
        if (searchField    != null) searchField.clear();
        if (fromDatePicker != null) fromDatePicker.setValue(null);
        if (toDatePicker   != null) toDatePicker.setValue(null);
        loadAll();
    }

    // ── Export ────────────────────────────────────────────────────────────────

    @FXML
    public void handleExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Activity Logs to CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("activity_logs_export.csv");
        var file = chooser.showSaveDialog(logsTable != null ? logsTable.getScene().getWindow() : null);
        if (file == null) return;

        boolean ok = logService.exportLogsToCSV(file.getAbsolutePath());
        if (ok) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Logs exported to:\n" + file.getAbsolutePath());
            a.showAndWait();
        } else {
            Alert a = new Alert(Alert.AlertType.ERROR, "Export failed. Check file permissions.");
            a.showAndWait();
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupTable() {
        if (colId         != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colAdmin      != null) colAdmin.setCellValueFactory(new PropertyValueFactory<>("adminName"));
        if (colAction     != null) colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        if (colTargetType != null) colTargetType.setCellValueFactory(new PropertyValueFactory<>("targetType"));
        if (colTargetName != null) colTargetName.setCellValueFactory(new PropertyValueFactory<>("targetName"));
        if (colStatus     != null) colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (colTimestamp  != null) colTimestamp.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCreatedAt() != null
                        ? data.getValue().getCreatedAt().format(DISPLAY_FMT) : ""));

        if (logsTable != null) {
            logsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            logsTable.setPlaceholder(new Label("No activity logs found."));
        }
    }

    private void setTableData(List<ActivityLog> logs) {
        if (logsTable   != null) logsTable.setItems(FXCollections.observableArrayList(logs));
        if (lblTotalCount != null) lblTotalCount.setText(logs.size() + " log" + (logs.size() != 1 ? "s" : ""));
    }
}
