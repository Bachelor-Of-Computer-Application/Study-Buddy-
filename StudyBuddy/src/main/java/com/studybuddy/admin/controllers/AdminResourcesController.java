package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.models.Resource;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resource management: activate/deactivate, delete, preview/download, search, sort, download count.
 */
public class AdminResourcesController {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<Resource>              resourcesTable;
    @FXML private TableColumn<Resource, Integer>   colId;
    @FXML private TableColumn<Resource, String>    colTitle;
    @FXML private TableColumn<Resource, String>    colSubject;
    @FXML private TableColumn<Resource, String>    colUploader;
    @FXML private TableColumn<Resource, Integer>   colDownloads;
    @FXML private TableColumn<Resource, String>    colStatus;
    @FXML private TableColumn<Resource, String>    colDate;

    @FXML private Label  lblPageNumber;
    @FXML private Label  lblTotalCount;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    private final AdminService adminService = AdminService.getInstance();
    private final ObservableList<Resource> masterList = FXCollections.observableArrayList();
    private List<Resource> filteredList = new ArrayList<>();
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    @FXML
    public void initialize() {
        setupColumns();
        setupFilters();
        loadData();
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void loadData() {
        masterList.setAll(adminService.getResources());
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
        String status  = statusFilter.getValue();

        filteredList = masterList.stream()
            .filter(r -> q.isEmpty()
                    || nullSafe(r.getTitle()).toLowerCase().contains(q)
                    || nullSafe(r.getSubject()).toLowerCase().contains(q)
                    || nullSafe(r.getSource()).toLowerCase().contains(q))
            .filter(r -> subject == null || subject.isEmpty() || nullSafe(r.getSubject()).equalsIgnoreCase(subject))
            .filter(r -> {
                if (status == null || status.isEmpty()) return true;
                return "Active".equalsIgnoreCase(status) ? r.isActive() : !r.isActive();
            })
            .collect(Collectors.toList());

        currentPage = 1;
        updateTable();
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        subjectFilter.getSelectionModel().clearSelection();
        statusFilter.getSelectionModel().clearSelection();
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
        resourcesTable.setItems(FXCollections.observableArrayList(
                from < total ? filteredList.subList(from, to) : List.of()));

        if (lblPageNumber != null) lblPageNumber.setText("Page " + currentPage + " of " + maxPages);
        if (lblTotalCount != null) lblTotalCount.setText(total + " resource" + (total != 1 ? "s" : ""));
        if (btnPrevPage   != null) btnPrevPage.setDisable(currentPage == 1);
        if (btnNextPage   != null) btnNextPage.setDisable(currentPage == maxPages);
    }

    private int maxPages() { return Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE)); }

    // ── Resource Actions ──────────────────────────────────────────────────────

    @FXML
    public void handleActivate() {
        Resource r = selected(); if (r == null) return;
        boolean ok = adminService.activateResource(r.getId(), r.getTitle());
        if (ok) { r.setActive(true); resourcesTable.refresh(); info("Resource activated."); }
    }

    @FXML
    public void handleDeactivate() {
        Resource r = selected(); if (r == null) return;
        boolean ok = adminService.deactivateResource(r.getId(), r.getTitle());
        if (ok) { r.setActive(false); resourcesTable.refresh(); }
    }

    @FXML
    public void handleDelete() {
        Resource r = selected(); if (r == null) return;
        if (confirm("Delete resource '" + r.getTitle() + "'? This cannot be undone.")) {
            boolean ok = adminService.deleteResource(r.getId(), r.getTitle());
            if (ok) { masterList.remove(r); filteredList.remove(r); updateTable(); }
        }
    }

    /** Open the resource file in the system default application. */
    @FXML
    public void handlePreview() {
        Resource r = selected(); if (r == null) return;
        if (r.getFilePath() == null || r.getFilePath().isBlank()) {
            warn("No file path available for this resource.");
            return;
        }
        try {
            File file = new File(r.getFilePath());
            if (!file.exists()) { warn("File not found:\n" + r.getFilePath()); return; }
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file);
        } catch (IOException e) {
            warn("Could not open file: " + e.getMessage());
        }
    }

    /** Copy/download the resource file using a save dialog. */
    @FXML
    public void handleDownload() {
        Resource r = selected(); if (r == null) return;
        if (r.getFilePath() == null || r.getFilePath().isBlank()) {
            warn("No file path available."); return;
        }
        File source = new File(r.getFilePath());
        if (!source.exists()) { warn("Source file not found."); return; }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Resource As");
        chooser.setInitialFileName(source.getName());
        File dest = chooser.showSaveDialog(resourcesTable.getScene().getWindow());
        if (dest == null) return;
        try {
            java.nio.file.Files.copy(source.toPath(), dest.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            info("Saved to: " + dest.getAbsolutePath());
        } catch (IOException e) {
            warn("Download failed: " + e.getMessage());
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupColumns() {
        if (colId        != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colTitle     != null) colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (colSubject   != null) colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (colUploader  != null) colUploader.setCellValueFactory(new PropertyValueFactory<>("source"));
        if (colDownloads != null) colDownloads.setCellValueFactory(new PropertyValueFactory<>("downloads"));
        if (colDate      != null) colDate.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
        if (colStatus    != null) colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isActive() ? "✅ Active" : "⏸ Inactive"));

        // Allow sorting by downloads
        if (colDownloads != null) colDownloads.setSortable(true);
    }

    private void setupFilters() {
        if (subjectFilter != null) subjectFilter.setItems(FXCollections.observableArrayList(
                "", "Mathematics", "Physics", "Chemistry", "Computer Science", "Biology", "English", "Other"));
        if (statusFilter  != null) statusFilter.setItems(FXCollections.observableArrayList(
                "", "Active", "Inactive"));
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private Resource selected() {
        Resource r = resourcesTable.getSelectionModel().getSelectedItem();
        if (r == null) { warn("Please select a resource first."); }
        return r;
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void info(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void warn(String msg) { Alert a = new Alert(Alert.AlertType.WARNING, msg); a.setHeaderText(null); a.showAndWait(); }
    private String nullSafe(String s) { return s != null ? s : ""; }
}
