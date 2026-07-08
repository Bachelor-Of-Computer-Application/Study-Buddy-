package com.studybuddy.models;

import java.time.LocalDateTime;

/**
 * Model representing an admin-sent notification.
 */
public class Notification {

    private int id;
    private int userId;
    private String title;
    private String message;
    /** 'ALL', 'DEPARTMENT', 'SEMESTER', 'USER' */
    private String recipientType;
    /** null for ALL; dept name, semester number, or user email otherwise */
    private String recipientValue;
    /** 'LOW', 'NORMAL', 'HIGH', 'URGENT', 'Critical' */
    private String priority;
    private int sentBy;
    private LocalDateTime sentAt;
    private boolean isRead;
    /** Announcement, Exam, Assignment, Event, Holiday, Maintenance, Emergency */
    private String notificationType;
    private LocalDateTime expiryDate;
    private String attachmentPath;
    private boolean archived;
    private Integer departmentId;
    private Integer semesterId;

    public Notification() {}

    public Notification(String title, String message, String recipientType,
                        String recipientValue, String priority, int sentBy) {
        this.title = title;
        this.message = message;
        this.recipientType = recipientType;
        this.recipientValue = recipientValue;
        this.priority = priority;
        this.sentBy = sentBy;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getRecipientType() { return recipientType; }
    public String getRecipientValue() { return recipientValue; }
    public String getPriority() { return priority; }
    public int getSentBy() { return sentBy; }
    public LocalDateTime getSentAt() { return sentAt; }
    public boolean isRead() { return isRead; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }
    public void setRecipientValue(String recipientValue) { this.recipientValue = recipientValue; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setSentBy(int sentBy) { this.sentBy = sentBy; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public void setRead(boolean read) { isRead = read; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public Integer getSemesterId() { return semesterId; }
    public void setSemesterId(Integer semesterId) { this.semesterId = semesterId; }

    /** Display-friendly recipient label */
    public String getRecipientDisplay() {
        if ("ALL".equalsIgnoreCase(recipientType)) return "All Users";
        if (recipientValue != null && !recipientValue.isBlank())
            return recipientType + ": " + recipientValue;
        return recipientType;
    }
}
