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
    private Timestamp createdAt;   // maps to SQL column: created_at

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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}