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

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Persist a new notification to the database.
     *
     * @return true on success
     */
    public boolean sendNotification(Notification n) {
        String sql = """
                INSERT INTO Notifications
                    (title, message, recipient_type, recipient_value, priority, sent_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, n.getTitle());
            ps.setString(2, n.getMessage());
            ps.setString(3, n.getRecipientType());
            ps.setString(4, n.getRecipientValue());
            ps.setString(5, n.getPriority() != null ? n.getPriority() : "NORMAL");
            ps.setInt(6, n.getSentBy());
            return ps.executeUpdate() > 0;

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
        String sql = "UPDATE Notifications SET is_read = 1 WHERE id = ?";
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
        String sql = "SELECT * FROM Notifications ORDER BY sent_at DESC";
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
        String sql = "SELECT COUNT(*) FROM Notifications WHERE is_read = 0";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.warning("Failed to count unread notifications: " + e.getMessage());
        }
        return 0;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setRecipientType(rs.getString("recipient_type"));
        n.setRecipientValue(rs.getString("recipient_value"));
        n.setPriority(rs.getString("priority"));
        n.setSentBy(rs.getInt("sent_by"));
        Timestamp ts = rs.getTimestamp("sent_at");
        if (ts != null) n.setSentAt(ts.toLocalDateTime());
        n.setRead(rs.getBoolean("is_read"));
        return n;
    }
}
