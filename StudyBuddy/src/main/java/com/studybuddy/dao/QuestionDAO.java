package com.studybuddy.dao;

import com.studybuddy.models.Answer;
import com.studybuddy.models.Question;
import com.studybuddy.utils.DatabaseUtil;
import com.studybuddy.utils.EventBus;

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
                                  int subjectId, int points, String attachment,
                                  Integer departmentId, Integer semesterId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return createQuestion(conn, userId, text, subject, subjectId, points, attachment, departmentId, semesterId);
        }
    }

    /**
     * Inserts a question using an existing connection (for transactional operations).
     */
    public boolean createQuestion(Connection conn, int userId, String text, String subject,
                                  int subjectId, int points, String attachment,
                                  Integer departmentId, Integer semesterId) throws SQLException {
        String sql = "INSERT INTO Questions " +
                "(user_id, subject, subjectId, departmentId, semesterId, question_text, attachment_path, reward_points, reward_status, approved, created_at, votes, views, is_locked) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', 0, GETDATE(), 0, 0, 0)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, subject);
            if (subjectId > 0) {
                stmt.setInt(3, subjectId);
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            setNullableInt(stmt, 4, departmentId);
            setNullableInt(stmt, 5, semesterId);
            stmt.setString(6, text);
            stmt.setString(7, attachment != null ? attachment : "");
            stmt.setInt(8, points);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Backward-compatible overload without department/semester columns. */
    public boolean createQuestion(int userId, String text, String subject,
                                  int subjectId, int points, String attachment) throws SQLException {
        return createQuestion(userId, text, subject, subjectId, points, attachment, null, null);
    }

    // =========================
    // CREATE QUESTION WITH REWARD
    // =========================

    /**
     * Creates a question with an explicit reward points value.
     * This is the primary method for creating questions in the achievement points system.
     * Requirement: 2.1, 2.6
     *
     * @param userId The ID of the user creating the question
     * @param text The question text content
     * @param subject The subject name
     * @param subjectId The subject ID from Subjects table (0 treated as NULL)
     * @param rewardPoints The reward points offered for this question (0 or positive)
     * @param attachment Optional file attachment path
     * @param departmentId Optional department ID
     * @param semesterId Optional semester ID
     * @return true if the question was created successfully
     * @throws SQLException if a database error occurs
     */
    public boolean createQuestionWithReward(int userId, String text, String subject,
                                           int subjectId, int rewardPoints, String attachment,
                                           Integer departmentId, Integer semesterId) throws SQLException {
        // Delegate to existing createQuestion which already handles reward_points
        return createQuestion(userId, text, subject, subjectId, rewardPoints, attachment, departmentId, semesterId);
    }

    // =========================
    // MARK BEST ANSWER & TRANSFER POINTS (Requirements 3.1, 3.2, 3.3)
    // =========================

    /**
     * Marks an answer as the best answer and transfers reward points from the question
     * author to the answer author.
     *
     * This method performs the following operations atomically:
     * 1. Validates the question exists and belongs to the requesting user
     * 2. Validates the answer exists and belongs to the specified question
     * 3. Checks the question has available reward points
     * 4. Transfers points from question author to answer author
     * 5. Updates the answer's is_rewarded status
     * 6. Updates the question's best_answer_id and reward_status
     * 7. Records the transaction in RewardTransactions table
     *
     * Requirements: 3.1, 3.2, 3.3
     *
     * @param questionId The ID of the question
     * @param answerId The ID of the answer to mark as best
     * @param questionAuthorId The ID of the question author (for authorization)
     * @return true if the best answer was marked successfully, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean markBestAnswerAndTransferPoints(int questionId, int answerId, int questionAuthorId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Step 1: Get question details
                String questionSql = "SELECT user_id, reward_points, reward_status FROM Questions WHERE question_id = ?";
                int questionAuthor = -1;
                int rewardPoints = 0;

                try (PreparedStatement ps = conn.prepareStatement(questionSql)) {
                    ps.setInt(1, questionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            questionAuthor = rs.getInt("user_id");
                            rewardPoints = rs.getInt("reward_points");
                            String rewardStatus = rs.getString("reward_status");
                            // Prevent double-rewarding
                            if ("TRANSFERRED".equals(rewardStatus)) {
                                return false;
                            }
                        } else {
                            return false; // Question not found
                        }
                    }
                }

                // Validate the requesting user is the question author
                if (questionAuthor != questionAuthorId) {
                    return false;
                }

                // Get zero reward check
                if (rewardPoints <= 0) {
                    // Still mark as best answer even with 0 points
                }

                // Step 2: Get answer details
                String answerSql = "SELECT user_id, is_rewarded FROM Answers WHERE answer_id = ? AND question_id = ?";
                int answerAuthorId = -1;
                boolean answerAlreadyRewarded = false;

                try (PreparedStatement ps = conn.prepareStatement(answerSql)) {
                    ps.setInt(1, answerId);
                    ps.setInt(2, questionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            answerAuthorId = rs.getInt("user_id");
                            answerAlreadyRewarded = rs.getBoolean("is_rewarded");
                        } else {
                            return false; // Answer not found or doesn't belong to this question
                        }
                    }
                }

                // Prevent self-awarding
                if (answerAuthorId == questionAuthorId) {
                    return false;
                }

                // Prevent double-rewarding the same answer
                if (answerAlreadyRewarded) {
                    return false;
                }

                // Step 3: If there are points to transfer, deduct from author and add to answerer
                if (rewardPoints > 0) {
                    // Deduct from question author
                    String deductSql = "UPDATE Users SET achievement_points = achievement_points - ? WHERE id = ? AND achievement_points >= ?";
                    try (PreparedStatement ps = conn.prepareStatement(deductSql)) {
                        ps.setInt(1, rewardPoints);
                        ps.setInt(2, questionAuthorId);
                        ps.setInt(3, rewardPoints);
                        int updated = ps.executeUpdate();
                        if (updated == 0) {
                            // User doesn't have enough points
                            conn.rollback();
                            return false;
                        }
                    }

                    // Add to answer author
                    String addSql = "UPDATE Users SET achievement_points = achievement_points + ? WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(addSql)) {
                        ps.setInt(1, rewardPoints);
                        ps.setInt(2, answerAuthorId);
                        ps.executeUpdate();
                    }
                }

                // Step 4: Mark answer as rewarded (is_rewarded = 1)
                String updateAnswerSql = "UPDATE Answers SET is_rewarded = 1 WHERE answer_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateAnswerSql)) {
                    ps.setInt(1, answerId);
                    ps.executeUpdate();
                }

                // Step 5: Update question with best_answer_id and reward_status
                String updateQuestionSql = "UPDATE Questions SET best_answer_id = ?, reward_status = ? WHERE question_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateQuestionSql)) {
                    ps.setInt(1, answerId);
                    ps.setString(2, rewardPoints > 0 ? "TRANSFERRED" : "ACCEPTED");
                    ps.setInt(3, questionId);
                    ps.executeUpdate();
                }

                // Step 6: Record the transaction in RewardTransactions table
                String insertTransactionSql = "INSERT INTO RewardTransactions (question_id, answer_id, from_user_id, to_user_id, points, status, created_at) " +
                                               "VALUES (?, ?, ?, ?, ?, ?, GETDATE())";
                try (PreparedStatement ps = conn.prepareStatement(insertTransactionSql)) {
                    ps.setInt(1, questionId);
                    ps.setInt(2, answerId);
                    ps.setInt(3, questionAuthorId);
                    ps.setInt(4, answerAuthorId);
                    ps.setInt(5, rewardPoints);
                    ps.setString(6, rewardPoints > 0 ? "COMPLETED" : "ACCEPTED");
                    ps.executeUpdate();
                }

                conn.commit();
                
                // Publish events for real-time UI updates
                EventBus.getInstance().publish(new EventBus.PointsChangedEvent(answerAuthorId, 
                    rewardPoints > 0 ? rewardPoints : 0));
                EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
                EventBus.getInstance().publish(new EventBus.QuestionsChangedEvent());
                
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Checks if a question already has a best answer marked.
     * Requirement: 3.1
     *
     * @param questionId The ID of the question
     * @return true if a best answer has been marked, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean hasBestAnswer(int questionId) throws SQLException {
        String sql = "SELECT best_answer_id FROM Questions WHERE question_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int bestAnswerId = rs.getInt("best_answer_id");
                    return bestAnswerId > 0;
                }
                return false;
            }
        }
    }

    /**
     * Gets the best answer ID for a question.
     *
     * @param questionId The ID of the question
     * @return The best answer ID, or 0 if none
     * @throws SQLException if a database error occurs
     */
    public int getBestAnswerId(int questionId) throws SQLException {
        String sql = "SELECT best_answer_id FROM Questions WHERE question_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("best_answer_id");
                }
                return 0;
            }
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
                "q.subject, q.subjectId, q.departmentId, q.semesterId, q.question_text, COALESCE(q.tags, '') AS tags, " +
                "q.attachment_path, " +
                "q.reward_points, q.reward_status, COALESCE(q.approved, 0) AS approved, " +
                "COALESCE(q.votes, 0) AS votes, COALESCE(q.views, 0) AS views, " +
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
                "q.reward_points, q.reward_status, COALESCE(q.approved, 0) AS approved, " +
                "COALESCE(q.votes, 0) AS votes, COALESCE(q.views, 0) AS views, " +
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
                "q.reward_points, q.reward_status, COALESCE(q.approved, 0) AS approved, " +
                "COALESCE(q.votes, 0) AS votes, COALESCE(q.views, 0) AS views, " +
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
        // includes is_rewarded for requirement 9.4
        String sql = "SELECT a.answer_id AS id, a.question_id, a.user_id, " +
                "COALESCE(u.name, u.email, 'Unknown User') AS author_name, " +
                "a.answer_text, COALESCE(a.votes, 0) AS votes, " +
                "CONVERT(varchar(10), a.created_at, 120) AS created_at, " +
                "COALESCE(a.is_rewarded, 0) AS is_rewarded " +
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
            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                EventBus.getInstance().publish(new EventBus.QuestionsChangedEvent());
                EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
            }
            return success;
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
    public boolean updateQuestionVotes(int questionId, int userId) throws SQLException {

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Check if this user already voted
            String checkSql =
                    "SELECT 1 FROM QuestionVotes WHERE question_id=? AND user_id=?";

            PreparedStatement check = conn.prepareStatement(checkSql);
            check.setInt(1, questionId);
            check.setInt(2, userId);

            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                // already voted
                return false;
            }

            // Save vote
            String insertSql =
                    "INSERT INTO QuestionVotes(question_id,user_id,vote_type) VALUES(?,?,1)";

            PreparedStatement insert = conn.prepareStatement(insertSql);
            insert.setInt(1, questionId);
            insert.setInt(2, userId);
            insert.executeUpdate();

            // Increase question vote
            String updateSql =
                    "UPDATE Questions SET votes=COALESCE(votes,0)+1 WHERE question_id=?";

            PreparedStatement update = conn.prepareStatement(updateSql);
            update.setInt(1, questionId);

            return update.executeUpdate() > 0;
        }
    }

    /**
     * Updates vote count on an answer.
     * FIXED: Uses answer_id (the actual PK column) in WHERE clause.
     */
    public boolean updateAnswerVotes(int answerId, int userId) throws SQLException {

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Check if the user has already voted
            String checkSql =
                    "SELECT 1 FROM AnswerVotes WHERE answer_id=? AND user_id=?";

            PreparedStatement check = conn.prepareStatement(checkSql);
            check.setInt(1, answerId);
            check.setInt(2, userId);

            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                return false;
            }

            // Save vote
            String insertSql =
                    "INSERT INTO AnswerVotes(answer_id,user_id,vote_type) VALUES(?,?,1)";

            PreparedStatement insert = conn.prepareStatement(insertSql);
            insert.setInt(1, answerId);
            insert.setInt(2, userId);
            insert.executeUpdate();

            // Increase answer votes
            String updateSql =
                    "UPDATE Answers SET votes = COALESCE(votes,0)+1 WHERE answer_id=?";

            PreparedStatement update = conn.prepareStatement(updateSql);
            update.setInt(1, answerId);

            return update.executeUpdate() > 0;
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
    // GET QUESTION REWARD POINTS
    // =========================

    /**
     * Gets the reward points assigned to a specific question.
     * Requirement: 3.3
     *
     * @param questionId The ID of the question to get reward points for
     * @return The reward points value, or 0 if question not found
     */
    public int getQuestionRewardPoints(int questionId) throws SQLException {
        String sql = "SELECT reward_points FROM Questions WHERE question_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("reward_points") : 0;
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

    /**
     * Counts best answers for a specific user (for dashboard hero stats).
     * SQL: COUNT(*) FROM Answers WHERE user_id = ? AND is_rewarded = 1
     */
    public int countBestAnswersByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Answers WHERE user_id = ? AND is_rewarded = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // =========================
    // QUESTION BANK CRUD
    // =========================

    public int createQuestionBankEntry(Question q) throws SQLException {
        String sql = "INSERT INTO Questions (user_id, author_name, title, subject, subjectId, " +
                "question_text, tags, attachment_path, difficulty, question_type, status, " +
                "departmentId, semesterId, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, q.getUserId());
            stmt.setString(2, q.getAuthorName());
            stmt.setString(3, q.getTitle());
            stmt.setString(4, q.getSubject());
            if (q.getSubjectId() > 0) stmt.setInt(5, q.getSubjectId());
            else stmt.setNull(5, java.sql.Types.INTEGER);
            stmt.setString(6, q.getQuestionText());
            stmt.setString(7, q.getTags());
            stmt.setString(8, q.getAttachmentPath());
            stmt.setString(9, q.getDifficulty());
            stmt.setString(10, q.getQuestionType());
            stmt.setString(11, q.getStatus());
            if (q.getDepartmentId() > 0) stmt.setInt(12, q.getDepartmentId());
            else stmt.setNull(12, java.sql.Types.INTEGER);
            if (q.getSemesterId() > 0) stmt.setInt(13, q.getSemesterId());
            else stmt.setNull(13, java.sql.Types.INTEGER);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public boolean updateQuestionBankEntry(Question q) throws SQLException {
        String sql = "UPDATE Questions SET title = ?, subject = ?, subjectId = ?, question_text = ?, " +
                "tags = ?, attachment_path = ?, difficulty = ?, question_type = ?, status = ?, " +
                "departmentId = ?, semesterId = ? WHERE question_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, q.getTitle());
            stmt.setString(2, q.getSubject());
            if (q.getSubjectId() > 0) stmt.setInt(3, q.getSubjectId());
            else stmt.setNull(3, java.sql.Types.INTEGER);
            stmt.setString(4, q.getQuestionText());
            stmt.setString(5, q.getTags());
            stmt.setString(6, q.getAttachmentPath());
            stmt.setString(7, q.getDifficulty());
            stmt.setString(8, q.getQuestionType());
            stmt.setString(9, q.getStatus());
            if (q.getDepartmentId() > 0) stmt.setInt(10, q.getDepartmentId());
            else stmt.setNull(10, java.sql.Types.INTEGER);
            if (q.getSemesterId() > 0) stmt.setInt(11, q.getSemesterId());
            else stmt.setNull(11, java.sql.Types.INTEGER);
            stmt.setInt(12, q.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public Question getQuestionById(int id) throws SQLException {
        String sql = adminQuestionSelectSql() + "WHERE q.question_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapQuestionBank(rs);
            }
        }
        return null;
    }

    /** All questions for the admin panel, including optional bank metadata when columns exist. */
    public List<Question> getAllQuestionsForAdminPanel() throws SQLException {
        String sql = adminQuestionSelectSql() + "ORDER BY q.created_at DESC";
        List<Question> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapQuestionBank(rs));
        }
        return list;
    }

    public List<Question> getQuestionBankEntries() throws SQLException {
        return getAllQuestionsForAdminPanel();
    }

    private String adminQuestionSelectSql() {
        return "SELECT q.question_id AS id, q.user_id, " +
                "COALESCE(u.name, u.email, q.author_name, 'Unknown') AS author_name, " +
                "u.name, u.fullName, u.department, u.semester, " +
                "q.subject, q.subjectId, q.question_text, COALESCE(q.tags, '') AS tags, " +
                "q.attachment_path, q.reward_points, q.reward_status, COALESCE(q.approved, 0) AS approved, " +
                "COALESCE(q.votes, 0) AS votes, COALESCE(q.views, 0) AS views, " +
                "CONVERT(varchar(19), q.created_at, 120) AS created_at, COALESCE(q.is_locked, 0) AS is_locked, " +
                "q.title, q.difficulty, q.question_type, q.status, q.departmentId, q.semesterId, " +
                "d.name AS departmentName, s.name AS semesterName, " +
                "(SELECT COUNT(*) FROM Answers a WHERE a.question_id = q.question_id) AS answer_count " +
                "FROM Questions q " +
                "LEFT JOIN Users u ON q.user_id = u.id " +
                "LEFT JOIN Departments d ON q.departmentId = d.id " +
                "LEFT JOIN Semesters s ON q.semesterId = s.id ";
    }

    public List<Question> getQuestionsByUserId(int userId) throws SQLException {
        String sql = "SELECT q.question_id AS id, q.*, u.name, u.fullName, u.department, u.semester " +
                "FROM Questions q LEFT JOIN Users u ON q.user_id = u.id " +
                "WHERE q.user_id = ? ORDER BY q.created_at DESC";
        List<Question> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapQuestion(rs));
            }
        }
        return list;
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
     * @throws SQLException if a database error occurs
     */
    public boolean approveQuestion(int questionId) throws SQLException {
        String sql = "UPDATE Questions SET approved = 1 WHERE question_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);
            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                EventBus.getInstance().publish(new EventBus.QuestionsChangedEvent());
                EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
            }
            return success;
        }
    }

    /**
     * Gets the approval status of a question.
     * Requirement: 2.3
     *
     * @param questionId The ID of the question
     * @return true if the question is approved, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean isQuestionApproved(int questionId) throws SQLException {
        String sql = "SELECT approved FROM Questions WHERE question_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("approved");
                }
                return false;
            }
        }
    }

    public boolean deleteQuestionById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (DatabaseUtil.tableExists(conn, "QuestionVotes")) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM QuestionVotes WHERE question_id = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                if (DatabaseUtil.tableExists(conn, "Answers")) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Answers WHERE question_id = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                int rows;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Questions WHERE question_id = ?")) {
                    ps.setInt(1, id);
                    rows = ps.executeUpdate();
                }
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private Question mapQuestionBank(ResultSet rs) throws SQLException {
        Question q = mapQuestion(rs);
        try { q.setTitle(rs.getString("title")); } catch (SQLException ignored) {}
        try { q.setDifficulty(rs.getString("difficulty")); } catch (SQLException ignored) {}
        try { q.setQuestionType(rs.getString("question_type")); } catch (SQLException ignored) {}
        try { q.setStatus(rs.getString("status")); } catch (SQLException ignored) {}
        try {
            int deptId = rs.getInt("departmentId");
            if (!rs.wasNull()) q.setDepartmentId(deptId);
        } catch (SQLException ignored) {}
        try {
            int semId = rs.getInt("semesterId");
            if (!rs.wasNull()) q.setSemesterId(semId);
        } catch (SQLException ignored) {}
        try { q.setDepartmentName(rs.getString("departmentName")); } catch (SQLException ignored) {}
        try { q.setSemesterName(rs.getString("semesterName")); } catch (SQLException ignored) {}
        try {
            int answerCount = rs.getInt("answer_count");
            java.util.List<Answer> placeholder = new java.util.ArrayList<>();
            for (int i = 0; i < answerCount; i++) placeholder.add(null);
            q.setAnswers(placeholder);
        } catch (SQLException ignored) {}
        return q;
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

        try {
            int deptId = rs.getInt("departmentId");
            if (!rs.wasNull()) q.setDepartmentId(deptId);
        } catch (SQLException ignored) {}
        try {
            int semId = rs.getInt("semesterId");
            if (!rs.wasNull()) q.setSemesterId(semId);
        } catch (SQLException ignored) {}

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

        // Load reward status and approval status
        try {
            String rewardStatus = rs.getString("reward_status");
            if (rewardStatus != null) {
                q.setRewardStatus(rewardStatus);
            }
        } catch (SQLException ignored) {}
        try {
            boolean approved = rs.getBoolean("approved");
            q.setApproved(approved);
        } catch (SQLException ignored) {}

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
        Answer answer = new Answer(
                rs.getInt("id"),              // aliased from answer_id
                rs.getInt("question_id"),
                rs.getInt("user_id"),
                rs.getString("author_name"),
                rs.getString("answer_text"),
                rs.getInt("votes"),
                rs.getString("created_at")
        );
        // Set is_rewarded from query result (Requirement 9.4)
        try {
            int isRewarded = rs.getInt("is_rewarded");
            if (!rs.wasNull()) {
                answer.setRewarded(isRewarded == 1);
            }
        } catch (SQLException ignored) {
            // Column may not exist in older schemas
        }
        return answer;
    }

    private static void setNullableInt(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null || value == 0) {
            stmt.setNull(index, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }

    // =========================
    // CHECK ANSWER IS REWARDED (Requirement 9.4)
    // =========================

    /**
     * Checks if an answer has been marked as rewarded/best answer.
     * Requirement: 9.4
     *
     * @param answerId The ID of the answer to check
     * @return true if the answer is rewarded, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean isAnswerRewarded(int answerId) throws SQLException {
        String sql = "SELECT is_rewarded FROM Answers WHERE answer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, answerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_rewarded");
                }
                return false;
            }
        }
    }

    // =========================
    // MARK BEST ANSWER (Requirement 3)
    // =========================

    /**
     * Marks an answer as the best answer and transfers reward points to the answer author.
     * Only administrators can call this method.
     * Requirements: 3.1, 3.2, 3.3
     *
     * This method:
     * 1. Validates the answer exists and hasn't been rewarded yet
     * 2. Gets the reward_points from the question
     * 3. If there are reward points, transfers them from question author to answer author
     * 4. Marks the answer as rewarded (is_rewarded = true)
     * 5. All operations are atomic (all-or-nothing)
     *
     * @param questionId The ID of the question
     * @param answerId The ID of the answer to mark as best
     * @return true if the best answer was marked successfully, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean markBestAnswer(int questionId, int answerId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Step 1: Load question reward state
                int questionAuthorId = -1;
                int rewardPoints = 0;
                String rewardStatus = "NONE";
                String questionSql = "SELECT user_id, reward_points, reward_status FROM Questions WHERE question_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(questionSql)) {
                    ps.setInt(1, questionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            questionAuthorId = rs.getInt("user_id");
                            rewardPoints = rs.getInt("reward_points");
                            rewardStatus = rs.getString("reward_status");
                        } else {
                            return false;
                        }
                    }
                }

                if ("TRANSFERRED".equalsIgnoreCase(rewardStatus)) {
                    return false;
                }

                // Step 2: Validate answer belongs to question and is not already rewarded
                int answerAuthorId = -1;
                String answerSql = "SELECT user_id, is_rewarded FROM Answers WHERE answer_id = ? AND question_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(answerSql)) {
                    ps.setInt(1, answerId);
                    ps.setInt(2, questionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            if (rs.getBoolean("is_rewarded")) {
                                return false;
                            }
                            answerAuthorId = rs.getInt("user_id");
                        } else {
                            return false;
                        }
                    }
                }

                if (questionAuthorId == answerAuthorId) {
                    return false;
                }

                int answerAuthorNewBalance = 0;

                // Step 3: Transfer escrowed reward to answer author (already deducted at question post)
                if (rewardPoints > 0) {
                    String addSql = "UPDATE Users SET achievement_points = achievement_points + ? OUTPUT INSERTED.achievement_points WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(addSql)) {
                        ps.setInt(1, rewardPoints);
                        ps.setInt(2, answerAuthorId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                conn.rollback();
                                return false;
                            }
                            answerAuthorNewBalance = rs.getInt("achievement_points");
                        }
                    }
                }

                // Step 4: Mark answer rewarded and update question best-answer metadata
                String updateAnswerSql = "UPDATE Answers SET is_rewarded = 1 WHERE answer_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateAnswerSql)) {
                    ps.setInt(1, answerId);
                    ps.executeUpdate();
                }

                String updateQuestionSql = "UPDATE Questions SET best_answer_id = ?, reward_status = ? WHERE question_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateQuestionSql)) {
                    ps.setInt(1, answerId);
                    ps.setString(2, rewardPoints > 0 ? "TRANSFERRED" : "ACCEPTED");
                    ps.setInt(3, questionId);
                    ps.executeUpdate();
                }

                conn.commit();

                if (rewardPoints > 0) {
                    com.studybuddy.utils.EventBus.getInstance()
                        .publish(new com.studybuddy.utils.EventBus.PointsChangedEvent(answerAuthorId, answerAuthorNewBalance));
                }

                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
