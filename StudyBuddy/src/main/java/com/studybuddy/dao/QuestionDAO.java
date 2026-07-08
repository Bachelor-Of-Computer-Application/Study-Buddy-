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

/**
 * Data Access Object for Question and Answer operations.
 *
 * Key SQL schema facts:
 *   Questions PK column is: question_id  (not "id")
 *   Answers   PK column is: answer_id    (not "id")
 *
 * All SELECT queries alias these PKs as "id" so the existing
 * mapQuestion() / mapAnswer() methods work without changes.
 */
public class QuestionDAO {

    // =========================
    // CREATE QUESTION
    // =========================

    /**
     * Inserts a new question into the Questions table.
     * SQL column names match the schema: user_id, subject, question_text,
     * attachment_path, reward_points, author_name
     */
    /**
     * Creates a question with an optional subjectId FK referencing the Subjects table.
     * subjectId = 0 is treated as NULL (backward-compatible with questions created before
     * the hierarchy migration).
     */
    public boolean createQuestion(int userId, String text, String subject,
                                  int subjectId, int points, String attachment) throws SQLException {
        String sql = "INSERT INTO Questions " +
                "(user_id, subject, subjectId, question_text, attachment_path, reward_points, created_at, votes, views, is_locked) " +
                "VALUES (?, ?, ?, ?, ?, ?, GETDATE(), 0, 0, 0)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, subject);
            // NULL when no subject was chosen (backward-compatible)
            if (subjectId > 0) {
                stmt.setInt(3, subjectId);
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            stmt.setString(4, text);
            stmt.setString(5, attachment != null ? attachment : "");
            stmt.setInt(6, points);
            return stmt.executeUpdate() > 0;
        }
    }

    // =========================
    // GET ALL QUESTIONS
    // =========================

    /**
     * Returns all questions with the author's name joined from Users.
     * FIXED: q.question_id is aliased as "id" so mapQuestion() works correctly.
     * SQL PK is question_id — aliased to "id" in the result set.
     */
    public List<Question> getAllQuestions() throws SQLException {
        // question_id aliased as id — matches mapQuestion() which reads rs.getInt("id").
        // subjectId is selected so the model is fully populated after the hierarchy migration.
        String sql = "SELECT q.question_id AS id, q.user_id, " +
                "COALESCE(u.name, u.email, 'Unknown User') AS author_name, " +
                "u.name, u.fullName, u.department, u.semester, " +
                "q.subject, q.subjectId, q.question_text, COALESCE(q.tags, '') AS tags, " +
                "q.attachment_path, " +
                "q.reward_points, COALESCE(q.votes, 0) AS votes, COALESCE(q.views, 0) AS views, " +
                "CONVERT(varchar(10), q.created_at, 120) AS created_at, " +
                "COALESCE(q.is_locked, 0) AS is_locked " +
                "FROM Questions q " +
                "LEFT JOIN Users u ON q.user_id = u.id " +
                "ORDER BY q.created_at DESC";

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

    // =========================
    // SEARCH QUESTIONS
    // =========================

    /**
     * Searches questions by text/tag keyword and/or subject filter.
     * FIXED: q.question_id aliased as "id".
     */
    public List<Question> searchQuestions(String query, String subject) throws SQLException {
        // question_id aliased as id — matches mapQuestion()
        String sql = "SELECT q.question_id AS id, q.user_id, " +
                "COALESCE(u.name, u.email, 'Unknown User') AS author_name, " +
                "u.name, u.fullName, u.department, u.semester, " +
                "q.subject, q.question_text, COALESCE(q.tags, '') AS tags, q.attachment_path, " +
                "q.reward_points, COALESCE(q.votes, 0) AS votes, COALESCE(q.views, 0) AS views, " +
                "CONVERT(varchar(10), q.created_at, 120) AS created_at, " +
                "COALESCE(q.is_locked, 0) AS is_locked " +
                "FROM Questions q " +
                "LEFT JOIN Users u ON q.user_id = u.id " +
                "WHERE (? IS NULL OR q.question_text LIKE ? OR q.tags LIKE ?) " +
                "  AND (? IS NULL OR q.subject = ?) " +
                "ORDER BY q.created_at DESC";

        String normalizedQuery   = (query   == null || query.trim().isEmpty())   ? null : "%" + query.trim()   + "%";
        String normalizedSubject = (subject == null || subject.trim().isEmpty()) ? null : subject.trim();
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

    // =========================
    // GET RELATED QUESTIONS
    // =========================

    /**
     * Returns related questions by subject, excluding the current question.
     * FIXED: q.question_id aliased as "id"; WHERE clause uses q.question_id.
     */
    public List<Question> getRelatedQuestions(int questionId, String subject, int limit) throws SQLException {
        // question_id aliased as id; WHERE filters on q.question_id (the actual PK)
        String sql = "SELECT TOP (?) q.question_id AS id, q.user_id, " +
                "COALESCE(u.name, u.email, 'Unknown User') AS author_name, " +
                "u.name, u.fullName, u.department, u.semester, " +
                "q.subject, q.question_text, COALESCE(q.tags, '') AS tags, q.attachment_path, " +
                "q.reward_points, COALESCE(q.votes, 0) AS votes, COALESCE(q.views, 0) AS views, " +
                "CONVERT(varchar(10), q.created_at, 120) AS created_at, " +
                "COALESCE(q.is_locked, 0) AS is_locked " +
                "FROM Questions q " +
                "LEFT JOIN Users u ON q.user_id = u.id " +
                "WHERE q.question_id <> ? AND q.subject = ? " +
                "ORDER BY q.created_at DESC";

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

    // =========================
    // GET ANSWERS BY QUESTION ID
    // =========================

    /**
     * Returns all answers for a given question.
     * FIXED: a.answer_id aliased as "id" so mapAnswer() works correctly.
     * SQL PK is answer_id — aliased to "id" in the result set.
     */
    public List<Answer> getAnswersByQuestionId(int questionId) throws SQLException {
        // answer_id aliased as id — matches mapAnswer() which reads rs.getInt("id")
        String sql = "SELECT a.answer_id AS id, a.question_id, a.user_id, " +
                "COALESCE(u.name, u.email, 'Unknown User') AS author_name, " +
                "a.answer_text, COALESCE(a.votes, 0) AS votes, " +
                "CONVERT(varchar(10), a.created_at, 120) AS created_at " +
                "FROM Answers a " +
                "LEFT JOIN Users u ON a.user_id = u.id " +
                "WHERE a.question_id = ? " +
                "ORDER BY a.created_at ASC";

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

    // =========================
    // CREATE ANSWER
    // =========================

    /**
     * Inserts a new answer for a question.
     * SQL column names: question_id, user_id, answer_text, votes, created_at
     */
    public boolean createAnswer(int questionId, int userId, String answerText) throws SQLException {
        String sql = "INSERT INTO Answers " +
                "(question_id, user_id, answer_text, votes, created_at) " +
                "VALUES (?, ?, ?, 0, GETDATE())";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);
            stmt.setInt(2, userId);
            stmt.setString(3, answerText);
            return stmt.executeUpdate() > 0;
        }
    }

    // =========================
    // SUBMIT ANSWER (with author_name)
    // =========================

    /**
     * Inserts a new answer including the author’s display name.
     * SQL: INSERT INTO Answers (question_id, user_id, author_name, answer_text, votes, created_at)
     *      VALUES (?, ?, ?, ?, 0, GETDATE())
     */
    public boolean submitAnswer(int questionId, int userId, String authorName, String answerText) throws SQLException {
        String sql = "INSERT INTO Answers (question_id, user_id, author_name, answer_text, votes, created_at) "
                   + "VALUES (?, ?, ?, ?, 0, GETDATE())";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);
            stmt.setInt(2, userId);
            stmt.setString(3, authorName);
            stmt.setString(4, answerText);
            return stmt.executeUpdate() > 0;
        }
    }

    // =========================
    // UPDATE ANSWER TEXT
    // =========================

    /**
     * Updates the answer_text of an existing answer.
     * Only the original author should be allowed to call this.
     * SQL: UPDATE Answers SET answer_text = ? WHERE answer_id = ? AND user_id = ?
     */
    public boolean updateAnswerText(int answerId, int userId, String newText) throws SQLException {
        String sql = "UPDATE Answers SET answer_text = ? WHERE answer_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newText);
            stmt.setInt(2, answerId);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    // =========================
    // DELETE ANSWER
    // =========================

    /**
     * Deletes an answer by answer_id, restricted to the owning user_id.
     * SQL: DELETE FROM Answers WHERE answer_id = ? AND user_id = ?
     */
    public boolean deleteAnswer(int answerId, int userId) throws SQLException {
        String sql = "DELETE FROM Answers WHERE answer_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, answerId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    // =========================
    // UPDATE VOTES
    // =========================

    /**
     * Updates vote count on a question.
     * FIXED: Uses question_id (the actual PK column) in WHERE clause.
     */
    public boolean updateQuestionVotes(int questionId, int delta) throws SQLException {
        // WHERE uses question_id — the actual Questions PK column
        String sql = "UPDATE Questions SET votes = COALESCE(votes, 0) + ? WHERE question_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, questionId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates vote count on an answer.
     * FIXED: Uses answer_id (the actual PK column) in WHERE clause.
     */
    public boolean updateAnswerVotes(int answerId, int delta) throws SQLException {
        // WHERE uses answer_id — the actual Answers PK column
        String sql = "UPDATE Answers SET votes = COALESCE(votes, 0) + ? WHERE answer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, answerId);
            return stmt.executeUpdate() > 0;
        }
    }

    // =========================
    // GET USER POINTS
    // =========================

    /**
     * Gets the current points total for a user.
     */
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

    // =========================
    // GET AVAILABLE SUBJECTS
    // =========================

    /**
     * Returns all distinct, active subject names from the canonical Subjects table.
     * These names are guaranteed to match the subject strings stored in Notes and
     * Questions because CreateNoteController / AskQuestionController always write
     * Subject.getName() into those columns.
     *
     * Falls back to a UNION of Notes/Questions subjects for backward-compatibility
     * with rows that were created before the hierarchy migration.
     */
    public List<String> getAvailableSubjects() throws SQLException {
        String sql =
            "SELECT name AS subject FROM Subjects WHERE isActive = 1 " +
            "UNION " +
            "SELECT DISTINCT subject FROM Notes WHERE subject IS NOT NULL AND subject NOT IN (SELECT name FROM Subjects WHERE isActive = 1) " +
            "UNION " +
            "SELECT DISTINCT subject FROM Questions WHERE subject IS NOT NULL AND subject NOT IN (SELECT name FROM Subjects WHERE isActive = 1) " +
            "ORDER BY subject";

        List<String> subjects = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                subjects.add(rs.getString("subject"));
            }
        }

        return subjects;
    }

    // =========================
    // COUNT QUESTIONS BY USER
    // =========================

    /**
     * Counts questions posted by a specific user.
     * SQL column: user_id (correct in Questions table)
     */
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

    // =========================
    // COUNT ANSWERS BY USER
    // =========================

    /**
     * Counts answers posted by a specific user.
     * SQL column: user_id (correct in Answers table)
     */
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

    // =========================
    // COUNT ALL QUESTIONS
    // =========================

    /**
     * Counts all questions in the database (for dashboard stats).
     */
    public int countAllQuestions() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Questions";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // =========================
    // HELPER — MAP ResultSet TO Question
    // =========================

    /**
     * Maps a ResultSet row to a Question object.
     * Reads column alias "id" which is question_id aliased in SELECT.
     */
    private Question mapQuestion(ResultSet rs) throws SQLException {
        Question q = new Question(
                rs.getInt("id"),              // aliased from question_id
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
        // subjectId — only present after hierarchy migration; guard against missing column
        try {
            int subjectId = rs.getInt("subjectId");
            if (!rs.wasNull()) {
                q.setSubjectId(subjectId);
            }
        } catch (SQLException ignored) {
            // Column not yet present in this DB — backward compatible
        }

        // Load user info (full name, department, semester)
        try {
            String fullName = rs.getString("fullName");
            if (fullName == null || fullName.isEmpty()) {
                fullName = rs.getString("name"); // Fallback to name field
            }
            q.setUserFullName(fullName != null ? fullName : "Unknown User");
        } catch (SQLException ignored) {
            q.setUserFullName("Unknown User");
        }

        try {
            q.setUserDepartment(rs.getString("department"));
        } catch (SQLException ignored) {
            q.setUserDepartment(null);
        }

        try {
            q.setUserSemester(rs.getString("semester"));
        } catch (SQLException ignored) {
            q.setUserSemester(null);
        }

        return q;
    }

    // =========================
    // HELPER — MAP ResultSet TO Answer
    // =========================

    /**
     * Maps a ResultSet row to an Answer object.
     * Reads column alias "id" which is answer_id aliased in SELECT.
     */
    private Answer mapAnswer(ResultSet rs) throws SQLException {
        return new Answer(
                rs.getInt("id"),              // aliased from answer_id
                rs.getInt("question_id"),
                rs.getInt("user_id"),
                rs.getString("author_name"),
                rs.getString("answer_text"),
                rs.getInt("votes"),
                rs.getString("created_at")
        );
    }
}
