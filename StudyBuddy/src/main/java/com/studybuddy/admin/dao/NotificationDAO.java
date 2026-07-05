package com.studybuddy.admin.dao;

import com.studybuddy.models.Notification;
import com.studybuddy.utils.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO for the Notifications table.
 */
public class NotificationDAO {

    private static final Logger logger = Logger.getLogger(NotificationDAO.class.getName());
    private static NotificationDAO instance;

    private NotificationDAO() {}

    public static synchronized NotificationDAO getInstance() {
        if (instance == null) instance = new NotificationDAO();
        return instance;
    }

    private List<Integer> resolveUserIds(String recipientType, String recipientValue) {
        List<Integer> ids = new ArrayList<>();
        String sql = "";
        if ("ALL".equalsIgnoreCase(recipientType)) {
            sql = "SELECT id FROM Users";
        } else if ("DEPARTMENT".equalsIgnoreCase(recipientType)) {
            sql = "SELECT id FROM Users WHERE department = ?";
        } else if ("SEMESTER".equalsIgnoreCase(recipientType)) {
            sql = "SELECT id FROM Users WHERE semester = ?";
        } else if ("USER".equalsIgnoreCase(recipientType)) {
            sql = "SELECT id FROM Users WHERE email = ?";
        } else {
            return ids;
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!"ALL".equalsIgnoreCase(recipientType)) {
                ps.setString(1, recipientValue);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            logger.warning("resolveUserIds failed: " + e.getMessage());
        }
        return ids;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Persist a new notification to the database.
     *
     * @return true on success
     */
    public boolean sendNotification(Notification n) {
        List<Integer> userIds = resolveUserIds(n.getRecipientType(), n.getRecipientValue());
        if (userIds.isEmpty()) {
            return false;
        }
        String sql = "INSERT INTO Notifications (userId, title, message, type, isRead, created_at) VALUES (?, ?, ?, ?, 0, GETDATE())";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                for (int userId : userIds) {
                    ps.setInt(1, userId);
                    ps.setString(2, n.getTitle());
                    ps.setString(3, n.getMessage());
                    ps.setString(4, n.getRecipientType());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.warning("Failed to send notification: " + e.getMessage());
            return false;
        }
    }

    /** Soft-delete: remove a notification by ID. */
    public boolean deleteNotification(int id) {
        String sql = "DELETE FROM Notifications WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.warning("Failed to delete notification: " + e.getMessage());
            return false;
        }
    }

    /** Mark a single notification as read. */
    public boolean markAsRead(int id) {
        String sql = "UPDATE Notifications SET isRead = 1 WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.warning("Failed to mark notification as read: " + e.getMessage());
            return false;
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** All notifications, newest first. */
    public List<Notification> getAllNotifications() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM Notifications ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.warning("Failed to fetch notifications: " + e.getMessage());
        }
        return list;
    }

    /** Count of unread notifications. */
    public int getUnreadCount() {
        String sql = "SELECT COUNT(*) FROM Notifications WHERE isRead = 0";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.warning("Failed to count unread notifications: " + e.getMessage());
        }
        return 0;
    }

    public List<Notification> getNotificationsByUserId(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM Notifications WHERE userId = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to fetch notifications by user: " + e.getMessage());
        }
        return list;
    }

    public int getUnreadCountByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM Notifications WHERE userId = ? AND isRead = 0";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.warning("Failed to count unread notifications by user: " + e.getMessage());
        }
        return 0;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("userId"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setRecipientType(rs.getString("type"));
        n.setRecipientValue(null);
        n.setPriority("NORMAL");
        n.setSentBy(0);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) n.setSentAt(ts.toLocalDateTime());
        n.setRead(rs.getBoolean("isRead"));
        return n;
    }
}
