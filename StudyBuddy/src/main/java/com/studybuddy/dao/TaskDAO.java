package com.studybuddy.dao;

import com.studybuddy.models.Task;
import com.studybuddy.utils.EventBus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Task operations.
 *
 * IMPORTANT — SQL schema column name:
 *   Tasks table uses column name: userId  (NOT user_id)
 *   All SQL strings use "userId" to match the actual schema.
 *
 * Tasks table schema:
 *   id          INT IDENTITY(1,1) PRIMARY KEY
 *   userId      INT NOT NULL
 *   title       NVARCHAR(100)
 *   description NVARCHAR(MAX)
 *   status      NVARCHAR(20) DEFAULT 'pending'
 *   created_at  DATETIME DEFAULT GETDATE()
 */
public class TaskDAO {

    // =========================
    // CREATE TASK
    // =========================

    /**
     * Inserts a new task into the Tasks table.
     * SQL column name: userId (matches Tasks table schema)
     */
    public boolean createTask(Task task) {
        String sql = "INSERT INTO Tasks (user_id, title, description, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            task.setDescription(buildStoredDescription(task));
            ps.setInt(1,    task.getUserId());
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getStatus());

            boolean success = ps.executeUpdate() > 0;
            if (success) {
                EventBus.getInstance().publish(new EventBus.TasksChangedEvent());
                EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
            }
            return success;

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Adds default study tasks for a new user.
     */
    public boolean addDefaultTasks(int userId) {
        // Define default tasks
        record DefaultTask(String title, String description, String status) {}
        
        var defaultTasks = new DefaultTask[]{
            new DefaultTask("Complete Profile", "Fill in your full name, username, and add a profile picture", "pending"),
            new DefaultTask("Upload First Note", "Create and upload a study note for one of your subjects", "pending"),
            new DefaultTask("Upload 5 Notes", "Create and upload 5 study notes", "pending"),
            new DefaultTask("Upload 10 Notes", "Create and upload 10 study notes", "pending"),
            new DefaultTask("Upload First Resource", "Share a helpful resource with the community", "pending"),
            new DefaultTask("Upload 5 Resources", "Share 5 helpful resources with the community", "pending"),
            new DefaultTask("Ask First Question", "Post a question to the community about a topic you're struggling with", "pending"),
            new DefaultTask("Ask 5 Questions", "Post 5 questions to the community", "pending"),
            new DefaultTask("Answer First Question", "Help another student by answering one of their questions", "pending"),
            new DefaultTask("Answer 5 Questions", "Help other students by answering 5 of their questions", "pending"),
            new DefaultTask("Complete First Task", "Finish your first task", "pending"),
            new DefaultTask("Complete 5 Tasks", "Finish 5 of your tasks", "pending"),
            new DefaultTask("Complete 10 Tasks", "Finish 10 of your tasks", "pending"),
            new DefaultTask("Complete 20 Tasks", "Finish 20 of your tasks", "pending"),
            new DefaultTask("Study for Midterm", "Prepare study materials for your midterm exams", "pending"),
            new DefaultTask("Prepare Final Notes", "Create comprehensive final exam study notes", "pending"),
            new DefaultTask("Download 5 Resources", "Download 5 helpful resources from the library", "pending"),
            new DefaultTask("Complete Weekly Goal", "Set and complete your weekly study goal", "pending"),
            new DefaultTask("Organize Study Materials", "Create a system for organizing your study materials", "pending"),
            new DefaultTask("Review Semester Progress", "Review your overall progress for the semester", "pending")
        };
        
        boolean allCreated = true;
        
        // Add each default task
        for (var dt : defaultTasks) {
            Task task = new Task();
            task.setUserId(userId);
            task.setTitle(dt.title());
            task.setDescription(dt.description());
            task.setStatus(dt.status());
            
            if (!createTask(task)) {
                allCreated = false;
            }
        }
        
        return allCreated;
    }

    // =========================
    // READ ALL TASKS BY USER
    // =========================

    /**
     * Returns all tasks for a given user, ordered by newest first.
     * SQL: SELECT * FROM Tasks WHERE userId = ? ORDER BY created_at DESC
     * Includes is_rewarded for requirement 4.4
     */
    public List<Task> getTasksByUserId(int userId) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT id, user_id AS userId, title, description, status, created_at, COALESCE(is_rewarded, 0) AS is_rewarded " +
                     "FROM Tasks WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapTask(rs));
                }
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return tasks;
    }

    // =========================
    // READ RECENT TASKS BY USER
    // =========================

    /**
     * Returns the most recent tasks (up to 10) for a given user.
     *
     * SQL:
     *   SELECT TOP (10) id, userId, title, description, status, created_at
     *   FROM Tasks
     *   WHERE userId = ?
     *   ORDER BY created_at DESC
     * Includes is_rewarded for requirement 4.4
     */
    public List<Task> getRecentTasksByUserId(int userId) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT TOP (10) id, user_id AS userId, title, description, status, created_at, COALESCE(is_rewarded, 0) AS is_rewarded " +
                     "FROM Tasks WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapTask(rs));
                }
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return tasks;
    }

    // =========================
    // STUDY PROGRESS (single-query aggregate)
    // =========================

    /**
     * Returns study progress percentage for a user in a single SQL query.
     *
     * SQL:
     *   SELECT
     *       COUNT(*) AS TotalTasks,
     *       SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) AS CompletedTasks
     *   FROM Tasks
     *   WHERE userId = ?
     *
     * Returns 0 when TotalTasks is 0 (no division by zero).
     */
    public int getStudyProgress(int userId) {
        String sql = "SELECT " +
                     "    COUNT(*) AS TotalTasks, " +
                     "    SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) AS CompletedTasks " +
                     "FROM Tasks " +
                     "WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total     = rs.getInt("TotalTasks");
                    int completed = rs.getInt("CompletedTasks");

                    if (total == 0) {
                        return 0;
                    }

                    return (completed * 100) / total;
                }
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return 0;
    }

    // =========================
    // UPDATE TASK
    // =========================

    /**
     * Updates an existing task's title, description, and status.
     */
    public boolean updateTask(Task task) {
        String sql = "UPDATE Tasks SET title = ?, description = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            task.setDescription(buildStoredDescription(task));
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStatus());
            ps.setInt(4,    task.getId());

            boolean success = ps.executeUpdate() > 0;
            if (success) {
                EventBus.getInstance().publish(new EventBus.TasksChangedEvent());
                EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
            }
            return success;

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return false;
        }
    }

    // =========================
    // DELETE TASK
    // =========================

    /**
     * Deletes a task by its ID.
     */
    public boolean deleteTask(int taskId) {
        String sql = "DELETE FROM Tasks WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);

            boolean success = ps.executeUpdate() > 0;
            if (success) {
                EventBus.getInstance().publish(new EventBus.TasksChangedEvent());
                EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
            }
            return success;

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return false;
        }
    }

    // =========================
    // TOTAL TASK COUNT
    // =========================

    /**
     * Returns total task count for a user.
     * SQL: SELECT COUNT(*) FROM Tasks WHERE userId = ?
     */
    public int getTaskCount(int userId) {
        String sql = "SELECT COUNT(*) FROM Tasks WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return 0;
    }

    // =========================
    // COMPLETED TASK COUNT
    // =========================

    /**
     * Returns the number of completed tasks for a user.
     * SQL: SELECT COUNT(*) FROM Tasks WHERE user_id = ? AND status = 'completed'
     */
    public int getCompletedTaskCount(int userId) {
        String sql = "SELECT COUNT(*) FROM Tasks WHERE user_id = ? AND status = 'completed'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return 0;
    }
    
    /**
     * Returns the number of pending tasks for a user.
     * SQL: SELECT COUNT(*) FROM Tasks WHERE user_id = ? AND status = 'pending'
     */
    public int getPendingTaskCount(int userId) {
        String sql = "SELECT COUNT(*) FROM Tasks WHERE user_id = ? AND status = 'pending'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return 0;
    }
    
    /**
     * Returns the number of in-progress tasks for a user.
     * SQL: SELECT COUNT(*) FROM Tasks WHERE user_id = ? AND status = 'in-progress' OR status = 'in_progress'
     */
    public int getInProgressTaskCount(int userId) {
        String sql = "SELECT COUNT(*) FROM Tasks WHERE user_id = ? AND (status = 'in-progress' OR status = 'in_progress')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return 0;
    }

    // =========================
    // COMPLETION PERCENTAGE
    // =========================

    /**
     * Calculates the completion percentage of tasks for a user.
     * Delegates to getStudyProgress() for a single-query implementation.
     */
    public int getCompletedPercentage(int userId) {
        return getStudyProgress(userId);
    }

    // =========================
    // IS TASK REWARDED (Requirement 4.4)
    // =========================

    /**
     * Checks if a task has already been rewarded with achievement points.
     * SQL: SELECT is_rewarded FROM Tasks WHERE id = ?
     *
     * @param taskId the ID of the task to check
     * @return true if the task has been rewarded, false otherwise
     */
    public boolean isTaskRewarded(int taskId) {
        String sql = "SELECT is_rewarded FROM Tasks WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int rewarded = rs.getInt("is_rewarded");
                    return rewarded == 1;
                }
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return false;
    }

    /**
     * Marks a task as rewarded (is_rewarded = 1) without awarding points.
     * This is typically called when points are awarded separately.
     * 
     * @param taskId the ID of the task to mark as rewarded
     * @return true if the update was successful, false otherwise
     */
    public boolean markTaskAsRewarded(int taskId) {
        String sql = "UPDATE Tasks SET is_rewarded = 1 WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Counts the number of rewarded tasks for a specific user.
     * Used for achievement tracking.
     * 
     * @param userId the ID of the user
     * @return the count of rewarded tasks
     */
    public int countRewardedTasksByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM Tasks WHERE user_id = ? AND is_rewarded = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return 0;
    }

    // =========================
    // UPDATE TASK WITH REWARD (atomic transaction)
    // =========================

    /**
     * Updates a task and awards achievement points in a single JDBC transaction.
     * Prevents duplicate rewards via Tasks.is_rewarded.
     */
    public boolean updateTaskWithReward(Task task, int points) {
        if (points <= 0) {
            return updateTask(task);
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                String checkSql = "SELECT is_rewarded FROM Tasks WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, task.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getInt("is_rewarded") == 1) {
                            conn.rollback();
                            return updateTask(task);
                        }
                    }
                }

                task.setDescription(buildStoredDescription(task));
                String updateSql = "UPDATE Tasks SET title = ?, description = ?, status = ?, is_rewarded = 1 WHERE id = ? AND is_rewarded = 0";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, task.getTitle());
                    ps.setString(2, task.getDescription());
                    ps.setString(3, task.getStatus());
                    ps.setInt(4, task.getId());
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                int newBalance = 0;
                String addSql = "UPDATE Users SET achievement_points = achievement_points + ? OUTPUT INSERTED.achievement_points WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(addSql)) {
                    ps.setInt(1, points);
                    ps.setInt(2, task.getUserId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        newBalance = rs.getInt("achievement_points");
                    }
                }

                conn.commit();
                task.setRewarded(true);
                EventBus.getInstance().publish(new EventBus.TasksChangedEvent());
                EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
                EventBus.getInstance().publish(new EventBus.PointsChangedEvent(task.getUserId(), newBalance));
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(TaskDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return false;
        }
    }

    // =========================
    // STUDY HOURS (DERIVED)
    // =========================

    /**
     * Returns an estimated study hours value based on completed tasks.
     * Each completed task is counted as 2 hours of study time.
     */
    public double getStudyHours(int userId) {
        return getCompletedTaskCount(userId) * 2.0;
    }

    // =========================
    // PRIVATE HELPER — MAP ResultSet TO Task
    // =========================

    /**
     * Maps a single ResultSet row to a Task object.
     * All column names match the exact SQL schema.
     */
    private Task mapTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getInt("id"));
        task.setUserId(rs.getInt("userId"));            // SQL column: userId
        task.setTitle(rs.getString("title"));
        task.setStatus(rs.getString("status"));
        task.setCreatedAt(rs.getTimestamp("created_at")); // SQL column: created_at

        // Set is_rewarded from query result (Requirement 4.4)
        try {
            int isRewarded = rs.getInt("is_rewarded");
            if (!rs.wasNull()) {
                task.setRewarded(isRewarded == 1);
            }
        } catch (SQLException ignored) {
            // Column may not exist in older schemas
        }

        String storedDescription = rs.getString("description");
        TaskMetadata metadata = parseMetadata(storedDescription);
        task.setDescription(metadata.description);
        task.setPriority(metadata.priority);
        task.setSubject(metadata.subject);
        task.setDueDate(metadata.dueDate);
        task.setEstimatedTime(metadata.estimatedTime);
        return task;
    }

    private String buildStoredDescription(Task task) {
        String description = task.getDescription() == null ? "" : task.getDescription().trim();
        StringBuilder builder = new StringBuilder(description);
        if (task.getPriority() != null || task.getDueDate() != null
                || task.getEstimatedTime() != null || task.getSubject() != null) {
            if (!description.isEmpty()) {
                builder.append("\n");
            }
            builder.append("[studybuddy-meta]");
            if (task.getPriority() != null && !task.getPriority().isBlank()) {
                builder.append("priority=").append(task.getPriority()).append("|");
            }
            if (task.getSubject() != null && !task.getSubject().isBlank()) {
                builder.append("subject=").append(task.getSubject()).append("|");
            }
            if (task.getDueDate() != null) {
                builder.append("due=").append(task.getDueDate().toLocalDateTime().toLocalDate()).append("|");
            }
            if (task.getEstimatedTime() != null && !task.getEstimatedTime().isBlank()) {
                builder.append("estimate=").append(task.getEstimatedTime());
            }
        }
        return builder.toString();
    }

    private TaskMetadata parseMetadata(String storedDescription) {
        TaskMetadata metadata = new TaskMetadata();
        if (storedDescription == null || storedDescription.isBlank()) {
            return metadata;
        }

        String[] parts = storedDescription.split("\\n", 2);
        metadata.description = parts[0].trim();

        if (storedDescription.contains("[studybuddy-meta]")) {
            String metaBlock = storedDescription.substring(storedDescription.indexOf("[studybuddy-meta]") + "[studybuddy-meta]".length());
            for (String entry : metaBlock.split("\\|")) {
                if (entry.contains("priority=")) {
                    metadata.priority = entry.replace("priority=", "").trim();
                } else if (entry.contains("subject=")) {
                    metadata.subject = entry.replace("subject=", "").trim();
                } else if (entry.contains("due=")) {
                    String dueValue = entry.replace("due=", "").trim();
                    try {
                        metadata.dueDate = Timestamp.valueOf(java.time.LocalDate.parse(dueValue).atStartOfDay());
                    } catch (Exception ignored) { /* intentionally ignored: optional data or best-effort cleanup */ }
                } else if (entry.contains("estimate=")) {
                    metadata.estimatedTime = entry.replace("estimate=", "").trim();
                }
            }
        }

        return metadata;
    }

    private static class TaskMetadata {
        private String description = "";
        private String priority = "Medium";
        private String subject = "";
        private Timestamp dueDate;
        private String estimatedTime = "";
    }
}