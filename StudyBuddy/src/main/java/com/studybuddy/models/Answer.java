package com.studybuddy.models;

public class Answer {
    private int id;
    private int questionId;
    private int userId;
    private String authorName;
    private String answerText;
    private int votes;
    private String createdAt;
    private boolean isRewarded;

    public Answer() {
    }

    public Answer(int id, int questionId, int userId, String authorName, String answerText, int votes, String createdAt) {
        this.id = id;
        this.questionId = questionId;
        this.userId = userId;
        this.authorName = authorName;
        this.answerText = answerText;
        this.votes = votes;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public int getVotes() { return votes; }
    public void setVotes(int votes) { this.votes = votes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isRewarded() { return isRewarded; }
    public void setRewarded(boolean rewarded) { this.isRewarded = rewarded; }
}
