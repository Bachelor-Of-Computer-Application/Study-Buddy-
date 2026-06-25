package com.studybuddy.dao;

import com.studybuddy.models.Answer;
import com.studybuddy.models.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAO {

    public boolean createQuestion(int userId, String text, String subject, int points, String attachment) throws SQLException {
        String sql = """
                INSERT INTO Questions
                (user_id, subject, question_text, attachment_path, reward_points, created_at, votes, views, is_locked)
                VALUES (?, ?, ?, ?, ?, GETDATE(), 0, 0, 0)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, subject);
            stmt.setString(3, text);
            stmt.setString(4, attachment);
            stmt.setInt(5, points);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Question> getAllQuestions() throws SQLException {
        String sql = """
                SELECT q.id, q.user_id, COALESCE(u.name, u.email, 'Unknown User') AS author_name,
                       q.subject, q.question_text, COALESCE(q.tags, '') AS tags, q.attachment_path,
                       q.reward_points, COALESCE(q.votes, 0) AS votes, COALESCE(q.views, 0) AS views,
                       CONVERT(varchar(10), q.created_at, 120) AS created_at,
                       COALESCE(q.is_locked, 0) AS is_locked
                FROM Questions q
                LEFT JOIN Users u ON q.user_id = u.id
                ORDER BY q.created_at DESC
                """;
        List<Question> questions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Question question = mapQuestion(rs);
                question.setAnswers(getAnswersByQuestionId(question.getId()));
                questions.add(question);
            }
        }

        return questions;
    }

    public List<Question> searchQuestions(String query, String subject) throws SQLException {
        String sql = """
                SELECT q.id, q.user_id, COALESCE(u.name, u.email, 'Unknown User') AS author_name,
                       q.subject, q.question_text, COALESCE(q.tags, '') AS tags, q.attachment_path,
                       q.reward_points, COALESCE(q.votes, 0) AS votes, COALESCE(q.views, 0) AS views,
                       CONVERT(varchar(10), q.created_at, 120) AS created_at,
                       COALESCE(q.is_locked, 0) AS is_locked
                FROM Questions q
                LEFT JOIN Users u ON q.user_id = u.id
                WHERE (? IS NULL OR q.question_text LIKE ? OR q.tags LIKE ?)
                  AND (? IS NULL OR q.subject = ?)
                ORDER BY q.created_at DESC
                """;
        String normalizedQuery = query == null || query.trim().isEmpty() ? null : "%" + query.trim() + "%";
        String normalizedSubject = subject == null || subject.trim().isEmpty() ? null : subject.trim();
        List<Question> questions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizedQuery);
            stmt.setString(2, normalizedQuery);
            stmt.setString(3, normalizedQuery);
            stmt.setString(4, normalizedSubject);
            stmt.setString(5, normalizedSubject);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Question question = mapQuestion(rs);
                    question.setAnswers(getAnswersByQuestionId(question.getId()));
                    questions.add(question);
                }
            }
        }

        return questions;
    }

    public List<Question> getRelatedQuestions(int questionId, String subject, int limit) throws SQLException {
        String sql = """
                SELECT TOP (?) q.id, q.user_id, COALESCE(u.name, u.email, 'Unknown User') AS author_name,
                       q.subject, q.question_text, COALESCE(q.tags, '') AS tags, q.attachment_path,
                       q.reward_points, COALESCE(q.votes, 0) AS votes, COALESCE(q.views, 0) AS views,
                       CONVERT(varchar(10), q.created_at, 120) AS created_at,
                       COALESCE(q.is_locked, 0) AS is_locked
                FROM Questions q
                LEFT JOIN Users u ON q.user_id = u.id
                WHERE q.id <> ? AND q.subject = ?
                ORDER BY q.created_at DESC
                """;
        List<Question> questions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, questionId);
            stmt.setString(3, subject);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapQuestion(rs));
                }
            }
        }

        return questions;
    }

    public List<Answer> getAnswersByQuestionId(int questionId) throws SQLException {
        String sql = """
                SELECT a.id, a.question_id, a.user_id, COALESCE(u.name, u.email, 'Unknown User') AS author_name,
                       a.answer_text, COALESCE(a.votes, 0) AS votes,
                       CONVERT(varchar(10), a.created_at, 120) AS created_at
                FROM Answers a
                LEFT JOIN Users u ON a.user_id = u.id
                WHERE a.question_id = ?
                ORDER BY a.created_at ASC
                """;
        List<Answer> answers = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapAnswer(rs));
                }
            }
        }

        return answers;
    }

    public boolean createAnswer(int questionId, int userId, String answerText) throws SQLException {
        String sql = """
                INSERT INTO Answers
                (question_id, user_id, answer_text, votes, created_at)
                VALUES (?, ?, ?, 0, GETDATE())
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);
            stmt.setInt(2, userId);
            stmt.setString(3, answerText);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateQuestionVotes(int questionId, int delta) throws SQLException {
        String sql = "UPDATE Questions SET votes = COALESCE(votes, 0) + ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, questionId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateAnswerVotes(int answerId, int delta) throws SQLException {
        String sql = "UPDATE Answers SET votes = COALESCE(votes, 0) + ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, answerId);
            return stmt.executeUpdate() > 0;
        }
    }

    public int getUserPoints(int userId) throws SQLException {
        String sql = "SELECT points FROM Users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("points") : 0;
            }
        }
    }

    public List<String> getAvailableSubjects() throws SQLException {
        String sql = """
                SELECT DISTINCT subject FROM Notes WHERE subject IS NOT NULL
                UNION
                SELECT DISTINCT subject FROM Questions WHERE subject IS NOT NULL
                ORDER BY subject
                """;
        List<String> subjects = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                subjects.add(rs.getString("subject"));
            }
        }

        return subjects;
    }

    public int countQuestionsByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Questions WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countAnswersByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Answers WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Question mapQuestion(ResultSet rs) throws SQLException {
        return new Question(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("author_name"),
                rs.getString("subject"),
                rs.getString("question_text"),
                rs.getString("tags"),
                rs.getString("attachment_path"),
                rs.getInt("reward_points"),
                rs.getInt("votes"),
                rs.getInt("views"),
                rs.getString("created_at"),
                rs.getBoolean("is_locked")
        );
    }

    private Answer mapAnswer(ResultSet rs) throws SQLException {
        return new Answer(
                rs.getInt("id"),
                rs.getInt("question_id"),
                rs.getInt("user_id"),
                rs.getString("author_name"),
                rs.getString("answer_text"),
                rs.getInt("votes"),
                rs.getString("created_at")
        );
    }
}
