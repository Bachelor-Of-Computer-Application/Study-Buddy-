package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.AdminService;
import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;
import com.studybuddy.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class AdminUserDetailsController {

    @FXML private Label lblUserName;
    @FXML private Label lblUserEmail;
    @FXML private Label lblFullName;
    @FXML private Label lblName;
    @FXML private Label lblEmail;
    @FXML private Label lblRegDate;
    @FXML private Label lblLastLogin;
    @FXML private Label lblDepartment;
    @FXML private Label lblSemester;
    @FXML private Label lblRole;
    @FXML private Label lblStatus;
    @FXML private Label lblPoints;
    @FXML private Label lblTotalNotes;
    @FXML private Label lblTotalResources;
    @FXML private Label lblTotalQuestions;

    @FXML private TableView<Map<String, Object>> tblRecentUploads;
    @FXML private TableColumn<Map<String, Object>, String> colItemType;
    @FXML private TableColumn<Map<String, Object>, String> colItemTitle;
    @FXML private TableColumn<Map<String, Object>, String> colItemStatus;
    @FXML private TableColumn<Map<String, Object>, String> colItemDate;

    @FXML private TableView<Map<String, Object>> tblApprovalHistory;
    @FXML private TableColumn<Map<String, Object>, String> colHistoryType;
    @FXML private TableColumn<Map<String, Object>, String> colHistoryTitle;
    @FXML private TableColumn<Map<String, Object>, String> colHistoryStatus;
    @FXML private TableColumn<Map<String, Object>, String> colHistoryDate;

    private AdminService adminService;
    private int userId;
    private Stage dialogStage;

    public AdminUserDetailsController() {
        this.adminService = AdminService.getInstance();
    }

    public void setUserId(int userId) {
        this.userId = userId;
        loadUserDetails();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    private void loadUserDetails() {
        User user = adminService.getUserById(userId);
        if (user == null) {
            return;
        }

        lblUserName.setText(user.getFullName() != null ? user.getFullName() : user.getName());
        lblUserEmail.setText(user.getEmail());
        lblFullName.setText(user.getFullName() != null ? user.getFullName() : "-");
        lblName.setText(user.getName());
        lblEmail.setText(user.getEmail());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (user.getCreatedAt() != null) {
            lblRegDate.setText(user.getCreatedAt().format(formatter));
        } else {
            lblRegDate.setText("-");
        }
        if (user.getLastLogin() != null) {
            lblLastLogin.setText(user.getLastLogin().format(formatter));
        } else {
            lblLastLogin.setText("-");
        }

        lblDepartment.setText(user.getDepartment() != null ? user.getDepartment() : "-");
        lblSemester.setText(user.getSemester() != null ? user.getSemester() : "-");
        lblRole.setText(user.getRole());
        lblStatus.setText(user.getStatus());
        lblPoints.setText(String.valueOf(user.getPoints()));

        int totalNotes = adminService.getUserTotalNotes(userId);
        int totalResources = adminService.getUserTotalResources(userId);
        int totalQuestions = adminService.getUserTotalQuestions(userId);
        lblTotalNotes.setText(String.valueOf(totalNotes));
        lblTotalResources.setText(String.valueOf(totalResources));
        lblTotalQuestions.setText(String.valueOf(totalQuestions));

        loadRecentUploads();
        loadApprovalHistory();
    }

    private void loadRecentUploads() {
        ObservableList<Map<String, Object>> uploads = FXCollections.observableArrayList();
        
        List<Note> recentNotes = adminService.getUserRecentNotes(userId, 5);
        for (Note note : recentNotes) {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("type", "Note");
            row.put("title", note.getTitle());
            row.put("status", note.getStatus());
            row.put("date", note.getUploadDate());
            uploads.add(row);
        }
        
        List<Resource> recentResources = adminService.getUserRecentResources(userId, 5);
        for (Resource res : recentResources) {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("type", "Resource");
            row.put("title", res.getTitle());
            row.put("status", res.isActive() ? "Approved" : "Pending");
            row.put("date", res.getUploadDate());
            uploads.add(row);
        }
        
        uploads.sort((a, b) -> {
            String dateA = (String) a.get("date");
            String dateB = (String) b.get("date");
            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateB.compareTo(dateA);
        });

        colItemType.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty((String) cellData.getValue().get("type")));
        colItemTitle.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty((String) cellData.getValue().get("title")));
        colItemStatus.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty((String) cellData.getValue().get("status")));
        colItemDate.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty((String) cellData.getValue().get("date")));
        
        tblRecentUploads.setItems(uploads);
    }

    private void loadApprovalHistory() {
        List<Map<String, Object>> history = adminService.getUserApprovalHistory(userId, 10);
        ObservableList<Map<String, Object>> items = FXCollections.observableArrayList(history);
        
        colHistoryType.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty((String) cellData.getValue().get("itemType")));
        colHistoryTitle.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty((String) cellData.getValue().get("title")));
        colHistoryStatus.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty((String) cellData.getValue().get("status")));
        colHistoryDate.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty((String) cellData.getValue().get("date")));
        
        tblApprovalHistory.setItems(items);
    }

    @FXML
    public void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
