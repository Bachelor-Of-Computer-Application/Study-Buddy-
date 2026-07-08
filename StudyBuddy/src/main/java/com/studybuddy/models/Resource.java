package com.studybuddy.models;

/**
 * Data container class for Resource.
 */
public class Resource {

    private int id;
    private Integer noteId; // nullable (some resources may have no linked note)
    private int uploadedBy;
    private String uploaderName;
    private String title;
    private String subject;
    private int subjectId;
    private String source;
    private String description;
    private String uploadDate;
    private String filePath;
    private String fileType;
    private String fileId; // used by ResourceDAO (UUID-based schema)
    private int downloads;
    private boolean isActive;
    private String userFullName;
    private String userDepartment;
    private String userSemester;

    public Resource() {
    }

    public Resource(int id,
            Integer noteId,
            int uploadedBy,
            String title,
            String subject,
            String source,
            String description,
            String uploadDate,
            String filePath,
            String fileType,
            int downloads,
            boolean isActive) {
        this.id = id;
        this.noteId = noteId;
        this.uploadedBy = uploadedBy;
        this.title = title;
        this.subject = subject;
        this.source = source;
        this.description = description;
        this.uploadDate = uploadDate;
        this.filePath = filePath;
        this.fileType = fileType;
        this.downloads = downloads;
        this.isActive = isActive;
    }

    // Getters
    public int getId() {
        return id;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public int getUploadedBy() {
        return uploadedBy;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        return subject;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public String getSource() {
        return source;
    }

    public String getDescription() {
        return description;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFileId() {
        return fileId;
    }

    public int getDownloads() {
        return downloads;
    }

    public boolean isActive() {
        return isActive;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setNoteId(Integer noteId) {
        this.noteId = noteId;
    }

    public void setUploadedBy(int uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    // Getters and Setters for transient user info fields
    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUserDepartment() {
        return userDepartment;
    }

    public void setUserDepartment(String userDepartment) {
        this.userDepartment = userDepartment;
    }

    public String getUserSemester() {
        return userSemester;
    }

    public void setUserSemester(String userSemester) {
        this.userSemester = userSemester;
    }
}