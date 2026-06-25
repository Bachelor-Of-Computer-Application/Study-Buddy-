package com.studybuddy.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model class representing a User in the Study Buddy application.
 * Extended with profile, preferences, and notification fields for Edit Profile functionality.
 */
public class User {
    // ===========================
    // Core User Properties
    // ===========================
    private int id;
    private String name;
    private String email;
    private String password;
    private String role;
    private LocalDateTime createdAt;

    // ===========================
    // Profile Information (New for Edit Profile)
    // ===========================
    private String fullName;
    private String username;
    private String bio;
    private String educationLevel;
    private LocalDate dateOfBirth;
    private String profileImagePath;
    private String phoneNumber;
    private String department;
    private String semester;

    // ===========================
    // Study Preferences (New for Edit Profile)
    // ===========================
    private String preferredSubjects;
    private String studyGoals;
    private String learningInterests;
    private boolean notificationsEnabled;

    // ===========================
    // Notification Settings (New for Edit Profile)
    // ===========================
    private boolean emailNotificationsEnabled;
    private boolean resourceUpdateNotifications;
    private boolean systemNotifications;

    // ===========================
    // User Statistics (New for Edit Profile)
    // ===========================
    private int answersCount;
    private int questionsCount;
    private int achievements;
    private int points;

    // ===========================
    // Default Constructor
    // ===========================
    public User() {
    }

    // ===========================
    // Constructor with Core Properties
    // ===========================
    public User(int id, String name, String email, String password, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // ===========================
    // Getters and Setters - Core Properties
    // ===========================

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ===========================
    // Getters and Setters - Profile Information
    // ===========================

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getProfileImagePath() {
        return profileImagePath;
    }

    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    // ===========================
    // Getters and Setters - Study Preferences
    // ===========================

    public String getPreferredSubjects() {
        return preferredSubjects;
    }

    public void setPreferredSubjects(String preferredSubjects) {
        this.preferredSubjects = preferredSubjects;
    }

    public String getStudyGoals() {
        return studyGoals;
    }

    public void setStudyGoals(String studyGoals) {
        this.studyGoals = studyGoals;
    }

    public String getLearningInterests() {
        return learningInterests;
    }

    public void setLearningInterests(String learningInterests) {
        this.learningInterests = learningInterests;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    // ===========================
    // Getters and Setters - Notification Settings
    // ===========================

    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public boolean isResourceUpdateNotifications() {
        return resourceUpdateNotifications;
    }

    public void setResourceUpdateNotifications(boolean resourceUpdateNotifications) {
        this.resourceUpdateNotifications = resourceUpdateNotifications;
    }

    public boolean isSystemNotifications() {
        return systemNotifications;
    }

    public void setSystemNotifications(boolean systemNotifications) {
        this.systemNotifications = systemNotifications;
    }

    // ===========================
    // Getters and Setters - User Statistics
    // ===========================

    public int getAnswersCount() {
        return answersCount;
    }

    public void setAnswersCount(int answersCount) {
        this.answersCount = answersCount;
    }

    public int getQuestionsCount() {
        return questionsCount;
    }

    public void setQuestionsCount(int questionsCount) {
        this.questionsCount = questionsCount;
    }

    public int getAchievements() {
        return achievements;
    }

    public void setAchievements(int achievements) {
        this.achievements = achievements;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    // ===========================
    // toString Method
    // ===========================

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", educationLevel='" + educationLevel + '\'' +
                ", points=" + points +
                ", achievements=" + achievements +
                '}';
    }

    // ===========================
    // equals Method
    // ===========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        User user = (User) obj;
        return id == user.id;
    }

    // ===========================
    // hashCode Method
    // ===========================

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}