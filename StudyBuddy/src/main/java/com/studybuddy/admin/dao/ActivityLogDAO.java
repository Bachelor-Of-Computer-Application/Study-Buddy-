package com.studybuddy.admin.dao;

import com.studybuddy.models.ActivityLog;
import com.studybuddy.utils.DatabaseUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO for the ActivityLogs table.
 * Every admin action is persisted here via insertLog().
 */
public class ActivityLogDAO {

    private static final Logger logger = Logger.getLogger(ActivityLogDAO.class.getName());
    private static ActivityLogDAO instance;

    private ActivityLogDAO() {}

    public static synchronized ActivityLogDAO getInstance() {
        if (instance == null) instance = new ActivityLogDAO();
        return instance;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Persist a new activity log record.
     *
     * @return true on success
     */
    public boolean insertLog(ActivityLog log) {
        String sql = """
                INSERT INTO ActivityLogs
                    (admin_id, admin_name, action, target_type, target_name, status, remarks)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, log.getAdminId());
            ps.setString(2, log.getAdminName());
            ps.setString(3, log.getAction());
            ps.setString(4, log.getTargetType());
            ps.setString(5, log.getTargetName());
            ps.setString(6, log.getStatus() != null ? log.getStatus() : "SUCCESS");
            ps.setString(7, log.getRemarks());
            
            logger.fine("[ActivityLogDAO] Inserting log with admin_id=" + log.getAdminId() + 
                       ", admin_name=" + log.getAdminName());
            
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.severe("Failed to insert activity log: " + e.getMessage() + 
                         " | admin_id=" + log.getAdminId() + 
                         ", action=" + log.getAction());
            return false;
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** All logs, newest first. */
    public List<ActivityLog> getAllLogs() {
        return queryLogs(
                "SELECT * FROM ActivityLogs ORDER BY created_at DESC",
                ps -> {});
    }

    /**
     * Full-text search across admin_name, action, target_type, target_name, remarks.
     */
    public List<ActivityLog> searchLogs(String query) {
        String like = "%" + query.toLowerCase() + "%";
        String sql = """
                SELECT * FROM ActivityLogs
                WHERE LOWER(admin_name)  LIKE ?
                   OR LOWER(action)      LIKE ?
                   OR LOWER(target_type) LIKE ?
                   OR LOWER(target_name) LIKE ?
                   OR LOWER(remarks)     LIKE ?
                ORDER BY created_at DESC
                """;
        return queryLogs(sql, ps -> {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
        });
    }

    /** Filter logs within an inclusive date range. */
    public List<ActivityLog> filterByDate(LocalDate from, LocalDate to) {
        String sql = """
                SELECT * FROM ActivityLogs
                WHERE CAST(created_at AS DATE) BETWEEN ? AND ?
                ORDER BY created_at DESC
                """;
        return queryLogs(sql, ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
        });
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<ActivityLog> queryLogs(String sql, ParamSetter setter) {
        List<ActivityLog> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setter.set(ps);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActivityLog log = new ActivityLog();
                    log.setId(rs.getInt("id"));
                    log.setAdminId(rs.getInt("admin_id"));
                    log.setAdminName(rs.getString("admin_name"));
                    log.setAction(rs.getString("action"));
                    log.setTargetType(rs.getString("target_type"));
                    log.setTargetName(rs.getString("target_name"));
                    log.setStatus(rs.getString("status"));
                    log.setRemarks(rs.getString("remarks"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) log.setCreatedAt(ts.toLocalDateTime());
                    list.add(log);
                }
            }
        } catch (SQLException e) {
            logger.warning("ActivityLogDAO query failed: " + e.getMessage());
        }
        return list;
    }
}
