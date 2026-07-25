package com.studybuddy.services;

import com.studybuddy.App;
import com.studybuddy.dao.QuestionDAO;
import com.studybuddy.dao.UserDAO;
import com.studybuddy.models.Question;
import com.studybuddy.models.User;
import com.studybuddy.utils.EventBus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QuestionService {

    private final QuestionDAO questionDAO = new QuestionDAO();
    private final UserDAO userDAO = new UserDAO();

    /**
     * Persists a new question.
     * Delegates to QuestionDAO which now stores both subject (name) and subjectId (FK).
     */
    public boolean saveQuestion(String text, String subject, int subjectId, int points, String attachment,
                                  Integer departmentId, Integer semesterId) {
        try {
            int userId = getCurrentUserId();
            boolean ok = questionDAO.createQuestion(userId, text, subject, subjectId, points, attachment,
                    departmentId, semesterId);
            if (ok) {
                com.studybuddy.admin.services.ActivityLogService.getInstance().logAction("Question Posted", "Question", subject);
            }
            return ok;
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).warning("[QuestionService] ❌ Failed to save question: " + e.getMessage());
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return false;
        }
    }

    public boolean saveQuestion(String text, String subject, int subjectId, int points, String attachment) {
        return saveQuestion(text, subject, subjectId, points, attachment, null, null);
    }

    /**
     * Saves a question and deducts reward points from the user's achievement balance.
     * Requirement 2: Deduct points immediately when question is created.
     *
     * @param text The question text content
     * @param subject The subject name
     * @param subjectId The subject ID from Subjects table (0 treated as NULL)
     * @param rewardPoints The reward points offered for this question (0 or positive)
     * @param attachment Optional file attachment path
     * @param departmentId Optional department ID
     * @param semesterId Optional semester ID
     * @return true if question was saved and points were deducted, false otherwise
     */
    public boolean saveQuestionWithDeduction(String text, String subject, int subjectId, int rewardPoints,
                                              String attachment, Integer departmentId, Integer semesterId) {
        if (rewardPoints <= 0) {
            // No points to deduct, just save the question
            return saveQuestion(text, subject, subjectId, 0, attachment, departmentId, semesterId);
        }

        int userId = getCurrentUserId();

        try (java.sql.Connection conn = com.studybuddy.dao.DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Step 1: Deduct points from user (validate sufficient balance)
                int newBalance = 0;
                String deductSql = "UPDATE Users SET achievement_points = achievement_points - ? OUTPUT INSERTED.achievement_points WHERE id = ? AND achievement_points >= ?";
                try (java.sql.PreparedStatement ps = conn.prepareStatement(deductSql)) {
                    ps.setInt(1, rewardPoints);
                    ps.setInt(2, userId);
                    ps.setInt(3, rewardPoints);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            newBalance = rs.getInt("achievement_points");
                        } else {
                            // User doesn't have enough points
                            conn.rollback();
                            return false;
                        }
                    }
                }

                // Step 2: Create the question with reward points (same connection)
                boolean questionCreated = questionDAO.createQuestion(conn, userId, text, subject, subjectId,
                        rewardPoints, attachment, departmentId, semesterId);
                if (!questionCreated) {
                    conn.rollback();
                    return false;
                }

                conn.commit();

                // Requirement 5: Publish PointsChangedEvent for profile refresh
                EventBus.getInstance().publish(new EventBus.PointsChangedEvent(userId, newBalance));

                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).warning("[QuestionService] ❌ Failed to save question with deduction: " + e.getMessage());
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Returns the achievement point balance for the current user.
     */
    public int getAchievementPoints() {
        Integer points = userDAO.getAchievementPoints(getCurrentUserId());
        return points != null ? points : 0;
    }

    /**
     * Returns the point balance for the current user.
     * @deprecated Use {@link #getAchievementPoints()} for the achievement points system.
     */
    @Deprecated
    public int getUserPoints() {
        return getAchievementPoints();
    }

    /**
     * Returns the list of subject names for filter ComboBoxes.
     * Queries the canonical Subjects table (+ backward-compat legacy subjects).
     * Never returns a hardcoded list.
     */
    public List<String> getAvailableSubjects() {
        try {
            return questionDAO.getAvailableSubjects();
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).warning("[QuestionService] Could not load subjects: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private int getCurrentUserId() {
        return App.getCurrentUser() != null ? App.getCurrentUser().getId() : 1;
    }

    public boolean validateInputs(String q, String s, int rewardPoints) {
        return q != null && !q.isEmpty()
                && s != null && !s.isEmpty()
                && rewardPoints > 0;
    }

    public int saveQuestionBankEntry(Question q) throws SQLException {
        return questionDAO.createQuestionBankEntry(q);
    }

    public boolean updateQuestionBankEntry(Question q) throws SQLException {
        return questionDAO.updateQuestionBankEntry(q);
    }

    public List<Question> getQuestionBankEntries() throws SQLException {
        return questionDAO.getQuestionBankEntries();
    }

    public List<Question> getAllQuestionsForAdminPanel() throws SQLException {
        return questionDAO.getAllQuestionsForAdminPanel();
    }

    public Question getQuestionById(int id) throws SQLException {
        return questionDAO.getQuestionById(id);
    }

    public List<Question> getQuestionsByUserId(int userId) throws SQLException {
        return questionDAO.getQuestionsByUserId(userId);
    }

    public boolean deleteQuestion(int id, User user) throws SQLException {
        Question q = questionDAO.getQuestionById(id);
        if (q == null) return false;
        AuthorizationService.getInstance().requireOwnership(user, q.getUserId());
        return deleteQuestion(id);
    }

    public boolean deleteQuestion(int id) throws SQLException {
        Question q = questionDAO.getQuestionById(id);
        boolean deleted = questionDAO.deleteQuestionById(id);
        if (deleted && q != null) {
            if (q.getAttachmentPath() != null && !q.getAttachmentPath().isBlank()) {
                FileStorageService.getInstance().deleteFile(q.getAttachmentPath());
            }
            com.studybuddy.admin.services.ActivityLogService.getInstance().logAction("Question Deleted", "Question", q.getQuestionText());
        }
        return deleted;
    }

    // =========================
    // MARK BEST ANSWER (Requirement 3)
    // =========================

    /**
     * Marks an answer as the best answer and transfers reward points to the answer author.
     * Only administrators can call this method.
     * Requirements: 3.1, 3.2, 3.3
     *
     * @param questionId The ID of the question
     * @param answerId The ID of the answer to mark as best
     * @return true if the best answer was marked successfully, false otherwise
     */
    public boolean markBestAnswer(int questionId, int answerId) {
        try {
            boolean ok = questionDAO.markBestAnswer(questionId, answerId);
            if (ok) {
                com.studybuddy.admin.services.ActivityLogService.getInstance().logAction("Best Answer Selected", "Question", "Q#" + questionId);
            }
            return ok;
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).warning("[QuestionService] Failed to mark best answer: " + e.getMessage());
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    // =========================
    // APPROVE QUESTION (Requirement 2.3)
    // =========================

    /**
     * Approves a question, allowing the reward points to be transferred when a best answer is marked.
     * Requirement: 2.3
     *
     * @param questionId The ID of the question to approve
     * @return true if the question was approved successfully, false otherwise
     */
    public boolean approveQuestion(int questionId) {
        try {
            return questionDAO.approveQuestion(questionId);
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).warning("[QuestionService] Failed to approve question: " + e.getMessage());
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the approval status of a question.
     * Requirement: 2.3
     *
     * @param questionId The ID of the question
     * @return true if the question is approved, false otherwise
     */
    public boolean isQuestionApproved(int questionId) {
        try {
            return questionDAO.isQuestionApproved(questionId);
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).warning("[QuestionService] Failed to check question approval: " + e.getMessage());
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    public boolean approveAnswer(int answerId) {
        try {
            boolean ok = questionDAO.approveAnswer(answerId);
            if (ok) {
                com.studybuddy.admin.services.ActivityLogService.getInstance().logAction("Answer Approved", "Answer", "Answer#" + answerId);
            }
            return ok;
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).warning("[QuestionService] Failed to approve answer: " + e.getMessage());
            java.util.logging.Logger.getLogger(QuestionService.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }
}