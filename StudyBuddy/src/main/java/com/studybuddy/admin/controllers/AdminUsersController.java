package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.services.AcademicService;
import com.studybuddy.models.Department;
import com.studybuddy.models.Semester;
import com.studybuddy.models.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Full-featured user management controller.
 * Features: search, role/dept filter, edit, reset password, suspend/activate,
 * promote/demote, soft-delete, CSV export. Every action is activity-logged.
 */
public class AdminUsersController {

    @FXML private TextField         searchField;
    @FXML private ComboBox<String>  roleFilter;
    @FXML private ComboBox<String>  departmentFilter;
    @FXML private ComboBox<String>  semesterFilter;
    @FXML private ComboBox<String>  statusFilter;

    @FXML private TableView<User>             usersTable;
    @FXML private TableColumn<User, String>   colUsername;
    @FXML private TableColumn<User, String>   colFullName;
    @FXML private TableColumn<User, String>   colEmail;
    @FXML private TableColumn<User, String>   colDepartment;
    @FXML private TableColumn<User, String>   colSemester;
    @FXML private TableColumn<User, String>   colRole;
    @FXML private TableColumn<User, String>   colStatus;
    @FXML private TableColumn<User, String>   colCreatedAt;

    @FXML private Label  lblPageNumber;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label  lblTotalCount;

    private final AdminService adminService = AdminService.getInstance();
    private final AcademicService academicService = AcademicService.getInstance();
    private final ObservableList<User> masterList = FXCollections.observableArrayList();
    private List<User> filteredList = new ArrayList<>();
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    /**
     * Guard flag: true while a hard-delete background thread is running.
     *
     * Purpose: AdminService.hardDeleteUser() publishes AdminChangesEvent
     * synchronously on the background thread.  EventBus dispatches that event
     * to our subscriber which queues Platform.runLater(this::loadData).
     * If that runLater fires while successAlert.showAndWait() is blocking the
     * FX thread, JavaFX pumps the event queue and runs loadData() — a SQL
     * query — on the FX thread, causing the UI to freeze / deadlock.
     *
     * Setting this flag true before the deletion thread starts and false after
     * the success dialog closes makes the EventBus subscriber a no-op during
     * the entire deletion window.  A clean reload is then scheduled as a
     * separate background Task after the dialog returns.
     */
    private volatile boolean deletionInProgress = false;

    @FXML
    public void initialize() {
        setupColumns();
        setupFilters();
        loadData();
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        if (usersTable != null) {
            usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }
        
        // Subscribe to EventBus for real-time updates.
        // IMPORTANT: the listener must NOT call loadData() while a deletion is
        // in progress.  hardDeleteUser() publishes AdminChangesEvent on the
        // background thread; that event is dispatched synchronously, queuing
        // Platform.runLater(loadData) before our own runLater(uiUpdate) has
        // finished.  When successAlert.showAndWait() pumps the FX queue it
        // would execute loadData() — a SQL call — on the FX thread, causing
        // the freeze.  The deletionInProgress flag suppresses that reload; a
        // clean background reload is scheduled explicitly after deletion ends.
        com.studybuddy.utils.EventBus.getInstance().subscribe(
            com.studybuddy.utils.EventBus.AdminChangesEvent.class,
            (_event) -> {
                if (!deletionInProgress) {
                    javafx.application.Platform.runLater(this::loadData);
                }
            }
        );
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    private void loadData() {
        masterList.setAll(adminService.getUsers());
        filteredList = new ArrayList<>(masterList);
        currentPage  = 1;
        updateTable();
    }

    @FXML public void handleRefresh() { loadData(); }

    // ── Filtering ─────────────────────────────────────────────────────────────

    @FXML
    public void applyFilters() {
        String q      = searchField.getText().trim().toLowerCase();
        String role   = roleFilter.getValue();
        String dept   = departmentFilter.getValue();
        String sem    = semesterFilter != null ? semesterFilter.getValue() : null;
        String status = statusFilter.getValue();

        filteredList = masterList.stream()
            .filter(u -> q.isEmpty()
                    || nullSafe(u.getUsername()).toLowerCase().contains(q)
                    || nullSafe(u.getDisplayFullName()).toLowerCase().contains(q)
                    || nullSafe(u.getName()).toLowerCase().contains(q)
                    || nullSafe(u.getEmail()).toLowerCase().contains(q))
            .filter(u -> role   == null || role.isEmpty()   || nullSafe(u.getRole()).equalsIgnoreCase(role))
            .filter(u -> dept   == null || dept.isEmpty()   || nullSafe(u.getDepartment()).equalsIgnoreCase(dept))
            .filter(u -> sem    == null || sem.isEmpty()    || matchesSemesterFilter(u, sem))
            .filter(u -> status == null || status.isEmpty() || nullSafe(u.getStatus()).equalsIgnoreCase(status))
            .collect(Collectors.toList());

        currentPage = 1;
        updateTable();
    }

    private boolean matchesSemesterFilter(User u, String semFilter) {
        String userSem = nullSafe(u.getSemester());
        if (userSem.equalsIgnoreCase(semFilter)) return true;
        try {
            for (Semester s : academicService.getAllSemesters()) {
                if (s.getName().equalsIgnoreCase(semFilter)
                        || String.valueOf(s.getSemesterNumber()).equals(semFilter)) {
                    return userSem.equalsIgnoreCase(s.getName())
                            || userSem.equals(String.valueOf(s.getSemesterNumber()));
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        roleFilter.getSelectionModel().clearSelection();
        departmentFilter.getSelectionModel().clearSelection();
        if (semesterFilter != null) semesterFilter.getSelectionModel().clearSelection();
        statusFilter.getSelectionModel().clearSelection();
        filteredList = new ArrayList<>(masterList);
        currentPage  = 1;
        updateTable();
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @FXML public void handlePrevPage() { if (currentPage > 1) { currentPage--; updateTable(); } }

    @FXML public void handleNextPage() {
        int maxPages = maxPages();
        if (currentPage < maxPages) { currentPage++; updateTable(); }
    }

    private void updateTable() {
        int total    = filteredList.size();
        int maxPages = maxPages();
        if (currentPage > maxPages) currentPage = maxPages;
        if (currentPage < 1)       currentPage = 1;

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        List<User> page = from < total ? filteredList.subList(from, to) : List.of();
        usersTable.setItems(FXCollections.observableArrayList(page));

        if (lblPageNumber != null) lblPageNumber.setText("Page " + currentPage + " of " + maxPages);
        if (lblTotalCount != null) lblTotalCount.setText(total + " user" + (total != 1 ? "s" : ""));
        if (btnPrevPage   != null) btnPrevPage.setDisable(currentPage == 1);
        if (btnNextPage   != null) btnNextPage.setDisable(currentPage == maxPages);
    }

    private int maxPages() {
        int p = (int) Math.ceil((double) filteredList.size() / PAGE_SIZE);
        return Math.max(p, 1);
    }

    // ── User Actions ──────────────────────────────────────────────────────────

    @FXML
    public void handleSuspend() {
        User u = selectedUser(); if (u == null) return;
        if (confirm("Suspend user " + u.getName() + "?")) {
            adminService.suspendUser(u.getId(), u.getName());
            u.setStatus("Suspended"); usersTable.refresh();
        }
    }

    @FXML
    public void handleActivate() {
        User u = selectedUser(); if (u == null) return;
        if (confirm("Activate user " + u.getName() + "?")) {
            adminService.activateUser(u.getId(), u.getName());
            u.setStatus("Active"); usersTable.refresh();
        }
    }

    @FXML
    public void handleBan() {
        User u = selectedUser(); if (u == null) return;
        if (confirm("Ban user " + u.getName() + "? This cannot be undone without direct DB access.")) {
            adminService.banUser(u.getId(), u.getName());
            u.setStatus("Banned"); usersTable.refresh();
        }
    }

    @FXML
    public void handleEdit() {
        User u = selectedUser(); if (u == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit: " + u.getName());
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameF  = new TextField(nullSafe(u.getDisplayFullName()));
        TextField usernameF = new TextField(nullSafe(u.getUsername()));
        usernameF.setDisable(true); // Username should not be editable
        TextField emailF = new TextField(nullSafe(u.getEmail()));
        ComboBox<Department> deptC = new ComboBox<>(FXCollections.observableArrayList(academicService.getAllActiveDepartments()));
        deptC.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Department item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        deptC.setButtonCell(deptC.getCellFactory().call(null));
        ComboBox<Semester> semC = new ComboBox<>();
        semC.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Semester item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        semC.setButtonCell(semC.getCellFactory().call(null));
        for (Department d : deptC.getItems()) {
            if (d.getName().equalsIgnoreCase(nullSafe(u.getDepartment()))
                    || d.getCode().equalsIgnoreCase(nullSafe(u.getDepartment()))) {
                deptC.setValue(d);
                semC.setItems(FXCollections.observableArrayList(academicService.getSemestersByDepartment(d.getId())));
                break;
            }
        }
        for (Semester s : semC.getItems()) {
            if (s.getName().equalsIgnoreCase(nullSafe(u.getSemester()))
                    || String.valueOf(s.getSemesterNumber()).equals(nullSafe(u.getSemester()))) {
                semC.setValue(s);
                break;
            }
        }
        deptC.setOnAction(e -> {
            Department d = deptC.getValue();
            semC.getItems().clear();
            if (d != null) semC.setItems(FXCollections.observableArrayList(academicService.getSemestersByDepartment(d.getId())));
        });
        
        ComboBox<String> statusC = new ComboBox<>(FXCollections.observableArrayList("Active", "Suspended", "Banned", "Pending"));
        statusC.setValue(nullSafe(u.getStatus()));

        grid.addRow(0, new Label("Full Name:"), nameF);
        grid.addRow(1, new Label("Username:"), usernameF);
        grid.addRow(2, new Label("Email:"),      emailF);
        grid.addRow(3, new Label("Department:"), deptC);
        grid.addRow(4, new Label("Semester:"),   semC);
        grid.addRow(5, new Label("Status:"),     statusC);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(res -> {
            if (res == saveBtn) {
                if (nameF.getText().isBlank() || emailF.getText().isBlank()) {
                    showAlert(Alert.AlertType.WARNING, "Name and email are required."); return;
                }
                String deptName = deptC.getValue() != null ? deptC.getValue().getName() : "";
                String semLabel = semC.getValue() != null ? semC.getValue().getName() : "";
                // Role is preserved - not editable anymore
                boolean ok = adminService.editUserInfo(u.getId(),
                        nameF.getText().trim(), emailF.getText().trim(),
                        u.getRole(), deptName, semLabel);
                if (ok) {
                    u.setName(nameF.getText().trim());
                    u.setFullName(nameF.getText().trim());
                    u.setEmail(emailF.getText().trim());
                    u.setDepartment(deptName);
                    u.setSemester(semLabel);
                    u.setStatus(statusC.getValue());
                    usersTable.refresh();
                    showAlert(Alert.AlertType.INFORMATION, "User updated successfully.");
                }
            }
        });
    }

    @FXML
    public void handleResetPassword() {
        User u = selectedUser(); if (u == null) return;

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Reset Password");
        dlg.setHeaderText("Reset password for: " + u.getName());
        dlg.setContentText("New password:");
        dlg.showAndWait().ifPresent(pw -> {
            if (pw.trim().length() < 6) {
                showAlert(Alert.AlertType.WARNING, "Password must be at least 6 characters."); return;
            }
            boolean ok = adminService.resetPassword(u.getId(), u.getEmail(), pw.trim());
            showAlert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                    ok ? "Password reset successfully." : "Failed to reset password.");
        });
    }

    @FXML
    public void handleView() {
        User u = selectedUser(); if (u == null) return;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/studybuddy/admin/fxml/AdminUserDetailsDialog.fxml"));
            Parent root = loader.load();
            
            AdminUserDetailsController controller = loader.getController();
            controller.setUserId(u.getId());
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("User Details");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(usersTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            
            controller.setDialogStage(dialogStage);
            
            dialogStage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Failed to load user details dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleDelete() {
        User u = selectedUser(); if (u == null) return;
        
        // Show detailed warning dialog
        Alert confirmDialog = new Alert(Alert.AlertType.WARNING);
        confirmDialog.setTitle("Permanently Delete User");
        confirmDialog.setHeaderText("Are you sure you want to permanently delete this user?");
        
        StringBuilder content = new StringBuilder();
        content.append("User: ").append(u.getName()).append(" (").append(u.getEmail()).append(")\n\n");
        content.append("⚠️ WARNING: This action CANNOT be undone!\n\n");
        content.append("This will permanently delete:\n");
        content.append("• User account\n");
        content.append("• All user's notes\n");
        content.append("• All user's resources\n");
        content.append("• All user's questions\n");
        content.append("• All user's answers\n");
        content.append("• All user's tasks\n");
        content.append("• All user's notifications\n");
        content.append("• All user's activity logs\n");
        content.append("• All associated files\n\n");
        content.append("Are you absolutely sure you want to continue?");
        
        confirmDialog.setContentText(content.toString());
        confirmDialog.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        
        ButtonType result = confirmDialog.showAndWait().orElse(ButtonType.NO);
        if (result != ButtonType.YES) return;
        
        // Show progress indicator (non-blocking – no buttons, dismissed from code)
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Deleting User");
        progressAlert.setHeaderText("Please wait...");
        progressAlert.setContentText("Deleting user and all related data...");
        progressAlert.getButtonTypes().clear();
        progressAlert.show();
        
        System.out.println(">>> UI: Progress dialog shown");

        // Block EventBus-triggered loadData() for the entire deletion window.
        // AdminChangesEvent is published by hardDeleteUser() on the background
        // thread.  Without this flag, that event would queue loadData() via
        // Platform.runLater, which then runs on the FX thread while
        // successAlert.showAndWait() is blocking it — causing a UI freeze.
        deletionInProgress = true;
        
        // Perform deletion in background thread to keep FX thread free
        Thread deletionThread = new Thread(() -> {
            System.out.println(">>> THREAD: Background deletion thread STARTED for user: " + u.getName() + " (ID=" + u.getId() + ")");
            
            com.studybuddy.admin.dao.AdminDAO.DeletionResult deleteResult = null;
            Exception caughtException = null;
            
            try {
                System.out.println(">>> THREAD: Calling adminService.hardDeleteUser()...");
                deleteResult = adminService.hardDeleteUser(u.getId(), u.getName());
                System.out.println(">>> THREAD: adminService.hardDeleteUser() RETURNED. Success=" + (deleteResult != null && deleteResult.success));
            } catch (Exception e) {
                System.out.println(">>> THREAD: Exception caught: " + e.getMessage());
                e.printStackTrace();
                caughtException = e;
            }
            
            // Capture final values for lambda
            final com.studybuddy.admin.dao.AdminDAO.DeletionResult finalResult = deleteResult;
            final Exception finalException = caughtException;
            
            System.out.println(">>> THREAD: Queueing Platform.runLater() for UI update...");

            // ── Platform.runLater: UI UPDATES ONLY — no DB calls, no loadData() ──
            javafx.application.Platform.runLater(() -> {
                System.out.println(">>> UI: Platform.runLater() ENTERED");
                
                try {
                    // 1. Close progress dialog first — must complete before any showAndWait()
                    System.out.println(">>> UI: Closing progress dialog...");
                    progressAlert.close();
                    System.out.println(">>> UI: Progress dialog CLOSED");

                    if (finalException != null) {
                        // ── Exception path ──────────────────────────────────
                        deletionInProgress = false;   // release guard before dialog
                        System.out.println(">>> UI: Showing exception error dialog");
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Error");
                        errorAlert.setHeaderText("Deletion failed");
                        errorAlert.setContentText(finalException.getMessage());
                        errorAlert.showAndWait();

                    } else if (finalResult != null && finalResult.success) {
                        // ── Success path ─────────────────────────────────────
                        // 2. Remove from in-memory lists (pure UI — no SQL)
                        System.out.println(">>> UI: Deletion successful, updating lists...");
                        masterList.remove(u);
                        filteredList.remove(u);
                        updateTable();

                        // 3. Show success dialog — safe: no DB calls queued on FX thread
                        System.out.println(">>> UI: Showing success dialog");
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("User Deleted");
                        successAlert.setHeaderText("User deleted successfully");
                        successAlert.setContentText(finalResult.getSummary());
                        successAlert.showAndWait();
                        System.out.println(">>> UI: Success dialog closed");

                        // 4. Release guard AFTER dialog closes, then reload off-thread
                        deletionInProgress = false;
                        scheduleBackgroundReload();

                    } else {
                        // ── Failure path ─────────────────────────────────────
                        deletionInProgress = false;   // release guard before dialog
                        System.out.println(">>> UI: Deletion failed, showing error dialog");
                        String errorMsg = (finalResult != null && finalResult.errorMessage != null)
                            ? finalResult.errorMessage
                            : "Unknown error occurred";
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Deletion Failed");
                        errorAlert.setHeaderText("Failed to delete user");
                        errorAlert.setContentText(errorMsg);
                        errorAlert.showAndWait();
                    }
                    
                    System.out.println(">>> UI: Platform.runLater() COMPLETED");
                    
                } catch (Exception uiEx) {
                    System.out.println(">>> UI: Exception in Platform.runLater(): " + uiEx.getMessage());
                    uiEx.printStackTrace();
                    deletionInProgress = false;   // always release on unexpected error
                    try { progressAlert.close(); } catch (Exception ignored) {}
                }
            });
            
            System.out.println(">>> THREAD: Background deletion thread FINISHED");
        });
        
        deletionThread.setName("UserDeletionThread-" + u.getId());
        deletionThread.setDaemon(true);
        deletionThread.start();
        System.out.println(">>> UI: Background thread STARTED (thread name: " + deletionThread.getName() + ")");
    }

    /**
     * Reload the user list from the database on a background thread, then
     * apply the result on the FX thread.  Called only after the success dialog
     * has closed so showAndWait() can no longer pump the queue and accidentally
     * run this code on the FX thread.
     *
     * This is the ONLY place where a post-deletion DB reload is permitted.
     * DO NOT call loadData() directly on the FX thread after deletion.
     */
    private void scheduleBackgroundReload() {
        javafx.concurrent.Task<java.util.List<User>> reloadTask = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<User> call() {
                return adminService.getUsers();   // SQL runs off the FX thread
            }
        };

        reloadTask.setOnSucceeded(evt -> {
            // Back on the FX thread — pure UI update, no SQL
            java.util.List<User> fresh = reloadTask.getValue();
            masterList.setAll(fresh);
            filteredList = new java.util.ArrayList<>(masterList);
            currentPage  = 1;
            updateTable();
            System.out.println(">>> UI: Background reload completed (" + fresh.size() + " users)");
        });

        reloadTask.setOnFailed(evt -> {
            System.out.println(">>> UI: Background reload failed: " + reloadTask.getException());
        });

        Thread reloadThread = new Thread(reloadTask, "UserReloadThread");
        reloadThread.setDaemon(true);
        reloadThread.start();
    }

    // ── Export ────────────────────────────────────────────────────────────────

    @FXML
    public void handleExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Users to CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("users_export.csv");
        var file = chooser.showSaveDialog(usersTable.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Username,Full Name,Email,Role,Department,Semester,Status,Created At");
            for (User u : filteredList) {
                pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        esc(u.getUsername()), esc(u.getDisplayFullName()), esc(u.getEmail()), esc(u.getRole()),
                        esc(u.getDepartment()), esc(u.getSemester()), esc(u.getStatus()),
                        u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            }
            showAlert(Alert.AlertType.INFORMATION, "Exported " + filteredList.size() + " users to:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage());
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupColumns() {
        if (colUsername   != null) colUsername.setCellValueFactory(cellData ->
                new SimpleStringProperty(nullSafe(cellData.getValue().getUsername())));
        if (colFullName   != null) colFullName.setCellValueFactory(cellData ->
                new SimpleStringProperty(nullSafe(cellData.getValue().getDisplayFullName())));
        if (colEmail      != null) colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (colDepartment != null) colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        if (colSemester   != null) colSemester.setCellValueFactory(new PropertyValueFactory<>("semester"));
        if (colRole       != null) colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        if (colStatus     != null) colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (colCreatedAt  != null) colCreatedAt.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt() != null
                        ? cellData.getValue().getCreatedAt().toString().substring(0, 10) : ""));
    }

    private void setupFilters() {
        if (roleFilter       != null) roleFilter.setItems(FXCollections.observableArrayList("", "STUDENT", "ADMIN"));
        if (statusFilter     != null) statusFilter.setItems(FXCollections.observableArrayList("", "Active", "Suspended", "Banned", "Deleted", "Pending"));
        if (departmentFilter != null) {
            List<String> deptNames = academicService.getAllActiveDepartments().stream()
                    .map(Department::getName).collect(Collectors.toList());
            deptNames.add(0, "");
            departmentFilter.setItems(FXCollections.observableArrayList(deptNames));
        }
        if (semesterFilter != null) {
            List<String> semNames = new ArrayList<>();
            semNames.add("");
            try {
                academicService.getAllSemesters().stream()
                        .map(Semester::getName)
                        .distinct()
                        .sorted()
                        .forEach(semNames::add);
            } catch (Exception ignored) {}
            semesterFilter.setItems(FXCollections.observableArrayList(semNames));
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private User selectedUser() {
        User u = usersTable.getSelectionModel().getSelectedItem();
        if (u == null) showAlert(Alert.AlertType.WARNING, "Please select a user first.");
        return u;
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg); a.setHeaderText(null); a.showAndWait();
    }

    private String nullSafe(String s) { return s != null ? s : ""; }
    private String esc(String s)      { return s != null ? s.replace("\"", "\"\"") : ""; }
}
