package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.models.Note;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Notes moderation: view ALL notes (public + private), approve, reject,
 * make public/private, delete, bulk actions, search/filter.
 */
public class AdminNotesController {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> visibilityFilter;

    @FXML private TableView<Note>             notesTable;
    @FXML private TableColumn<Note, Integer>  colId;
    @FXML private TableColumn<Note, String>   colTitle;
    @FXML private TableColumn<Note, String>   colSubject;
    @FXML private TableColumn<Note, String>   colUploader;
    @FXML private TableColumn<Note, String>   colDate;
    @FXML private TableColumn<Note, String>   colVisibility;
    @FXML private TableColumn<Note, String>   colStatus;

    @FXML private Label  lblPageNumber;
    @FXML private Label  lblTotalCount;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    private final AdminService adminService = AdminService.getInstance();
    private final ObservableList<Note> masterList = FXCollections.observableArrayList();
    private List<Note> filteredList = new ArrayList<>();
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
        masterList.setAll(adminService.getNotes());
        filteredList = new ArrayList<>(masterList);
        currentPage  = 1;
        updateTable();
    }

    @FXML public void handleRefresh() { loadData(); }

    // ── Filtering ─────────────────────────────────────────────────────────────

    @FXML
    public void applyFilters() {
        String q          = searchField.getText().trim().toLowerCase();
        String subject    = subjectFilter.getValue();
        String status     = statusFilter.getValue();
        String visibility = visibilityFilter != null ? visibilityFilter.getValue() : null;

        filteredList = masterList.stream()
            .filter(n -> q.isEmpty()
                    || nullSafe(n.getTitle()).toLowerCase().contains(q)
                    || nullSafe(n.getSubject()).toLowerCase().contains(q)
                    || nullSafe(n.getSource()).toLowerCase().contains(q))
            .filter(n -> subject == null || subject.isEmpty() || nullSafe(n.getSubject()).equalsIgnoreCase(subject))
            .filter(n -> status  == null || status.isEmpty()  || nullSafe(n.getStatus()).equalsIgnoreCase(status))
            .filter(n -> {
                if (visibility == null || visibility.isEmpty()) return true;
                if ("Public".equalsIgnoreCase(visibility))  return !n.isPrivate();
                if ("Private".equalsIgnoreCase(visibility)) return n.isPrivate();
                return true;
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
        if (visibilityFilter != null) visibilityFilter.getSelectionModel().clearSelection();
        filteredList = new ArrayList<>(masterList);
        currentPage  = 1;
        updateTable();
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @FXML public void handlePrevPage() { if (currentPage > 1) { currentPage--; updateTable(); } }

    @FXML public void handleNextPage() {
        if (currentPage < maxPages()) { currentPage++; updateTable(); }
    }

    private void updateTable() {
        int total    = filteredList.size();
        int maxPages = maxPages();
        currentPage  = Math.max(1, Math.min(currentPage, maxPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        notesTable.setItems(FXCollections.observableArrayList(
                from < total ? filteredList.subList(from, to) : List.of()));

        if (lblPageNumber != null) lblPageNumber.setText("Page " + currentPage + " of " + maxPages);
        if (lblTotalCount != null) lblTotalCount.setText(total + " note" + (total != 1 ? "s" : ""));
        if (btnPrevPage   != null) btnPrevPage.setDisable(currentPage == 1);
        if (btnNextPage   != null) btnNextPage.setDisable(currentPage == maxPages);
    }

    private int maxPages() { return Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE)); }

    // ── Note Actions ──────────────────────────────────────────────────────────

    @FXML
    public void handleApprove() {
        Note n = selected(); if (n == null) return;
        if (adminService.approveNote(n.getId(), n.getTitle())) {
            n.setStatus("Approved"); notesTable.refresh();
            info("Note '" + n.getTitle() + "' approved.");
        }
    }

    @FXML
    public void handleReject() {
        Note n = selected(); if (n == null) return;
        if (adminService.rejectNote(n.getId(), n.getTitle())) {
            n.setStatus("Rejected"); notesTable.refresh();
        }
    }

    @FXML
    public void handleMakePublic() {
        Note n = selected(); if (n == null) return;
        if (adminService.makeNotePublic(n.getId(), n.getTitle())) {
            n.setPrivate(false); notesTable.refresh();
            info("Note made public.");
        }
    }

    @FXML
    public void handleMakePrivate() {
        Note n = selected(); if (n == null) return;
        if (adminService.makeNotePrivate(n.getId(), n.getTitle())) {
            n.setPrivate(true); notesTable.refresh();
        }
    }

    @FXML
    public void handleDelete() {
        Note n = selected(); if (n == null) return;
        if (confirm("Delete note '" + n.getTitle() + "'?")) {
            boolean ok = adminService.deleteNote(n.getId(), n.getTitle());
            if (ok) { masterList.remove(n); filteredList.remove(n); updateTable(); }
        }
    }

    /** Approve all currently selected notes. */
    @FXML
    public void handleBulkApprove() {
        List<Note> selected = new ArrayList<>(notesTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) { warn("No notes selected for bulk action."); return; }
        selected.forEach(n -> {
            adminService.approveNote(n.getId(), n.getTitle());
            n.setStatus("Approved");
        });
        notesTable.refresh();
        info("Bulk approved " + selected.size() + " notes.");
    }

    /** Delete all currently selected notes. */
    @FXML
    public void handleBulkDelete() {
        List<Note> selected = new ArrayList<>(notesTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) { warn("No notes selected for bulk action."); return; }
        if (!confirm("Delete " + selected.size() + " selected notes?")) return;
        selected.forEach(n -> adminService.deleteNote(n.getId(), n.getTitle()));
        masterList.removeAll(selected);
        filteredList.removeAll(selected);
        updateTable();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupColumns() {
        notesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (colId         != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colTitle      != null) colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (colSubject    != null) colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        if (colUploader   != null) colUploader.setCellValueFactory(new PropertyValueFactory<>("source"));
        if (colDate       != null) colDate.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
        if (colVisibility != null) colVisibility.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isPrivate() ? "🔒 Private" : "🌐 Public"));
        if (colStatus     != null) colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void setupFilters() {
        if (subjectFilter    != null) subjectFilter.setItems(FXCollections.observableArrayList(
                "", "Mathematics", "Physics", "Chemistry", "Computer Science", "Biology", "English", "Other"));
        if (statusFilter     != null) statusFilter.setItems(FXCollections.observableArrayList(
                "", "Pending", "Approved", "Rejected"));
        if (visibilityFilter != null) visibilityFilter.setItems(FXCollections.observableArrayList(
                "", "Public", "Private"));
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private Note selected() {
        Note n = notesTable.getSelectionModel().getSelectedItem();
        if (n == null) { warn("Please select a note first."); }
        return n;
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
