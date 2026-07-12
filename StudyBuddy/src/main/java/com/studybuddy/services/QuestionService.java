package com.studybuddy.services;

import com.studybuddy.App;
import com.studybuddy.dao.QuestionDAO;
import com.studybuddy.models.Question;
import com.studybuddy.models.User;
import com.studybuddy.utils.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QuestionService {

    private final QuestionDAO questionDAO = new QuestionDAO();

    /**
     * Persists a new question.
     * Delegates to QuestionDAO which now stores both subject (name) and subjectId (FK).
     */
    public boolean saveQuestion(String text, String subject, int subjectId, int points, String attachment,
                                  Integer departmentId, Integer semesterId) {
        try {
            int userId = getCurrentUserId();
            return questionDAO.createQuestion(userId, text, subject, subjectId, points, attachment,
                    departmentId, semesterId);
        } catch (SQLException e) {
            System.err.println("[QuestionService] ❌ Failed to save question: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveQuestion(String text, String subject, int subjectId, int points, String attachment) {
        return saveQuestion(text, subject, subjectId, points, attachment, null, null);
    }

    /**
     * Returns the point balance for the current user.
     * Returns 0 on any failure — never a magic number.
     */
    public int getUserPoints() {
        String sql = "SELECT points FROM Users WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, getCurrentUserId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("points");
            }

        } catch (SQLException e) {
            System.err.println("[QuestionService] Could not fetch user points: " + e.getMessage());
        }

        return 0;
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
            System.err.println("[QuestionService] Could not load subjects: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private int getCurrentUserId() {
        return App.getCurrentUser() != null ? App.getCurrentUser().getId() : 1;
    }

    public boolean validateInputs(String q, String s, int p) {
        return q != null && !q.isEmpty()
                && s != null && !s.isEmpty()
                && p >= 0;
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
        if (deleted && q != null && q.getAttachmentPath() != null && !q.getAttachmentPath().isBlank()) {
            FileStorageService.getInstance().deleteFile(q.getAttachmentPath());
        }
        return deleted;
    }
}