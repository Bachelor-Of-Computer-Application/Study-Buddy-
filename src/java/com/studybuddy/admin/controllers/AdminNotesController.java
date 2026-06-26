package com.studybuddy.admin.controllers;

import com.studybuddy.models.Note;
import com.studybuddy.admin.services.AdminService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminNotesController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<Note> notesTable;
    @FXML private TableColumn<Note, Integer> noteIdCol;
    @FXML private TableColumn<Note, String> titleCol;
    @FXML private TableColumn<Note, String> subjectCol;
    @FXML private TableColumn<Note, Integer> authorCol;
    @FXML private TableColumn<Note, String> dateCol;
    @FXML private TableColumn<Note, String> statusCol;

    @FXML private Label lblPageNumber;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    private final AdminService adminService = AdminService.getInstance();
    private ObservableList<Note> masterNotesList = FXCollections.observableArrayList();
    private List<Note> filteredList = new ArrayList<>();

    private int currentPage = 1;
    private final int itemsPerPage = 8;

    @FXML
    public void initialize() {
        // Setup table column bindings
        noteIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        authorCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));

        // Setup filter dropdowns
        subjectFilter.setItems(FXCollections.observableArrayList("Mathematics", "Physics", "Chemistry", "Computer Science", "Biology", "English"));
        statusFilter.setItems(FXCollections.observableArrayList("Approved", "Pending", "Rejected"));

        loadNotesData();
        setupListeners();
    }

    private void loadNotesData() {
        masterNotesList.setAll(adminService.getNotes());
        filteredList = new ArrayList<>(masterNotesList);
        currentPage = 1;
        updateTablePage();
    }

    private void updateTablePage() {
        int totalItems = filteredList.size();
        int maxPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (maxPages == 0) maxPages = 1;

        if (currentPage > maxPages) currentPage = maxPages;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, totalItems);

        List<Note> pageItems = new ArrayList<>();
        if (fromIndex < totalItems) {
            pageItems = filteredList.subList(fromIndex, toIndex);
        }

        notesTable.setItems(FXCollections.observableArrayList(pageItems));
        lblPageNumber.setText(String.format("Page %d of %d", currentPage, maxPages));

        btnPrevPage.setDisable(currentPage == 1);
        btnNextPage.setDisable(currentPage == maxPages);
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    @FXML
    public void applyFilters() {
        String query = searchField.getText().trim().toLowerCase();
        String subject = subjectFilter.getValue();
        String status = statusFilter.getValue();

        filteredList = masterNotesList.stream()
                .filter(n -> (query.isEmpty() || n.getTitle().toLowerCase().contains(query) || n.getSubject().toLowerCase().contains(query)) &&
                             (subject == null || n.getSubject().equalsIgnoreCase(subject)) &&
                             (status == null || n.getStatus().equalsIgnoreCase(status)))
                .collect(Collectors.toList());

        currentPage = 1;
        updateTablePage();
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        subjectFilter.getSelectionModel().clearSelection();
        statusFilter.getSelectionModel().clearSelection();
        filteredList = new ArrayList<>(masterNotesList);
        currentPage = 1;
        updateTablePage();
    }

    @FXML
    public void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            updateTablePage();
        }
    }

    @FXML
    public void handleNextPage() {
        int maxPages = (int) Math.ceil((double) filteredList.size() / itemsPerPage);
        if (currentPage < maxPages) {
            currentPage++;
            updateTablePage();
        }
    }

    @FXML
    public void handleApproveNote() {
        Note selected = notesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a note to approve.");
            return;
        }

        adminService.approveNote(selected.getId());
        loadNotesData();
        showAlert("Success", "Note '" + selected.getTitle() + "' approved and published successfully.");
    }

    @FXML
    public void handleRejectNote() {
        Note selected = notesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a note to reject.");
            return;
        }

        adminService.rejectNote(selected.getId());
        loadNotesData();
        showAlert("Rejected", "Note '" + selected.getTitle() + "' rejected.");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
