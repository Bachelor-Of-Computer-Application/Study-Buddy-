package com.studybuddy.admin.dao;

import com.studybuddy.models.AppSetting;
import com.studybuddy.utils.DatabaseUtil;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * DAO for the Settings table.
 * Provides simple key-value persistence for application-wide settings.
 */
public class SettingsDAO {

    private static final Logger logger = Logger.getLogger(SettingsDAO.class.getName());
    private static SettingsDAO instance;

    private SettingsDAO() {}

    public static synchronized SettingsDAO getInstance() {
        if (instance == null) instance = new SettingsDAO();
        return instance;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Get a single setting by key. Returns null if not found.
     */
    public String getSetting(String key) {
        String sql = "SELECT setting_value FROM Settings WHERE setting_key = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("setting_value");
            }
        } catch (SQLException e) {
            logger.warning("getSetting failed for key=" + key + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Load all settings as an ordered map of key → value.
     */
    public Map<String, String> getAllSettings() {
        Map<String, String> map = new LinkedHashMap<>();
        String sql = "SELECT setting_key, setting_value FROM Settings ORDER BY setting_key";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            logger.warning("getAllSettings failed: " + e.getMessage());
        }
        return map;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Upsert a single setting.
     *
     * @return true on success
     */
    public boolean updateSetting(String key, String value) {
        return updateSetting(key, value, null);
    }

    /**
     * Upsert a single setting with updated_by user ID.
     *
     * @return true on success
     */
    public boolean updateSetting(String key, String value, Integer updatedBy) {
        // MERGE (upsert) for SQL Server
        String sql = """
                MERGE INTO Settings AS target
                USING (SELECT ? AS setting_key) AS source
                ON (target.setting_key = source.setting_key)
                WHEN MATCHED THEN
                    UPDATE SET setting_value = ?, updated_at = SYSUTCDATETIME(), updated_by = ?
                WHEN NOT MATCHED THEN
                    INSERT (setting_key, setting_value, updated_by) VALUES (?, ?, ?);
                """;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            if (updatedBy != null) {
                ps.setInt(3, updatedBy);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, key);
            ps.setString(5, value);
            if (updatedBy != null) {
                ps.setInt(6, updatedBy);
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.warning("updateSetting failed for key=" + key + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Batch-save a map of settings in a single transaction.
     *
     * @return true if all updates succeeded
     */
    public boolean saveAllSettings(Map<String, String> settings) {
        return saveAllSettings(settings, null);
    }

    /**
     * Batch-save a map of settings in a single transaction with updated_by user ID.
     *
     * @return true if all updates succeeded
     */
    public boolean saveAllSettings(Map<String, String> settings, Integer updatedBy) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = """
                        MERGE INTO Settings AS target
                        USING (SELECT ? AS setting_key) AS source
                        ON (target.setting_key = source.setting_key)
                        WHEN MATCHED THEN
                            UPDATE SET setting_value = ?, updated_at = SYSUTCDATETIME(), updated_by = ?
                        WHEN NOT MATCHED THEN
                            INSERT (setting_key, setting_value, updated_by) VALUES (?, ?, ?);
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map.Entry<String, String> entry : settings.entrySet()) {
                        ps.setString(1, entry.getKey());
                        ps.setString(2, entry.getValue());
                        if (updatedBy != null) {
                            ps.setInt(3, updatedBy);
                        } else {
                            ps.setNull(3, Types.INTEGER);
                        }
                        ps.setString(4, entry.getKey());
                        ps.setString(5, entry.getValue());
                        if (updatedBy != null) {
                            ps.setInt(6, updatedBy);
                        } else {
                            ps.setNull(6, Types.INTEGER);
                        }
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                logger.warning("saveAllSettings batch failed: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.warning("saveAllSettings connection failed: " + e.getMessage());
            return false;
        }
    }
}
