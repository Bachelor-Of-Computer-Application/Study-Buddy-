package com.studybuddy.models;

import java.time.LocalDateTime;

/**
 * Model class representing a logged study session.
 */
public class StudySession {
    private int id;
    private int userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double durationHours;
    private String subject;

    public StudySession() {
    }

    public StudySession(int id, int userId, LocalDateTime startTime, LocalDateTime endTime, double durationHours, String subject) {
        this.id = id;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationHours = durationHours;
        this.subject = subject;
    }

    // Getters and Setters

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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public double getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(double durationHours) {
        this.durationHours = durationHours;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
