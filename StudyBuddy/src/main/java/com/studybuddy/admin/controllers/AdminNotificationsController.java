package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.NotificationService;
import com.studybuddy.models.Department;
import com.studybuddy.models.Notification;
import com.studybuddy.models.Semester;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.FileStorageService;
import com.studybuddy.utils.AcademicFilterHelper;
import com.studybuddy.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Smart notification management with targeting, types, priority, expiry, attachments.
 */
public class AdminNotificationsController {

    @FXML private TextField titleField;
    @FXML private TextArea messageArea;
    @FXML private ComboBox<String> notificationTypeCombo;
    @FXML private ComboBox<String> priorityCombo;
    @FXML private ComboBox<String> recipientTypeCombo;
    @FXML private TextField recipientValueField;
    @FXML private Label recipientValueLabel;
    @FXML private ComboBox<Department> deptCombo;
    @FXML private ComboBox<Semester> semCombo;
    @FXML private DatePicker expiryDatePicker;
    @FXML private TextField attachmentField;

    @FXML private TextField historySearchField;
    @FXML private ComboBox<String> historyTypeFilter;
    @FXML private TableView<Notification> historyTable;
    @FXML private TableColumn<Notification, String> colTitle;
    @FXML private TableColumn<Notification, String> colRecipient;
    @FXML private TableColumn<Notification, String> colType;
    @FXML private TableColumn<Notification, String> colPriority;
    @FXML private TableColumn<Notification, String> colSentAt;
    @FXML private TableColumn<Notification, String> colRead;
    @FXML private Label lblUnreadCount;

    private File attachmentFile;
    private List<Notification> historyMaster = new ArrayList<>();
    private final NotificationService notificationService = NotificationService.getInstance();
    private final AcademicService academicService = AcademicService.getInstance();

    @FXML
    public void initialize() {
        setupForm();
        setupTable();
        loadHistory();
        if (historySearchField != null) {
            historySearchField.textProperty().addListener((obs, o, n) -> applyHistoryFilters());
        }
        if (historyTypeFilter != null) {
            historyTypeFilter.setOnAction(e -> applyHistoryFilters());
        }
        if (historyTable != null) {
            historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
    }

    private void setupForm() {
        if (notificationTypeCombo != null) {
            notificationTypeCombo.setItems(FXCollections.observableArrayList(
                    "Announcement", "Exam", "Assignment", "Event", "Holiday", "Maintenance", "Emergency"));
            notificationTypeCombo.setValue("Announcement");
        }
        if (priorityCombo != null) {
            priorityCombo.setItems(FXCollections.observableArrayList("Low", "Normal", "High", "Critical"));
            priorityCombo.setValue("Normal");
        }
        if (recipientTypeCombo != null) {
            recipientTypeCombo.setItems(FXCollections.observableArrayList(
                    "COLLEGE", "ALL_STUDENTS", "ALL_FACULTY", "DEPARTMENT", "SEMESTER", "DEPT_SEM", "USER"));
            recipientTypeCombo.setValue("COLLEGE");
            recipientTypeCombo.setOnAction(e -> onRecipientTypeChanged());
        }
        if (deptCombo != null) {
            deptCombo.setItems(AcademicFilterHelper.departmentsForFilter(academicService));
            deptCombo.setValue(AcademicFilterHelper.allDepartments());
            deptCombo.setOnAction(e -> {
                Department d = deptCombo.getValue();
                if (semCombo != null) {
                    semCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, d));
                    semCombo.setValue(AcademicFilterHelper.allSemesters());
                    semCombo.setDisable(AcademicFilterHelper.isAllDepartments(d));
                }
            });
        }
        if (semCombo != null) {
            semCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
            semCombo.setValue(AcademicFilterHelper.allSemesters());
            semCombo.setDisable(true);
        }
        if (historyTypeFilter != null) {
            historyTypeFilter.setItems(FXCollections.observableArrayList(
                    "", "Announcement", "Exam", "Assignment", "Event", "Holiday", "Maintenance", "Emergency"));
        }
        onRecipientTypeChanged();
    }

    @FXML
    public void handleSend() {
        String title = titleField.getText().trim();
        String message = messageArea.getText().trim();
        if (title.isEmpty() || message.isEmpty()) {
            warn("Title and message are required."); return;
        }

        String recipientType = recipientTypeCombo.getValue();
        if (!validateTargeting(recipientType)) return;

        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(message);
        n.setNotificationType(notificationTypeCombo.getValue());
        n.setPriority(priorityCombo.getValue());
        n.setRecipientType(recipientType);
        n.setRecipientValue(recipientValueField.getText().trim());
        n.setDepartmentId(AcademicFilterHelper.resolveDepartmentId(deptCombo.getValue()));
        n.setSemesterId(AcademicFilterHelper.resolveSemesterId(semCombo.getValue()));
        if (expiryDatePicker.getValue() != null) {
            n.setExpiryDate(expiryDatePicker.getValue().atTime(23, 59));
        }
        if (SessionManager.getCurrentAdmin() != null) {
            n.setSentBy(SessionManager.getCurrentAdmin().getId());
        }
        try {
            if (attachmentFile != null) {
                n.setAttachmentPath(FileStorageService.getInstance().storeFile(attachmentFile, "notifications"));
            }
        } catch (Exception e) {
            warn("Attachment failed: " + e.getMessage()); return;
        }

        if (notificationService.sendSmartNotification(n)) {
            clearForm();
            loadHistory();
            info("Notification sent successfully.");
        } else {
            warn("Failed to send. Check recipient targeting and run database/migration_admin_enhancements.sql.");
        }
    }

    private boolean validateTargeting(String recipientType) {
        if (recipientType == null) return true;
        switch (recipientType.toUpperCase()) {
            case "USER":
                if (recipientValueField.getText() == null || recipientValueField.getText().isBlank()) {
                    warn("Enter the recipient user email."); return false;
                }
                break;
            case "DEPARTMENT":
                if (AcademicFilterHelper.isAllDepartments(deptCombo.getValue())) {
                    warn("Select a specific department for department notifications."); return false;
                }
                break;
            case "SEMESTER":
                if (AcademicFilterHelper.isAllSemesters(semCombo.getValue())) {
                    warn("Select a specific semester for semester notifications."); return false;
                }
                break;
            case "DEPT_SEM":
                if (AcademicFilterHelper.isAllDepartments(deptCombo.getValue())
                        || AcademicFilterHelper.isAllSemesters(semCombo.getValue())) {
                    warn("Select both a specific department and semester."); return false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    @FXML public void handleSelectAttachment() {
        FileChooser fc = new FileChooser();
        File f = fc.showOpenDialog(historyTable.getScene().getWindow());
        if (f != null) { attachmentFile = f; attachmentField.setText(f.getName()); }
    }

    @FXML public void handleClearForm() { clearForm(); }
    @FXML public void handleRefresh() { loadHistory(); }

    @FXML public void handleDelete() {
        Notification n = historyTable.getSelectionModel().getSelectedItem();
        if (n == null) { warn("Select a notification."); return; }
        if (confirm("Delete notification?")) {
            notificationService.deleteNotification(n.getId());
            loadHistory();
        }
    }

    @FXML public void handleMarkRead() {
        Notification n = historyTable.getSelectionModel().getSelectedItem();
        if (n != null) { notificationService.markAsRead(n.getId()); loadHistory(); }
    }

    @FXML public void handleArchive() {
        Notification n = historyTable.getSelectionModel().getSelectedItem();
        if (n != null) { notificationService.archiveNotification(n.getId()); loadHistory(); }
    }

    @FXML
    public void onRecipientTypeChanged() {
        if (recipientTypeCombo == null) return;
        String type = recipientTypeCombo.getValue() != null ? recipientTypeCombo.getValue().toUpperCase() : "COLLEGE";

        if (recipientValueField != null) {
            recipientValueField.setDisable(!"USER".equals(type));
        }
        if (recipientValueLabel != null) {
            recipientValueLabel.setText("USER".equals(type) ? "User Email:" : "Target (optional):");
        }

        boolean needsDept = "DEPARTMENT".equals(type) || "DEPT_SEM".equals(type);
        boolean needsSem = "SEMESTER".equals(type) || "DEPT_SEM".equals(type);

        if (deptCombo != null) {
            deptCombo.setDisable(!needsDept);
            if (needsDept && deptCombo.getValue() == null) {
                deptCombo.setValue(AcademicFilterHelper.allDepartments());
            }
        }
        if (semCombo != null) {
            semCombo.setDisable(!needsSem);
            if (needsSem) {
                Department d = deptCombo != null ? deptCombo.getValue() : AcademicFilterHelper.allDepartments();
                semCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, d));
                if (semCombo.getValue() == null) semCombo.setValue(AcademicFilterHelper.allSemesters());
                if ("DEPT_SEM".equals(type)) {
                    semCombo.setDisable(AcademicFilterHelper.isAllDepartments(deptCombo != null ? deptCombo.getValue() : null));
                }
            }
        }
    }

    private void clearForm() {
        if (titleField != null) titleField.clear();
        if (messageArea != null) messageArea.clear();
        if (recipientValueField != null) recipientValueField.clear();
        if (attachmentField != null) attachmentField.clear();
        if (expiryDatePicker != null) expiryDatePicker.setValue(null);
        attachmentFile = null;
        if (notificationTypeCombo != null) notificationTypeCombo.setValue("Announcement");
        if (priorityCombo != null) priorityCombo.setValue("Normal");
        if (recipientTypeCombo != null) recipientTypeCombo.setValue("COLLEGE");
        if (deptCombo != null) deptCombo.setValue(AcademicFilterHelper.allDepartments());
        if (semCombo != null) {
            semCombo.setItems(AcademicFilterHelper.semestersForFilter(academicService, AcademicFilterHelper.allDepartments()));
            semCombo.setValue(AcademicFilterHelper.allSemesters());
            semCombo.setDisable(true);
        }
        onRecipientTypeChanged();
    }

    private void loadHistory() {
        historyMaster = notificationService.getNotifications();
        applyHistoryFilters();
        if (lblUnreadCount != null) lblUnreadCount.setText("Unread: " + notificationService.getUnreadCount());
    }

    private void applyHistoryFilters() {
        if (historyTable == null) return;
        String q = historySearchField != null ? historySearchField.getText().trim().toLowerCase() : "";
        String type = historyTypeFilter != null ? historyTypeFilter.getValue() : null;
        List<Notification> filtered = historyMaster.stream()
                .filter(n -> q.isEmpty()
                        || nullSafe(n.getTitle()).toLowerCase().contains(q)
                        || nullSafe(n.getMessage()).toLowerCase().contains(q)
                        || nullSafe(n.getRecipientDisplay()).toLowerCase().contains(q))
                .filter(n -> type == null || type.isEmpty()
                        || type.equalsIgnoreCase(nullSafe(n.getNotificationType())))
                .collect(Collectors.toList());
        historyTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void setupTable() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        if (colTitle != null) colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (colRecipient != null) colRecipient.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRecipientDisplay()));
        if (colType != null) colType.setCellValueFactory(new PropertyValueFactory<>("notificationType"));
        if (colPriority != null) colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        if (colSentAt != null) colSentAt.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSentAt() != null
                        ? data.getValue().getSentAt().format(fmt) : ""));
        if (colRead != null) colRead.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isRead() ? "Read" : "Unread"));
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
