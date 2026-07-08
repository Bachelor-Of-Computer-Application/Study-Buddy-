package com.studybuddy.models;

import java.time.LocalDateTime;

/**
 * Model class representing a Department (BCA or BBA).
 * Maps to the Departments table in the database.
 */
public class Department {

    private int id;
    private String name;
    private String code;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;

    public Department() {
    }

    public Department(int id, String name, String code, String description, boolean isActive) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.isActive = isActive;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
