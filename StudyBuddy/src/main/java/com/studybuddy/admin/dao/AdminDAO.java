package com.studybuddy.admin.dao;

import com.studybuddy.models.*;
import com.studybuddy.utils.DatabaseUtil;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Central admin DAO providing all data-access operations used by the admin module.
 * Every method uses PreparedStatement and try-with-resources.
 */
public class AdminDAO {

    private static final Logger logger = Logger.getLogger(AdminDAO.class.getName());
    private static AdminDAO instance;

    private AdminDAO() {}

    public static synchronized AdminDAO getInstance() {
        if (instance == null) instance = new AdminDAO();
        return instance;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Dashboard Statistics
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Returns a map of key → integer statistics for the dashboard overview cards.
     * Keys: totalUsers, totalNotes, totalResources, totalQuestions,
     *       totalAnswers, totalTasks, newUsersToday, uploadsToday,
     *       pendingNotes, pendingResources
     */
    public Map<String, Integer> getDashboardStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String[] queries = {
            "SELECT COUNT(*) FROM Users",
            "SELECT COUNT(*) FROM Notes WHERE status != 'Deleted'",
            "SELECT COUNT(*) FROM Resources WHERE isActive = 1",
            "SELECT COUNT(*) FROM Questions",
            "SELECT COUNT(*) FROM Answers",
            "SELECT COUNT(*) FROM Tasks",
            "SELECT COUNT(*) FROM Users WHERE CAST(created_at AS DATE) = CAST(GETDATE() AS DATE)",
            "SELECT COUNT(*) FROM Notes WHERE CAST(uploadDate AS DATE) = CAST(GETDATE() AS DATE) AND status != 'Deleted'",
            "SELECT COUNT(*) FROM Notes WHERE status = 'Pending'",
            "SELECT COUNT(*) FROM Resources WHERE isActive = 0"
        };
        String[] keys = {
            "totalUsers", "totalNotes", "totalResources", "totalQuestions",
            "totalAnswers", "totalTasks", "newUsersToday", "uploadsToday",
            "pendingNotes", "pendingResources"
        };

        try (Connection conn = DatabaseUtil.getConnection()) {
            for (int i = 0; i < queries.length; i++) {
                try (PreparedStatement ps = conn.prepareStatement(queries[i]);
                     ResultSet rs = ps.executeQuery()) {
                    stats.put(keys[i], rs.next() ? rs.getInt(1) : 0);
                } catch (SQLException e) {
                    stats.put(keys[i], 0);
                    logger.warning("Stat query failed [" + keys[i] + "]: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.warning("getDashboardStats connection failed: " + e.getMessage());
        }
        return stats;
    }

    /** Most recent N users, ordered by creation date. */
    public List<User> getRecentUsers(int limit) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT TOP (?) id, name, email, role, status, created_at FROM Users ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapUserBasic(rs));
            }
        } catch (SQLException e) {
            logger.warning("getRecentUsers failed: " + e.getMessage());
        }
        return list;
    }

    /** Most recent N uploads (notes), ordered by upload date. */
    public List<Note> getRecentUploads(int limit) {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT TOP (?) id, title, subject, userId, uploadDate, status FROM Notes WHERE status != 'Deleted' ORDER BY uploadDate DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Note n = new Note();
                    n.setId(rs.getInt("id"));
                    n.setTitle(rs.getString("title"));
                    n.setSubject(rs.getString("subject"));
                    n.setUserId(rs.getInt("userId"));
                    n.setUploadDate(rs.getTimestamp("uploadDate") != null ? rs.getTimestamp("uploadDate").toString() : "");
                    n.setStatus(rs.getString("status"));
                    list.add(n);
                }
            }
        } catch (SQLException e) {
            logger.warning("getRecentUploads failed: " + e.getMessage());
        }
        return list;
    }

    /** Most recent N questions. */
    public List<Question> getRecentQuestions(int limit) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT TOP (?) question_id, question_text, subject, author_name, votes, views, created_at FROM Questions ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Question q = new Question();
                    q.setId(rs.getInt("question_id"));
                    q.setQuestionText(rs.getString("question_text"));
                    q.setSubject(rs.getString("subject"));
                    q.setAuthorName(rs.getString("author_name"));
                    q.setVotes(rs.getInt("votes"));
                    q.setViews(rs.getInt("views"));
                    q.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "");
                    list.add(q);
                }
            }
        } catch (SQLException e) {
            logger.warning("getRecentQuestions failed: " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // User Moderation
    // ══════════════════════════════════════════════════════════════════════════

    /** All users with extended profile fields. */
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = """
                SELECT id, name, email, role, status, department, semester,
                       points, created_at
                FROM Users
                ORDER BY created_at DESC
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = mapUserFull(rs);
                list.add(u);
            }
        } catch (SQLException e) {
            logger.warning("getAllUsers failed: " + e.getMessage());
        }
        return list;
    }

    /** Search users by name, email, department, or role. */
    public List<User> searchUsers(String query) {
        String like = "%" + query.toLowerCase() + "%";
        String sql = """
                SELECT id, name, email, role, status, department, semester, points, created_at
                FROM Users
                WHERE LOWER(name)       LIKE ?
                   OR LOWER(email)      LIKE ?
                   OR LOWER(department) LIKE ?
                   OR LOWER(role)       LIKE ?
                ORDER BY created_at DESC
                """;
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like);
            ps.setString(3, like); ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapUserFull(rs));
            }
        } catch (SQLException e) {
            logger.warning("searchUsers failed: " + e.getMessage());
        }
        return list;
    }

    public boolean updateUserStatus(int userId, String status) {
        return executeUpdate("UPDATE Users SET status = ? WHERE id = ?", ps -> {
            ps.setString(1, status); ps.setInt(2, userId);
        });
    }

    public boolean updateUserInfo(int userId, String name, String email, String role,
                                   String department, String semester) {
        String sql = "UPDATE Users SET name=?, email=?, role=?, department=?, semester=? WHERE id=?";
        return executeUpdate(sql, ps -> {
            ps.setString(1, name); ps.setString(2, email); ps.setString(3, role);
            ps.setString(4, department); ps.setString(5, semester); ps.setInt(6, userId);
        });
    }

    public boolean resetUserPassword(int userId, String hashedPassword) {
        return executeUpdate("UPDATE Users SET password = ? WHERE id = ?", ps -> {
            ps.setString(1, hashedPassword); ps.setInt(2, userId);
        });
    }

    public boolean promoteToAdmin(int userId) {
        return executeUpdate("UPDATE Users SET role = 'ADMIN' WHERE id = ?", ps -> ps.setInt(1, userId));
    }

    public boolean demoteToUser(int userId) {
        return executeUpdate("UPDATE Users SET role = 'STUDENT' WHERE id = ?", ps -> ps.setInt(1, userId));
    }

    /**
     * Soft delete: marks the user as 'Deleted' rather than removing the row.
     */
    public boolean softDeleteUser(int userId) {
        return executeUpdate("UPDATE Users SET status = 'Deleted' WHERE id = ?", ps -> ps.setInt(1, userId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Notes Moderation
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Returns ALL notes (public and private) excluding soft-deleted ones.
     * Admin can see everything.
     */
    public List<Note> getAllNotesAdmin() {
        List<Note> list = new ArrayList<>();
        String sql = """
                SELECT n.id, n.title, n.subject, n.source, n.uploadDate,
                       n.fileType, n.fileName, n.filePath, n.description,
                       n.userId, n.isPrivate, n.status,
                       u.name AS uploaderName
                FROM Notes n
                LEFT JOIN Users u ON n.userId = u.id
                WHERE n.status != 'Deleted'
                ORDER BY n.uploadDate DESC
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Note n = new Note();
                n.setId(rs.getInt("id"));
                n.setTitle(rs.getString("title"));
                n.setSubject(rs.getString("subject"));
                n.setSource(rs.getString("source"));
                n.setUploadDate(rs.getTimestamp("uploadDate") != null ? rs.getTimestamp("uploadDate").toString() : "");
                n.setFileType(rs.getString("fileType"));
                n.setFileName(rs.getString("fileName"));
                n.setFilePath(rs.getString("filePath"));
                n.setDescription(rs.getString("description"));
                n.setUserId(rs.getInt("userId"));
                n.setPrivate(rs.getBoolean("isPrivate"));
                n.setStatus(rs.getString("status"));
                // Attach uploader name using the source field as a convenient carrier
                String uploader = rs.getString("uploaderName");
                if (uploader != null) n.setSource(uploader);
                list.add(n);
            }
        } catch (SQLException e) {
            logger.warning("getAllNotesAdmin failed: " + e.getMessage());
        }
        return list;
    }

    public boolean updateNoteStatus(int noteId, String status) {
        return executeUpdate("UPDATE Notes SET status = ? WHERE id = ?", ps -> {
            ps.setString(1, status); ps.setInt(2, noteId);
        });
    }

    public boolean updateNoteVisibility(int noteId, boolean isPrivate) {
        return executeUpdate("UPDATE Notes SET isPrivate = ? WHERE id = ?", ps -> {
            ps.setBoolean(1, isPrivate); ps.setInt(2, noteId);
        });
    }

    /** Soft delete: set status = 'Deleted'. */
    public boolean softDeleteNote(int noteId) {
        return executeUpdate("UPDATE Notes SET status = 'Deleted' WHERE id = ?", ps -> ps.setInt(1, noteId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Resource Moderation
    // ══════════════════════════════════════════════════════════════════════════

    public List<Resource> getAllResources() {
        List<Resource> list = new ArrayList<>();
        String sql = """
                SELECT r.*, u.name AS uploaderName
                FROM Resources r
                LEFT JOIN Users u ON r.uploadedBy = u.id
                ORDER BY r.uploadDate DESC
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Resource r = new Resource();
                r.setId(rs.getInt("id"));
                int noteIdVal = rs.getInt("noteId");
                r.setNoteId(rs.wasNull() ? null : noteIdVal);
                r.setUploadedBy(rs.getInt("uploadedBy"));
                r.setTitle(rs.getString("title"));
                r.setSubject(rs.getString("subject"));
                r.setSource(rs.getString("source"));
                r.setDescription(rs.getString("description"));
                r.setUploadDate(rs.getTimestamp("uploadDate") != null ? rs.getTimestamp("uploadDate").toString() : "");
                r.setFilePath(rs.getString("filePath"));
                r.setFileType(rs.getString("fileType"));
                r.setDownloads(rs.getInt("downloads"));
                r.setActive(rs.getBoolean("isActive"));
                // store uploader name in source as convenient carrier
                String uploader = rs.getString("uploaderName");
                if (uploader != null) r.setSource(uploader);
                list.add(r);
            }
        } catch (SQLException e) {
            logger.warning("getAllResources failed: " + e.getMessage());
        }
        return list;
    }

    public boolean updateResourceStatus(int resourceId, boolean active) {
        return executeUpdate("UPDATE Resources SET isActive = ? WHERE id = ?", ps -> {
            ps.setBoolean(1, active); ps.setInt(2, resourceId);
        });
    }

    /** Hard delete a resource record. */
    public boolean deleteResource(int resourceId) {
        return executeUpdate("DELETE FROM Resources WHERE id = ?", ps -> ps.setInt(1, resourceId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Q&A Moderation
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Loads all questions with their answer count (not full answer list – use
     * getAnswersForQuestion when needed).
     */
    public List<Question> getAllQuestions() {
        List<Question> list = new ArrayList<>();
        String sql = """
                SELECT q.question_id, q.user_id, q.author_name, q.subject,
                       q.question_text, q.tags, q.reward_points, q.votes,
                       q.views, q.created_at, q.is_locked,
                       (SELECT COUNT(*) FROM Answers a WHERE a.question_id = q.question_id) AS answer_count
                FROM Questions q
                ORDER BY q.created_at DESC
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("question_id"));
                q.setUserId(rs.getInt("user_id"));
                q.setAuthorName(rs.getString("author_name"));
                q.setSubject(rs.getString("subject"));
                q.setQuestionText(rs.getString("question_text"));
                q.setTags(rs.getString("tags"));
                q.setRewardPoints(rs.getInt("reward_points"));
                q.setVotes(rs.getInt("votes"));
                q.setViews(rs.getInt("views"));
                q.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "");
                q.setLocked(rs.getBoolean("is_locked"));
                // Store answer count; Question model already has an answers list
                int answerCount = rs.getInt("answer_count");
                // We use an empty list as placeholder; the count is returned via answers.size() == answerCount
                List<Answer> placeholder = new ArrayList<>();
                for (int i = 0; i < answerCount; i++) placeholder.add(null);
                q.setAnswers(placeholder);
                list.add(q);
            }
        } catch (SQLException e) {
            logger.warning("getAllQuestions failed: " + e.getMessage());
        }
        return list;
    }

    /** Load full answer objects for a specific question (used in detail view). */
    public List<Answer> getAnswersForQuestion(int questionId) {
        List<Answer> list = new ArrayList<>();
        String sql = "SELECT * FROM Answers WHERE question_id = ? ORDER BY answer_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Answer a = new Answer();
                    a.setId(rs.getInt("answer_id"));
                    a.setQuestionId(rs.getInt("question_id"));
                    a.setUserId(rs.getInt("user_id"));
                    a.setAuthorName(rs.getString("author_name"));
                    a.setAnswerText(rs.getString("answer_text"));
                    a.setVotes(rs.getInt("votes"));
                    a.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "");
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            logger.warning("getAnswersForQuestion failed: " + e.getMessage());
        }
        return list;
    }

    public boolean deleteQuestion(int questionId) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Answers WHERE question_id = ?")) {
                    ps.setInt(1, questionId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Questions WHERE question_id = ?")) {
                    ps.setInt(1, questionId); ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                logger.warning("deleteQuestion failed: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.warning("deleteQuestion connection failed: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteAnswer(int answerId) {
        return executeUpdate("DELETE FROM Answers WHERE answer_id = ?", ps -> ps.setInt(1, answerId));
    }

    public boolean setQuestionLocked(int questionId, boolean locked) {
        return executeUpdate("UPDATE Questions SET is_locked = ? WHERE question_id = ?", ps -> {
            ps.setBoolean(1, locked); ps.setInt(2, questionId);
        });
    }

    public boolean toggleLockDiscussion(int questionId) {
        String selectSql = "SELECT is_locked FROM Questions WHERE question_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement selectPs = conn.prepareStatement(selectSql)) {
            selectPs.setInt(1, questionId);
            boolean currentLocked = false;
            try (ResultSet rs = selectPs.executeQuery()) {
                if (rs.next()) currentLocked = rs.getBoolean("is_locked");
            }
            return setQuestionLocked(questionId, !currentLocked);
        } catch (SQLException e) {
            logger.warning("toggleLockDiscussion failed: " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Reports
    // ══════════════════════════════════════════════════════════════════════════

    /** Top N most active users by total notes + questions + answers. */
    public List<Map<String, Object>> getTopActiveUsers(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT TOP (?) u.name,
                    (SELECT COUNT(*) FROM Notes    WHERE userId    = u.id AND status != 'Deleted') +
                    (SELECT COUNT(*) FROM Questions WHERE user_id  = u.id) +
                    (SELECT COUNT(*) FROM Answers   WHERE user_id  = u.id) AS activity_score
                FROM Users u
                ORDER BY activity_score DESC
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", rs.getString("name"));
                    row.put("score", rs.getInt("activity_score"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logger.warning("getTopActiveUsers failed: " + e.getMessage());
        }
        return list;
    }

    /** Top N most downloaded resources. */
    public List<Map<String, Object>> getTopDownloadedResources(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT TOP (?) title, downloads FROM Resources ORDER BY downloads DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("title", rs.getString("title"));
                    row.put("downloads", rs.getInt("downloads"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logger.warning("getTopDownloadedResources failed: " + e.getMessage());
        }
        return list;
    }

    /** Monthly upload counts (Notes) for the current year. */
    public Map<String, Integer> getMonthlyUploads() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = """
                SELECT MONTH(uploadDate) AS mon, COUNT(*) AS cnt
                FROM Notes
                WHERE YEAR(uploadDate) = YEAR(GETDATE()) AND status != 'Deleted'
                GROUP BY MONTH(uploadDate)
                ORDER BY mon
                """;
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        for (String m : months) result.put(m, 0);
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int mon = rs.getInt("mon");
                if (mon >= 1 && mon <= 12) result.put(months[mon - 1], rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            logger.warning("getMonthlyUploads failed: " + e.getMessage());
        }
        return result;
    }

    /** Monthly user registrations for the current year. */
    public Map<String, Integer> getMonthlyRegistrations() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = """
                SELECT MONTH(created_at) AS mon, COUNT(*) AS cnt
                FROM Users
                WHERE YEAR(created_at) = YEAR(GETDATE())
                GROUP BY MONTH(created_at)
                ORDER BY mon
                """;
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        for (String m : months) result.put(m, 0);
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int mon = rs.getInt("mon");
                if (mon >= 1 && mon <= 12) result.put(months[mon - 1], rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            logger.warning("getMonthlyRegistrations failed: " + e.getMessage());
        }
        return result;
    }

    /** Subject-wise note count. */
    public Map<String, Integer> getNotesBySubject() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = """
                SELECT subject, COUNT(*) AS cnt
                FROM Notes
                WHERE status != 'Deleted'
                GROUP BY subject
                ORDER BY cnt DESC
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.put(rs.getString("subject"), rs.getInt("cnt"));
        } catch (SQLException e) {
            logger.warning("getNotesBySubject failed: " + e.getMessage());
        }
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Global Search
    // ══════════════════════════════════════════════════════════════════════════

    public List<User> searchUsersGlobal(String q) {
        return searchUsers(q);
    }

    public List<Note> searchNotes(String query) {
        List<Note> list = new ArrayList<>();
        String like = "%" + query.toLowerCase() + "%";
        String sql = """
                SELECT id, title, subject, userId, uploadDate, status
                FROM Notes
                WHERE status != 'Deleted'
                  AND (LOWER(title) LIKE ? OR LOWER(subject) LIKE ?)
                ORDER BY uploadDate DESC
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Note n = new Note();
                    n.setId(rs.getInt("id")); n.setTitle(rs.getString("title"));
                    n.setSubject(rs.getString("subject")); n.setUserId(rs.getInt("userId"));
                    n.setStatus(rs.getString("status"));
                    list.add(n);
                }
            }
        } catch (SQLException e) {
            logger.warning("searchNotes failed: " + e.getMessage());
        }
        return list;
    }

    public List<Resource> searchResources(String query) {
        List<Resource> list = new ArrayList<>();
        String like = "%" + query.toLowerCase() + "%";
        String sql = "SELECT id, title, subject, downloads, isActive FROM Resources WHERE LOWER(title) LIKE ? OR LOWER(subject) LIKE ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Resource r = new Resource();
                    r.setId(rs.getInt("id")); r.setTitle(rs.getString("title"));
                    r.setSubject(rs.getString("subject")); r.setDownloads(rs.getInt("downloads"));
                    r.setActive(rs.getBoolean("isActive"));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            logger.warning("searchResources failed: " + e.getMessage());
        }
        return list;
    }

    public List<Question> searchQuestions(String query) {
        List<Question> list = new ArrayList<>();
        String like = "%" + query.toLowerCase() + "%";
        String sql = """
                SELECT question_id, question_text, subject, author_name, votes, views, created_at, is_locked
                FROM Questions
                WHERE LOWER(question_text) LIKE ? OR LOWER(subject) LIKE ? OR LOWER(author_name) LIKE ?
                ORDER BY created_at DESC
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Question q = new Question();
                    q.setId(rs.getInt("question_id")); q.setQuestionText(rs.getString("question_text"));
                    q.setSubject(rs.getString("subject")); q.setAuthorName(rs.getString("author_name"));
                    q.setVotes(rs.getInt("votes")); q.setViews(rs.getInt("views"));
                    q.setLocked(rs.getBoolean("is_locked"));
                    list.add(q);
                }
            }
        } catch (SQLException e) {
            logger.warning("searchQuestions failed: " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private boolean executeUpdate(String sql, ParamSetter setter) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.warning("executeUpdate failed [" + sql.substring(0, Math.min(40, sql.length())) + "]: " + e.getMessage());
            return false;
        }
    }

    private User mapUserBasic(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id")); u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email")); u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }

    private User mapUserFull(ResultSet rs) throws SQLException {
        User u = mapUserBasic(rs);
        try { u.setDepartment(rs.getString("department")); } catch (SQLException ignored) {}
        try { u.setSemester(rs.getString("semester")); } catch (SQLException ignored) {}
        try { u.setPoints(rs.getInt("points")); } catch (SQLException ignored) {}
        return u;
    }
}
