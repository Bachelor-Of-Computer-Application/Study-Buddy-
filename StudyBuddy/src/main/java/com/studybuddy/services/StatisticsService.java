
package com.studybuddy.services;

import com.studybuddy.dao.*;
import java.sql.SQLException;

public class StatisticsService {
    private final NoteDAO noteDAO = new NoteDAO();
    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final UserDAO userDAO = new UserDAO();

    // User profile statistics
    public int getNotesUploaded(int userId) throws SQLException {
        return noteDAO.countNotesByUser(userId);
    }

    public int getResourcesUploaded(int userId) throws SQLException {
        return resourceDAO.countResourcesByUser(userId);
    }

    public int getQuestionsAsked(int userId) throws SQLException {
        return questionDAO.countQuestionsByUser(userId);
    }

    public int getAnswersSubmitted(int userId) throws SQLException {
        return questionDAO.countAnswersByUser(userId);
    }

    // Note status counts by user
    public int getPendingNotes(int userId) throws SQLException {
        return countNotesByStatus(userId, "Pending");
    }

    public int getApprovedNotes(int userId) throws SQLException {
        return countNotesByStatus(userId, "Approved");
    }

    public int getRejectedNotes(int userId) throws SQLException {
        return countNotesByStatus(userId, "Rejected");
    }

    private int countNotesByStatus(int userId, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Notes WHERE userId = ? AND status = ?";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, status);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Total downloads and views
    public int getTotalDownloads(int userId) throws SQLException {
        // Sum downloads from resources uploaded by user
        String sql = "SELECT COALESCE(SUM(downloads), 0) FROM Resources WHERE uploadedBy = ?";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int getTotalViews(int userId) throws SQLException {
        // Sum views from questions asked by user
        String sql = "SELECT COALESCE(SUM(views), 0) FROM Questions WHERE user_id = ?";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Admin dashboard statistics
    public int getTotalUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Users";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int getActiveUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Users WHERE status = 'Active'";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int getDisabledUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Users WHERE status = 'Disabled'";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int getBCAStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Users WHERE department = 'BCA'";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int getBBAStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Users WHERE department = 'BBA'";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int getPendingNotes() throws SQLException {
        return countAllNotesByStatus("Pending");
    }

    public int getApprovedNotes() throws SQLException {
        return countAllNotesByStatus("Approved");
    }

    public int getRejectedNotes() throws SQLException {
        return countAllNotesByStatus("Rejected");
    }

    private int countAllNotesByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Notes WHERE status = ?";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Resources status counts
    public int getPendingResources() throws SQLException {
        return countResourcesByStatus("Pending");
    }

    public int getApprovedResources() throws SQLException {
        return countResourcesByStatus("Approved");
    }

    public int getRejectedResources() throws SQLException {
        return countResourcesByStatus("Rejected");
    }

    private int countResourcesByStatus(String status) throws SQLException {
        // Note: Resource table might use isActive instead of status; adjust if needed
        String sql = "SELECT COUNT(*) FROM Resources WHERE status = ?";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            // Fallback to isActive if status column doesn't exist
            if ("Approved".equals(status)) {
                String fallbackSql = "SELECT COUNT(*) FROM Resources WHERE isActive = 1";
                try (var conn = DatabaseConnection.getConnection();
                     var stmt = conn.createStatement();
                     var rs = stmt.executeQuery(fallbackSql)) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
            return 0;
        }
    }

    public int getTotalQuestions() throws SQLException {
        return questionDAO.countAllQuestions();
    }

    public int getTotalAnswers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Answers";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int getTotalReports() throws SQLException {
        // Assuming there's a Reports table; adjust if needed
        String sql = "SELECT COUNT(*) FROM Reports";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            // If Reports table doesn't exist, return 0
            return 0;
        }
    }

    // Time-based upload statistics
    public int getTodayUploads() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Notes WHERE CAST(uploadDate AS DATE) = CAST(GETDATE() AS DATE) " +
                     "UNION ALL " +
                     "SELECT COUNT(*) FROM Resources WHERE CAST(uploadDate AS DATE) = CAST(GETDATE() AS DATE)";
        int total = 0;
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                total += rs.getInt(1);
            }
        }
        return total;
    }

    public int getWeekUploads() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Notes WHERE uploadDate >= DATEADD(DAY, -7, GETDATE()) " +
                     "UNION ALL " +
                     "SELECT COUNT(*) FROM Resources WHERE uploadDate >= DATEADD(DAY, -7, GETDATE())";
        int total = 0;
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                total += rs.getInt(1);
            }
        }
        return total;
    }

    public int getMonthUploads() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Notes WHERE uploadDate >= DATEADD(MONTH, -1, GETDATE()) " +
                     "UNION ALL " +
                     "SELECT COUNT(*) FROM Resources WHERE uploadDate >= DATEADD(MONTH, -1, GETDATE())";
        int total = 0;
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                total += rs.getInt(1);
            }
        }
        return total;
    }
}
