package com.studybuddy.admin.controllers;

import com.studybuddy.models.User;
import com.studybuddy.admin.services.AdminService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminUsersController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> userIdCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> userRoleCol;
    @FXML private TableColumn<User, String> statusCol;

    @FXML private Label lblPageNumber;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    private final AdminService adminService = AdminService.getInstance();
    private ObservableList<User> masterUserList = FXCollections.observableArrayList();
    private List<User> filteredList = new ArrayList<>();

    private int currentPage = 1;
    private final int itemsPerPage = 8;

    @FXML
    public void initialize() {
        // Setup table column bindings
        userIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        userRoleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Setup filter dropdowns
        roleFilter.setItems(FXCollections.observableArrayList("STUDENT", "ADMIN"));
        statusFilter.setItems(FXCollections.observableArrayList("Active", "Suspended", "Banned"));

        loadUsersData();
        setupListeners();
    }

    private void loadUsersData() {
        masterUserList.setAll(adminService.getUsers());
        filteredList = new ArrayList<>(masterUserList);
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

        List<User> pageItems = new ArrayList<>();
        if (fromIndex < totalItems) {
            pageItems = filteredList.subList(fromIndex, toIndex);
        }

        usersTable.setItems(FXCollections.observableArrayList(pageItems));
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
        String role = roleFilter.getValue();
        String status = statusFilter.getValue();

        filteredList = masterUserList.stream()
                .filter(u -> (query.isEmpty() || u.getName().toLowerCase().contains(query) || u.getEmail().toLowerCase().contains(query)) &&
                             (role == null || u.getRole().equalsIgnoreCase(role)) &&
                             (status == null || u.getStatus().equalsIgnoreCase(status)))
                .collect(Collectors.toList());

        currentPage = 1;
        updateTablePage();
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        roleFilter.getSelectionModel().clearSelection();
        statusFilter.getSelectionModel().clearSelection();
        filteredList = new ArrayList<>(masterUserList);
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
    public void handleToggleUserStatus() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a user to toggle status.");
            return;
        }

        String oldStatus = selected.getStatus();
        String newStatus = "Active".equalsIgnoreCase(oldStatus) ? "Suspended" : "Active";

        if ("Active".equalsIgnoreCase(newStatus)) {
            adminService.activateUser(selected.getId());
        } else {
            adminService.suspendUser(selected.getId());
        }

        selected.setStatus(newStatus);
        usersTable.refresh();
    }

    @FXML
    public void handleBanUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a user to ban.");
            return;
        }

        adminService.banUser(selected.getId());
        selected.setStatus("Banned");
        usersTable.refresh();
    }

    @FXML
    public void handleEditUserInfo() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a user to edit.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit User Info");
        dialog.setHeaderText("Modify details for: " + selected.getName());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField name = new TextField(selected.getName());
        TextField email = new TextField(selected.getEmail());
        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList("STUDENT", "ADMIN"));
        role.setValue(selected.getRole());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(email, 1, 1);
        grid.add(new Label("Role:"), 0, 2);
        grid.add(role, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                String updatedName = name.getText().trim();
                String updatedEmail = email.getText().trim();
                String updatedRole = role.getValue();

                if (updatedName.isEmpty() || updatedEmail.isEmpty()) {
                    showAlert("Error", "Name and email cannot be empty.");
                    return;
                }

                adminService.editUserInfo(selected.getId(), updatedName, updatedEmail, updatedRole);
                selected.setName(updatedName);
                selected.setEmail(updatedEmail);
                selected.setRole(updatedRole);
                usersTable.refresh();
            }
        });
    }

    @FXML
    public void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a user to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete user " + selected.getName() + " permanently?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                adminService.deleteUser(selected.getId());
                masterUserList.remove(selected);
                loadUsersData();
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
