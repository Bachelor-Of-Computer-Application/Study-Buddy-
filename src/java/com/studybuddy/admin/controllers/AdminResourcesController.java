package com.studybuddy.admin.controllers;

import com.studybuddy.models.Resource;
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

public class AdminResourcesController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<Resource> resourcesTable;
    @FXML private TableColumn<Resource, Integer> resourceIdCol;
    @FXML private TableColumn<Resource, String> titleCol;
    @FXML private TableColumn<Resource, String> subjectCol;
    @FXML private TableColumn<Resource, Integer> uploadedByCol;
    @FXML private TableColumn<Resource, String> dateCol;
    @FXML private TableColumn<Resource, String> resourceStatusCol;

    @FXML private Label lblPageNumber;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    private final AdminService adminService = AdminService.getInstance();
    private ObservableList<Resource> masterResourceList = FXCollections.observableArrayList();
    private List<Resource> filteredList = new ArrayList<>();

    private int currentPage = 1;
    private final int itemsPerPage = 8;

    @FXML
    public void initialize() {
        // Setup table column bindings
        resourceIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        uploadedByCol.setCellValueFactory(new PropertyValueFactory<>("uploadedBy"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
        resourceStatusCol.setCellValueFactory(cellData -> {
            boolean active = cellData.getValue().isActive();
            return new SimpleStringProperty(active ? "Approved" : "Pending");
        });

        // Setup filter dropdowns
        subjectFilter.setItems(FXCollections.observableArrayList("Mathematics", "Physics", "Chemistry", "Computer Science", "Biology", "English"));
        statusFilter.setItems(FXCollections.observableArrayList("Approved", "Pending"));

        loadResourcesData();
        setupListeners();
    }

    private void loadResourcesData() {
        masterResourceList.setAll(adminService.getResources());
        filteredList = new ArrayList<>(masterResourceList);
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

        List<Resource> pageItems = new ArrayList<>();
        if (fromIndex < totalItems) {
            pageItems = filteredList.subList(fromIndex, toIndex);
        }

        resourcesTable.setItems(FXCollections.observableArrayList(pageItems));
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

        filteredList = masterResourceList.stream()
                .filter(r -> (query.isEmpty() || r.getTitle().toLowerCase().contains(query) || r.getSubject().toLowerCase().contains(query)) &&
                             (subject == null || r.getSubject().equalsIgnoreCase(subject)) &&
                             (status == null || matchesStatus(r.isActive(), status)))
                .collect(Collectors.toList());

        currentPage = 1;
        updateTablePage();
    }

    private boolean matchesStatus(boolean isActive, String status) {
        if ("Approved".equalsIgnoreCase(status)) return isActive;
        if ("Pending".equalsIgnoreCase(status)) return !isActive;
        return true;
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        subjectFilter.getSelectionModel().clearSelection();
        statusFilter.getSelectionModel().clearSelection();
        filteredList = new ArrayList<>(masterResourceList);
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
    public void handleApproveResource() {
        Resource selected = resourcesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a resource to approve.");
            return;
        }

        adminService.approveResource(selected.getId());
        selected.setActive(true);
        resourcesTable.refresh();
        showAlert("Success", "Resource '" + selected.getTitle() + "' approved successfully.");
    }

    @FXML
    public void handleRejectResource() {
        Resource selected = resourcesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a resource to reject.");
            return;
        }

        adminService.rejectResource(selected.getId());
        selected.setActive(false);
        resourcesTable.refresh();
        showAlert("Success", "Resource '" + selected.getTitle() + "' status set to Pending / Rejected.");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
