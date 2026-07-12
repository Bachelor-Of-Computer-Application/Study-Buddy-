package com.studybuddy.admin.services;

import com.studybuddy.admin.dao.ActivityLogDAO;
import com.studybuddy.models.ActivityLog;
import com.studybuddy.utils.SessionManager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for all activity log operations.
 * Provides the logAction() method used by every controller to record admin actions.
 */
public class ActivityLogService {

    private static final Logger logger = Logger.getLogger(ActivityLogService.class.getName());
    private static ActivityLogService instance;
    private final ActivityLogDAO activityLogDAO = ActivityLogDAO.getInstance();

    private ActivityLogService() {}

    public static synchronized ActivityLogService getInstance() {
        if (instance == null) instance = new ActivityLogService();
        return instance;
    }

    // ── Logging ───────────────────────────────────────────────────────────────

    /**
     * Convenience method: derive admin identity from SessionManager automatically.
     *
     * @param action     short description, e.g. "User Suspended"
     * @param targetType category, e.g. "User", "Note", "Resource"
     * @param targetName specific item name, e.g. "john@example.com"
     */
    public void logAction(String action, String targetType, String targetName) {
        int adminId = 0;
        String adminName = "Admin";
        
        if (SessionManager.getCurrentAdmin() != null) {
            adminId   = SessionManager.getCurrentAdmin().getId();
            adminName = SessionManager.getCurrentAdmin().getName();
            
            // Debug: Log admin ID before insertion
            logger.info("[ActivityLog] Admin ID: " + adminId + ", Admin Name: " + adminName);
        } else {
            logger.warning("[ActivityLog] SessionManager.getCurrentAdmin() is NULL! Cannot log activity.");
            return; // Don't insert if no admin is logged in
        }
        
        logAction(adminId, adminName, action, targetType, targetName);
    }

    /**
     * Full-parameter logging method.
     */
    public void logAction(int adminId, String adminName, String action,
                          String targetType, String targetName) {
        // Verify adminId is valid before attempting insert
        if (adminId <= 0) {
            logger.warning("[ActivityLog] Invalid admin_id (" + adminId + "). Skipping activity log.");
            return;
        }
        
        ActivityLog log = new ActivityLog(adminId, adminName, action, targetType, targetName, "SUCCESS", null);
        boolean success = activityLogDAO.insertLog(log);
        
        if (!success) {
            logger.severe("[ActivityLog] Failed to insert log. AdminId=" + adminId + 
                         ", Action=" + action + ", Target=" + targetName);
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<ActivityLog> getLogs() {
        return activityLogDAO.getAllLogs();
    }

    public List<ActivityLog> searchLogs(String query) {
        return activityLogDAO.searchLogs(query);
    }

    public List<ActivityLog> filterByDate(LocalDate from, LocalDate to) {
        return activityLogDAO.filterByDate(from, to);
    }

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Export all activity logs to a CSV file at the given path.
     *
     * @return true on success
     */
    public boolean exportLogsToCSV(String filePath) {
        List<ActivityLog> logs = activityLogDAO.getAllLogs();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println("ID,Admin,Action,Target Type,Target Name,Status,Remarks,Timestamp");
            for (ActivityLog log : logs) {
                pw.printf("\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        log.getId(),
                        esc(log.getAdminName()),
                        esc(log.getAction()),
                        esc(log.getTargetType()),
                        esc(log.getTargetName()),
                        esc(log.getStatus()),
                        esc(log.getRemarks()),
                        log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : "");
            }
            return true;
        } catch (IOException e) {
            logger.warning("exportLogsToCSV failed: " + e.getMessage());
            return false;
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }
}
