package com.studybuddy.models;

import java.time.LocalDateTime;

/**
 * Model class representing a Semester within a Department.
 * Maps to the Semesters table in the database.
 */
public class Semester {

    private int id;
    private int departmentId;
    private int semesterNumber;
    private String name;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;
    private String departmentName;
    private String departmentCode;

    public Semester() {
    }

    public Semester(int id, int departmentId, int semesterNumber, String name, 
                    String description, boolean isActive) {
        this.id = id;
        this.departmentId = departmentId;
        this.semesterNumber = semesterNumber;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public int getSemesterNumber() {
        return semesterNumber;
    }

    public void setSemesterNumber(int semesterNumber) {
        this.semesterNumber = semesterNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    @Override
    public String toString() {
        return name;
    }
}
