package com.studybuddy.models;

/**
 * Data container class for Note (user's personal notes).
 */
public class Note {
    private String id;
    private String title;
    private String subject;
    private String source;
    private String uploadDate;
    private String fileType;
    private String fileName;
    private String description;
    private String userId;
    private boolean isPrivate;

    public Note() {
    }

    public Note(String id, String title, String subject, String source, String uploadDate,
                String fileType, String fileName, String description, String userId, boolean isPrivate) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.source = source;
        this.uploadDate = uploadDate;
        this.fileType = fileType;
        this.fileName = fileName;
        this.description = description;
        this.userId = userId;
        this.isPrivate = isPrivate;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        return subject;
    }

    public String getSource() {
        return source;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDescription() {
        return description;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}