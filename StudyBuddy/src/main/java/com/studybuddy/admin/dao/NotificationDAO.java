package com.studybuddy.admin.dao;

import com.studybuddy.models.Notification;
import com.studybuddy.utils.DatabaseUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * DAO for Notifications with smart recipient targeting.
 */
public class NotificationDAO {

    private static final Logger LOGGER = Logger.getLogger(NotificationDAO.class.getName());
    private static NotificationDAO instance;

    private NotificationDAO() {}

    public static synchronized NotificationDAO getInstance() {
        if (instance == null) instance = new NotificationDAO();
        return instance;
    }

    private List<Integer> resolveUserIds(Notification n) {
        Set<Integer> ids = new HashSet<>();
        String scope = n.getRecipientType() != null ? n.getRecipientType().toUpperCase() : "COLLEGE";

        try (Connection conn = DatabaseUtil.getConnection()) {
            switch (scope) {
                case "ALL":
                case "COLLEGE":
                    addIds(ids, conn, "SELECT id FROM Users WHERE status = 'Active' OR status IS NULL");
                    break;
                case "ALL_STUDENTS":
                    addIds(ids, conn, "SELECT id FROM Users WHERE role NOT IN ('admin','ADMIN') AND (status = 'Active' OR status IS NULL)");
                    break;
                case "ALL_FACULTY":
                    addIds(ids, conn, "SELECT id FROM Users WHERE role IN ('faculty','FACULTY','teacher')");
                    break;
                case "DEPARTMENT":
                    resolveDepartmentUsers(conn, ids, n);
                    break;
                case "SEMESTER":
                    resolveSemesterUsers(conn, ids, n);
                    break;
                case "USER":
                    if (n.getRecipientValue() != null && !n.getRecipientValue().isBlank()) {
                        addIds(ids, conn, "SELECT id FROM Users WHERE email = ?", n.getRecipientValue().trim());
                    }
                    break;
                case "DEPT_SEM":
                    resolveDeptSemUsers(conn, ids, n.getDepartmentId(), n.getSemesterId());
                    break;
                default:
                    addIds(ids, conn, "SELECT id FROM Users WHERE status = 'Active' OR status IS NULL");
            }
        } catch (SQLException e) {
            LOGGER.warning("resolveUserIds failed: " + e.getMessage());
        }
        return new ArrayList<>(ids);
    }

    private void resolveDepartmentUsers(Connection conn, Set<Integer> ids, Notification n) throws SQLException {
        if (n.getDepartmentId() != null) {
            addIds(ids, conn,
                    "SELECT u.id FROM Users u INNER JOIN Departments d ON u.department = d.name OR u.department = d.code WHERE d.id = ?",
                    n.getDepartmentId());
        } else if (n.getRecipientValue() != null) {
            for (String dept : n.getRecipientValue().split(",")) {
                String trimmed = dept.trim();
                if (!trimmed.isEmpty()) {
                    addIds(ids, conn,
                            "SELECT id FROM Users WHERE department = ? OR department LIKE ?",
                            trimmed, "%" + trimmed + "%");
                }
            }
        }
    }

    private void resolveSemesterUsers(Connection conn, Set<Integer> ids, Notification n) throws SQLException {
        if (n.getSemesterId() != null) {
            addIds(ids, conn,
                    "SELECT u.id FROM Users u INNER JOIN Semesters s ON u.semester = CAST(s.semesterNumber AS NVARCHAR(20)) OR u.semester = s.name WHERE s.id = ?",
                    n.getSemesterId());
        } else if (n.getRecipientValue() != null) {
            for (String sem : n.getRecipientValue().split(",")) {
                String trimmed = sem.trim();
                if (!trimmed.isEmpty()) {
                    addIds(ids, conn, "SELECT id FROM Users WHERE semester = ?", trimmed);
                }
            }
        }
    }

    private void resolveDeptSemUsers(Connection conn, Set<Integer> ids, Integer deptId, Integer semId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT u.id FROM Users u WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (deptId != null) {
            sql.append("AND (u.department IN (SELECT name FROM Departments WHERE id = ?) OR u.department IN (SELECT code FROM Departments WHERE id = ?)) ");
            params.add(deptId);
            params.add(deptId);
        }
        if (semId != null) {
            sql.append("AND (u.semester IN (SELECT CAST(semesterNumber AS NVARCHAR(20)) FROM Semesters WHERE id = ?) OR u.semester IN (SELECT name FROM Semesters WHERE id = ?)) ");
            params.add(semId);
            params.add(semId);
        }
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
    }

    private void addIds(Set<Integer> ids, Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
    }

    public boolean sendNotification(Notification n) throws SQLException {
        List<Integer> userIds = resolveUserIds(n);
        if (userIds.isEmpty()) return false;

        String sql = "INSERT INTO Notifications (userId, title, message, type, notificationType, priority, " +
                "expiryDate, attachmentPath, isRead, isArchived, sentBy, recipientType, recipientValue, " +
                "departmentId, semesterId, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?, ?, ?, GETDATE())";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                for (int userId : userIds) {
                    int recipientId = userId;
                    int senderId = n.getSentBy();
                    String title = n.getTitle();
                    String message = n.getMessage();


                    if (recipientId <= 0) {
                        String errMsg = "Invalid Notification: recipientId (" + recipientId + ") must be > 0";
                        LOGGER.warning(errMsg);
                        throw new SQLException(errMsg);
                    }
                    if (senderId <= 0) {
                        String errMsg = "Invalid Notification: senderId (" + senderId + ") must be > 0";
                        LOGGER.warning(errMsg);
                        throw new SQLException(errMsg);
                    }
                    if (title == null) {
                        String errMsg = "Invalid Notification: title is null";
                        LOGGER.warning(errMsg);
                        throw new SQLException(errMsg);
                    }
                    if (message == null) {
                        String errMsg = "Invalid Notification: message is null";
                        LOGGER.warning(errMsg);
                        throw new SQLException(errMsg);
                    }

                    ps.setInt(1, userId);
                    ps.setString(2, n.getTitle());
                    ps.setString(3, n.getMessage());
                    ps.setString(4, n.getRecipientType());
                    ps.setString(5, n.getNotificationType());
                    ps.setString(6, n.getPriority());
                    if (n.getExpiryDate() != null) ps.setTimestamp(7, Timestamp.valueOf(n.getExpiryDate()));
                    else ps.setNull(7, Types.TIMESTAMP);
                    ps.setString(8, n.getAttachmentPath());
                    ps.setInt(9, n.getSentBy());
                    ps.setString(10, n.getRecipientType());
                    ps.setString(11, n.getRecipientValue());
                    if (n.getDepartmentId() != null) ps.setInt(12, n.getDepartmentId());
                    else ps.setNull(12, Types.INTEGER);
                    if (n.getSemesterId() != null) ps.setInt(13, n.getSemesterId());
                    else ps.setNull(13, Types.INTEGER);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();

                LOGGER.info("Notification delivered successfully.");

                // Requirement 7: Publish event after successful insert
                com.studybuddy.utils.EventBus.getInstance().publish(new com.studybuddy.utils.EventBus.NotificationsChangedEvent());

                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean deleteNotification(int id) {
        String sql = "DELETE FROM Notifications WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.warning("Failed to delete notification: " + e.getMessage());
            return false;
        }
    }

    public boolean markAsRead(int id) {
        String sql = "UPDATE Notifications SET isRead = 1 WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.warning("Failed to mark notification as read: " + e.getMessage());
            return false;
        }
    }

    public boolean markAllReadForUser(int userId) {
        String sql = "UPDATE Notifications SET isRead = 1 WHERE userId = ? AND isRead = 0";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            LOGGER.warning("Failed to mark all read: " + e.getMessage());
            return false;
        }
    }

    public boolean archiveNotification(int id) {
        String sql = "UPDATE Notifications SET isArchived = 1 WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Notification> getAllNotifications() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM Notifications ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.warning("Failed to fetch notifications: " + e.getMessage());
        }
        return list;
    }

    public int getUnreadCount() {
        String sql = "SELECT COUNT(*) FROM Notifications WHERE isRead = 0 AND (isArchived = 0 OR isArchived IS NULL)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.warning("Failed to count unread: " + e.getMessage());
        }
        return 0;
    }

    public List<Notification> getNotificationsByUserId(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM Notifications WHERE userId = ? AND (isArchived = 0 OR isArchived IS NULL) " +
                "AND (expiryDate IS NULL OR expiryDate > GETDATE()) ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.warning("Failed to fetch user notifications: " + e.getMessage());
        }
        return list;
    }

    public int getUnreadCountByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM Notifications WHERE userId = ? AND isRead = 0 " +
                "AND (isArchived = 0 OR isArchived IS NULL) AND (expiryDate IS NULL OR expiryDate > GETDATE())";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.warning("Failed to count user unread: " + e.getMessage());
        }
        return 0;
    }

    public List<Notification> searchNotificationsForUser(int userId, String query, String type, String priority) {
        List<Notification> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM Notifications WHERE userId = ? AND (isArchived = 0 OR isArchived IS NULL) ");
        if (query != null && !query.isBlank()) {
            sql.append("AND (title LIKE ? OR message LIKE ?) ");
        }
        if (type != null && !type.isBlank()) {
            sql.append("AND notificationType = ? ");
        }
        if (priority != null && !priority.isBlank()) {
            sql.append("AND priority = ? ");
        }
        sql.append("ORDER BY created_at DESC");

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, userId);
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            if (type != null && !type.isBlank()) ps.setString(idx++, type);
            if (priority != null && !priority.isBlank()) ps.setString(idx, priority);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.warning("searchNotificationsForUser failed: " + e.getMessage());
        }
        return list;
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("userId"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        try { n.setRecipientType(rs.getString("recipientType")); }
        catch (SQLException e) { n.setRecipientType(rs.getString("type")); }
        try { n.setRecipientValue(rs.getString("recipientValue")); } catch (SQLException ignored) { /* intentionally ignored: optional data or best-effort cleanup */ }
        try { n.setPriority(rs.getString("priority")); } catch (SQLException e) { n.setPriority("Normal"); }
        try { n.setNotificationType(rs.getString("notificationType")); } catch (SQLException ignored) { /* intentionally ignored: optional data or best-effort cleanup */ }
        try {
            Timestamp exp = rs.getTimestamp("expiryDate");
            if (exp != null) n.setExpiryDate(exp.toLocalDateTime());
        } catch (SQLException ignored) { /* intentionally ignored: optional data or best-effort cleanup */ }
        try { n.setAttachmentPath(rs.getString("attachmentPath")); } catch (SQLException ignored) { /* intentionally ignored: optional data or best-effort cleanup */ }
        try { n.setSentBy(rs.getInt("sentBy")); } catch (SQLException ignored) { /* intentionally ignored: optional data or best-effort cleanup */ }
        try {
            int deptId = rs.getInt("departmentId");
            if (!rs.wasNull()) n.setDepartmentId(deptId);
        } catch (SQLException ignored) { /* intentionally ignored: optional data or best-effort cleanup */ }
        try {
            int semId = rs.getInt("semesterId");
            if (!rs.wasNull()) n.setSemesterId(semId);
        } catch (SQLException ignored) { /* intentionally ignored: optional data or best-effort cleanup */ }
        try { n.setArchived(rs.getBoolean("isArchived")); } catch (SQLException ignored) { /* intentionally ignored: optional data or best-effort cleanup */ }
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) n.setSentAt(ts.toLocalDateTime());
        n.setRead(rs.getBoolean("isRead"));
        return n;
    }
}
