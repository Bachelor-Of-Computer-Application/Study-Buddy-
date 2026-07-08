
package com.studybuddy.models;

import java.time.LocalDateTime;

public class UserActivity {
    private int id;
    private int userId;
    private String userFullName;
    private String action; // "UPLOAD_NOTE", "UPLOAD_RESOURCE", "ASK_QUESTION", "SUBMIT_ANSWER", "APPROVE_NOTE", "REJECT_NOTE", etc.
    private String targetType; // "NOTE", "RESOURCE", "QUESTION", "ANSWER"
    private String targetName;
    private LocalDateTime createdAt;

    public UserActivity() {}

    public UserActivity(int userId, String userFullName, String action, String targetType, String targetName) {
        this.userId = userId;
        this.userFullName = userFullName;
        this.action = action;
        this.targetType = targetType;
        this.targetName = targetName;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
