package com.studybuddy.admin.controllers;

import com.studybuddy.admin.services.NotificationService;
import com.studybuddy.models.Notification;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Notification management: send to All/Department/Semester/User, view history, delete.
 */
public class AdminNotificationsController {

    // ── Send form ─────────────────────────────────────────────────────────────
    @FXML private TextField        titleField;
    @FXML private TextArea         messageArea;
    @FXML private ComboBox<String> recipientTypeCombo;
    @FXML private TextField        recipientValueField;
    @FXML private ComboBox<String> priorityCombo;
    @FXML private Label            recipientValueLabel;

    // ── History table ─────────────────────────────────────────────────────────
    @FXML private TableView<Notification>             historyTable;
    @FXML private TableColumn<Notification, String>   colTitle;
    @FXML private TableColumn<Notification, String>   colRecipient;
    @FXML private TableColumn<Notification, String>   colPriority;
    @FXML private TableColumn<Notification, String>   colSentAt;
    @FXML private TableColumn<Notification, String>   colRead;

    @FXML private Label lblUnreadCount;

    private final NotificationService notificationService = NotificationService.getInstance();

    @FXML
    public void initialize() {
        setupForm();
        setupTable();
        loadHistory();
    }

    // ── Form ──────────────────────────────────────────────────────────────────

    @FXML
    public void handleSend() {
        String title   = titleField   != null ? titleField.getText().trim()   : "";
        String message = messageArea  != null ? messageArea.getText().trim()   : "";
        String type    = recipientTypeCombo != null ? recipientTypeCombo.getValue() : "ALL";
        String value   = recipientValueField != null ? recipientValueField.getText().trim() : null;
        String priority= priorityCombo != null ? priorityCombo.getValue() : "NORMAL";

        if (title.isEmpty()) { warn("Notification title is required."); return; }
        if (message.isEmpty()) { warn("Notification message is required."); return; }

        // For non-ALL types, a recipient value is required
        if (!"ALL".equalsIgnoreCase(type) && (value == null || value.isEmpty())) {
            warn("Please specify the recipient " + type.toLowerCase() + "."); return;
        }

        boolean ok = notificationService.sendNotification(title, message, type, value, priority);
        if (ok) {
            clearForm();
            loadHistory();
            info("Notification sent successfully to: " + (value != null && !value.isBlank() ? value : "All Users"));
        } else {
            warn("Failed to send notification. Ensure the Notifications table exists in your database.");
        }
    }

    @FXML
    public void handleClearForm() { clearForm(); }

    private void clearForm() {
        if (titleField          != null) titleField.clear();
        if (messageArea         != null) messageArea.clear();
        if (recipientTypeCombo  != null) recipientTypeCombo.setValue("ALL");
        if (recipientValueField != null) { recipientValueField.clear(); recipientValueField.setDisable(true); }
        if (priorityCombo       != null) priorityCombo.setValue("NORMAL");
    }

    /** Show/hide the recipient value field based on recipient type. */
    @FXML
    public void onRecipientTypeChanged() {
        if (recipientTypeCombo == null || recipientValueField == null) return;
        String type = recipientTypeCombo.getValue();
        boolean needsValue = !"ALL".equalsIgnoreCase(type);
        recipientValueField.setDisable(!needsValue);

        if (recipientValueLabel != null) {
            switch (type) {
                case "DEPARTMENT" -> recipientValueLabel.setText("Department Name:");
                case "SEMESTER"   -> recipientValueLabel.setText("Semester Number:");
                case "USER"       -> recipientValueLabel.setText("User Email:");
                default           -> recipientValueLabel.setText("Recipient:");
            }
        }
    }

    // ── History ───────────────────────────────────────────────────────────────

    @FXML
    public void handleRefresh() { loadHistory(); }

    @FXML
    public void handleDelete() {
        Notification n = historyTable.getSelectionModel().getSelectedItem();
        if (n == null) { warn("Select a notification to delete."); return; }
        if (confirm("Delete notification \"" + n.getTitle() + "\"?")) {
            notificationService.deleteNotification(n.getId());
            loadHistory();
        }
    }

    @FXML
    public void handleMarkRead() {
        Notification n = historyTable.getSelectionModel().getSelectedItem();
        if (n == null) { warn("Select a notification to mark as read."); return; }
        notificationService.markAsRead(n.getId());
        loadHistory();
    }

    private void loadHistory() {
        List<Notification> list = notificationService.getNotifications();
        historyTable.setItems(FXCollections.observableArrayList(list));
        int unread = notificationService.getUnreadCount();
        if (lblUnreadCount != null) lblUnreadCount.setText("Unread: " + unread);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupForm() {
        if (recipientTypeCombo != null) {
            recipientTypeCombo.setItems(FXCollections.observableArrayList("ALL", "DEPARTMENT", "SEMESTER", "USER"));
            recipientTypeCombo.setValue("ALL");
            recipientTypeCombo.setOnAction(e -> onRecipientTypeChanged());
        }
        if (priorityCombo != null) {
            priorityCombo.setItems(FXCollections.observableArrayList("LOW", "NORMAL", "HIGH", "URGENT"));
            priorityCombo.setValue("NORMAL");
        }
        if (recipientValueField != null) recipientValueField.setDisable(true);
    }

    private void setupTable() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        if (colTitle     != null) colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (colRecipient != null) colRecipient.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRecipientDisplay()));
        if (colPriority  != null) colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        if (colSentAt    != null) colSentAt.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSentAt() != null
                        ? data.getValue().getSentAt().format(fmt) : ""));
        if (colRead      != null) colRead.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isRead() ? "✅ Read" : "🔵 Unread"));
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void info(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void warn(String msg) { Alert a = new Alert(Alert.AlertType.WARNING, msg); a.setHeaderText(null); a.showAndWait(); }
}
