package com.studybuddy.models;

import java.sql.Timestamp;

/**
 * Model class representing a row in the Tasks table.
 *
 * Tasks table schema:
 *   id          INT IDENTITY(1,1) PRIMARY KEY
 *   userId      INT NOT NULL          ← SQL column name is "userId"
 *   title       NVARCHAR(100)
 *   description NVARCHAR(MAX)
 *   status      NVARCHAR(20) DEFAULT 'pending'
 *   created_at  DATETIME DEFAULT GETDATE()
 */
public class Task {

    private int id;
    private int userId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String subject;
    private Timestamp dueDate;
    private String estimatedTime;
    private Timestamp createdAt;   // maps to SQL column: created_at
    private boolean isRewarded;    // tracks if achievement points have been awarded for this task

    // =========================
    // CONSTRUCTORS
    // =========================

    public Task() {
    }

    public Task(int id,
                int userId,
                String title,
                String description,
                String status) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    public Task(int id,
                int userId,
                String title,
                String description,
                String status,
                String priority,
                Timestamp dueDate,
                String estimatedTime) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.estimatedTime = estimatedTime;
    }

    public Task(int id,
                int userId,
                String title,
                String description,
                String status,
                Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    // =========================
    // GETTERS & SETTERS
    // =========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    public String getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRewarded() {
        return isRewarded;
    }

    public void setRewarded(boolean rewarded) {
        isRewarded = rewarded;
    }
}