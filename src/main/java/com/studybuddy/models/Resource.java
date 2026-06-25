// src/main/java/com/studybuddy/models/Resource.java
package com.studybuddy.models;

/**
 * Data container class for Resource (shared notes available to all users).
 */
public class Resource {
    private String id;
    private String noteId;
    private String title;
    private String subject;
    private String source;
    private String description;
    private String fileId;
    private String uploadedBy;
    private String uploadDate;
    private boolean isActive;
    private String filePath;
    private String fileType;

    public Resource() {
    }

    public Resource(String id, String noteId, String title, String subject, String source,
                    String description, String fileId, String uploadedBy, String uploadDate,
                    boolean isActive, String filePath, String fileType) {
        this.id = id;
        this.noteId = noteId;
        this.title = title;
        this.subject = subject;
        this.source = source;
        this.description = description;
        this.fileId = fileId;
        this.uploadedBy = uploadedBy;
        this.uploadDate = uploadDate;
        this.isActive = isActive;
        this.filePath = filePath;
        this.fileType = fileType;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getNoteId() {
        return noteId;
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

    public String getDescription() {
        return description;
    }

    public String getFileId() {
        return fileId;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileType() {
        return fileType;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}