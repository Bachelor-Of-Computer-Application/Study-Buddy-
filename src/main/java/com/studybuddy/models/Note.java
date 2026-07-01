package com.studybuddy.models;

/**
 * Data container class for Note (user's personal notes).
 */
public class Note {

    private int id;
    private String title;
    private String subject;
    private String source;
    private String uploadDate;
    private String fileType;
    private String fileName;
    private String filePath;
    private String description;
    private int userId;
    private boolean isPrivate;
    private String status = "Pending";

    public Note() {
    }

    public Note(int id,
                String title,
                String subject,
                String source,
                String uploadDate,
                String fileType,
                String fileName,
                String filePath,
                String description,
                int userId,
                boolean isPrivate) {

        this.id = id;
        this.title = title;
        this.subject = subject;
        this.source = source;
        this.uploadDate = uploadDate;
        this.fileType = fileType;
        this.fileName = fileName;
        this.filePath = filePath;
        this.description = description;
        this.userId = userId;
        this.isPrivate = isPrivate;
    }

    // =========================
    // GETTERS
    // =========================

    public int getId() {
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

    public String getFilePath() { return filePath; }

    public String getDescription() {
        return description;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    // =========================
    // SETTERS
    // =========================

    public void setId(int id) {
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

    public void setFilePath(String filePath) { this.filePath = filePath; }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}