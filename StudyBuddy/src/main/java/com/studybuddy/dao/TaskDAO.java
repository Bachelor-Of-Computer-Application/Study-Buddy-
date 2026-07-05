package com.studybuddy.dao;

import com.studybuddy.models.Task;

import java.sql.*;
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

            ps.setInt(1,    task.getUserId());
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getStatus());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================
    // READ ALL TASKS BY USER
    // =========================

    /**
     * Returns all tasks for a given user, ordered by newest first.
     * SQL: SELECT * FROM Tasks WHERE userId = ? ORDER BY created_at DESC
     */
    public List<Task> getTasksByUserId(int userId) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT id, user_id AS userId, title, description, status, created_at " +
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
            e.printStackTrace();
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
     */
    public List<Task> getRecentTasksByUserId(int userId) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT TOP (10) id, user_id AS userId, title, description, status, created_at " +
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
            e.printStackTrace();
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
            e.printStackTrace();
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

            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStatus());
            ps.setInt(4,    task.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
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

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
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
            e.printStackTrace();
        }

        return 0;
    }

    // =========================
    // COMPLETED TASK COUNT
    // =========================

    /**
     * Returns the number of completed tasks for a user.
     * SQL: SELECT COUNT(*) FROM Tasks WHERE userId = ? AND status = 'completed'
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
            e.printStackTrace();
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
        task.setDescription(rs.getString("description"));
        task.setStatus(rs.getString("status"));
        task.setCreatedAt(rs.getTimestamp("created_at")); // SQL column: created_at
        return task;
    }
}