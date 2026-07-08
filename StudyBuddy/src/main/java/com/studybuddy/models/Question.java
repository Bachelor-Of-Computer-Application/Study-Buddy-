package com.studybuddy.models;

import java.util.ArrayList;
import java.util.List;

public class Question {
    private int id;
    private int userId;
    private String authorName;
    private String subject;
    private int subjectId;
    private String questionText;
    private String tags;
    private String attachmentPath;
    private int rewardPoints;
    private int votes;
    private int views;
    private String createdAt;
    private boolean isLocked;
    private List<Answer> answers = new ArrayList<>();

    // Transient display fields for user info
    private String userFullName;
    private String userDepartment;
    private String userSemester;

    public Question() {
    }

    public Question(int id, int userId, String authorName, String subject, String questionText,
                    String tags, String attachmentPath, int rewardPoints, int votes, int views,
                    String createdAt, boolean isLocked) {
        this.id = id;
        this.userId = userId;
        this.authorName = authorName;
        this.subject = subject;
        this.questionText = questionText;
        this.tags = tags;
        this.attachmentPath = attachmentPath;
        this.rewardPoints = rewardPoints;
        this.votes = votes;
        this.views = views;
        this.createdAt = createdAt;
        this.isLocked = isLocked;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }

    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    public int getVotes() { return votes; }
    public void setVotes(int votes) { this.votes = votes; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    public List<Answer> getAnswers() { return answers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }

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

    @Override
    public String toString() {
        String sub  = subject      != null ? subject.toUpperCase()       : "GENERAL";
        String text = questionText != null ? questionText                 : "";
        String preview = text.length() > 50 ? text.substring(0, 47) + "..." : text;
        return "[" + sub + "] " + preview;
    }
}
