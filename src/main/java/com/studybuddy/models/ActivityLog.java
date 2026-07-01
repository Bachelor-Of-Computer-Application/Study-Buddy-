package com.studybuddy.models;

import java.time.LocalDateTime;

/**
 * Model representing an admin activity log entry.
 * Every admin action automatically generates one of these records.
 */
public class ActivityLog {

    private int id;
    private int adminId;
    private String adminName;
    private String action;
    private String targetType;  // e.g. "User", "Note", "Resource", "Question", "Settings"
    private String targetName;  // e.g. the user's name or note's title
    private String status;      // "SUCCESS" or "FAILED"
    private String remarks;
    private LocalDateTime createdAt;

    public ActivityLog() {}

    public ActivityLog(int adminId, String adminName, String action,
                       String targetType, String targetName, String status, String remarks) {
        this.adminId = adminId;
        this.adminName = adminName;
        this.action = action;
        this.targetType = targetType;
        this.targetName = targetName;
        this.status = status;
        this.remarks = remarks;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getId() { return id; }
    public int getAdminId() { return adminId; }
    public String getAdminName() { return adminName; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public String getTargetName() { return targetName; }
    public String getStatus() { return status; }
    public String getRemarks() { return remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(int id) { this.id = id; }
    public void setAdminId(int adminId) { this.adminId = adminId; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
    public void setAction(String action) { this.action = action; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public void setStatus(String status) { this.status = status; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "[" + (createdAt != null ? createdAt : "?") + "] "
                + adminName + " → " + action + " (" + targetType + ": " + targetName + ")";
    }
}
