package com.studybuddy.admin.dao;

import com.studybuddy.models.User;
import com.studybuddy.models.Resource;
import com.studybuddy.models.Question;
import com.studybuddy.models.Answer;
import com.studybuddy.models.Note;
import com.studybuddy.utils.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {
    private static AdminDAO instance;

    private AdminDAO() {}

    public static synchronized AdminDAO getInstance() {
        if (instance == null) {
            instance = new AdminDAO();
        }
        return instance;
    }

    // ===========================
    // User Moderation
    // ===========================
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, name, email, role, status, created_at FROM Users ORDER BY id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setEmail(rs.getString("email"));
                u.setRole(rs.getString("role"));
                u.setStatus(rs.getString("status"));
                u.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                        rs.getTimestamp("created_at").toLocalDateTime() : null);
                list.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateUserStatus(int userId, String status) {
        String sql = "UPDATE Users SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUserInfo(int userId, String name, String email, String role) {
        String sql = "UPDATE Users SET name = ?, email = ?, role = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, role);
            stmt.setInt(4, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Tasks WHERE user_id = ?")) {
                    ps.setInt(1, userId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Resources WHERE uploadedBy = ?")) {
                    ps.setInt(1, userId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Answers WHERE user_id = ?")) {
                    ps.setInt(1, userId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Questions WHERE user_id = ?")) {
                    ps.setInt(1, userId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Notes WHERE userId = ?")) {
                    ps.setInt(1, userId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Users WHERE id = ?")) {
                    ps.setInt(1, userId); ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===========================
    // Content Moderation (Resources & Notes)
    // ===========================
    public List<Resource> getAllResources() {
        List<Resource> list = new ArrayList<>();
        String sql = "SELECT * FROM Resources ORDER BY id DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
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
                r.setUploadDate(rs.getTimestamp("uploadDate") != null ? 
                        rs.getTimestamp("uploadDate").toString() : "");
                r.setFilePath(rs.getString("filePath"));
                r.setFileType(rs.getString("fileType"));
                r.setDownloads(rs.getInt("downloads"));
                r.setActive(rs.getBoolean("isActive"));
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateResourceStatus(int resourceId, boolean approved) {
        String sql = "UPDATE Resources SET isActive = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, approved);
            stmt.setInt(2, resourceId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Note> getAllNotes() {
        List<Note> list = new ArrayList<>();
        // Query public notes
        String sql = "SELECT * FROM Notes WHERE isPrivate = 0 ORDER BY id DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Note n = new Note();
                n.setId(rs.getInt("id"));
                n.setTitle(rs.getString("title"));
                n.setSubject(rs.getString("subject"));
                n.setSource(rs.getString("source"));
                n.setUploadDate(rs.getTimestamp("uploadDate") != null ? 
                        rs.getTimestamp("uploadDate").toString() : "");
                n.setFileType(rs.getString("fileType"));
                n.setFileName(rs.getString("fileName"));
                n.setFilePath(rs.getString("filePath"));
                n.setDescription(rs.getString("description"));
                n.setUserId(rs.getInt("userId"));
                n.setPrivate(rs.getBoolean("isPrivate"));
                n.setStatus(rs.getString("status"));
                list.add(n);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateNoteStatus(int noteId, boolean approved) {
        String sql = "UPDATE Notes SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, approved ? "Approved" : "Rejected");
            stmt.setInt(2, noteId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===========================
    // Q&A Moderation
    // ===========================
    public List<Question> getAllQuestions() {
        List<Question> list = new ArrayList<>();
        String qSql = "SELECT * FROM Questions ORDER BY question_id DESC";
        String aSql = "SELECT * FROM Answers WHERE question_id = ? ORDER BY answer_id ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement qStmt = conn.prepareStatement(qSql);
             ResultSet qRs = qStmt.executeQuery()) {
            while (qRs.next()) {
                Question q = new Question();
                q.setId(qRs.getInt("question_id"));
                q.setUserId(qRs.getInt("user_id"));
                q.setAuthorName(qRs.getString("author_name"));
                q.setSubject(qRs.getString("subject"));
                q.setQuestionText(qRs.getString("question_text"));
                q.setTags(qRs.getString("tags"));
                q.setAttachmentPath(qRs.getString("attachment_path"));
                q.setRewardPoints(qRs.getInt("reward_points"));
                q.setVotes(qRs.getInt("votes"));
                q.setViews(qRs.getInt("views"));
                q.setCreatedAt(qRs.getTimestamp("created_at") != null ? 
                        qRs.getTimestamp("created_at").toString() : "");
                q.setLocked(qRs.getBoolean("is_locked"));

                // Load answers for this question
                List<Answer> answers = new ArrayList<>();
                try (PreparedStatement aStmt = conn.prepareStatement(aSql)) {
                    aStmt.setInt(1, q.getId());
                    try (ResultSet aRs = aStmt.executeQuery()) {
                        while (aRs.next()) {
                            Answer a = new Answer();
                            a.setId(aRs.getInt("answer_id"));
                            a.setQuestionId(aRs.getInt("question_id"));
                            a.setUserId(aRs.getInt("user_id"));
                            a.setAuthorName(aRs.getString("author_name"));
                            a.setAnswerText(aRs.getString("answer_text"));
                            a.setVotes(aRs.getInt("votes"));
                            a.setCreatedAt(aRs.getTimestamp("created_at") != null ? 
                                    aRs.getTimestamp("created_at").toString() : "");
                            answers.add(a);
                        }
                    }
                }
                q.setAnswers(answers);
                list.add(q);
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteAnswer(int answerId) {
        String sql = "DELETE FROM Answers WHERE answer_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, answerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean toggleLockDiscussion(int questionId) {
        String selectSql = "SELECT is_locked FROM Questions WHERE question_id = ?";
        String updateSql = "UPDATE Questions SET is_locked = ? WHERE question_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setInt(1, questionId);
            boolean currentLocked = false;
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    currentLocked = rs.getBoolean("is_locked");
                }
            }
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setBoolean(1, !currentLocked);
                updateStmt.setInt(2, questionId);
                return updateStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
