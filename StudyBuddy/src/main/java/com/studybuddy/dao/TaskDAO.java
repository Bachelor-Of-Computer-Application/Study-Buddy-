package com.studybuddy.dao;

import com.studybuddy.models.Task;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Task operations.
 *
 * Key SQL schema fix:
 *   Tasks table uses column name: user_id  (NOT userId)
 *   All SQL strings have been updated to use user_id.
 */
public class TaskDAO {

    // =========================
    // CREATE TASK
    // =========================

    /**
     * Inserts a new task into the Tasks table.
     * FIXED: Column name is user_id (not userId) to match SQL schema.
     */
    public boolean createTask(Task task) {
        // SQL column name: user_id — matches Tasks table definition
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
    // READ TASKS BY USER
    // =========================

    /**
     * Returns all tasks for a given user.
     * FIXED: WHERE clause uses user_id (not userId) to match SQL schema.
     */
    public List<Task> getTasksByUserId(int userId) {
        List<Task> tasks = new ArrayList<>();

        // SQL column name: user_id — matches Tasks table definition
        String sql = "SELECT * FROM Tasks WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Task task = new Task();
                task.setId(rs.getInt("id"));
                task.setUserId(rs.getInt("user_id")); // SQL column: user_id
                task.setTitle(rs.getString("title"));
                task.setDescription(rs.getString("description"));
                task.setStatus(rs.getString("status"));
                tasks.add(task);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tasks;
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
     * FIXED: WHERE clause uses user_id (not userId).
     */
    public int getTaskCount(int userId) {
        // SQL column name: user_id — matches Tasks table definition
        String sql = "SELECT COUNT(*) FROM Tasks WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
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
     * FIXED: WHERE clause uses user_id (not userId).
     */
    public int getCompletedTaskCount(int userId) {
        // SQL column name: user_id — matches Tasks table definition
        String sql = "SELECT COUNT(*) FROM Tasks WHERE user_id = ? AND status = 'completed'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
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
     */
    public int getCompletedPercentage(int userId) {
        int total = getTaskCount(userId);

        if (total == 0) {
            return 0;
        }

        int completed = getCompletedTaskCount(userId);

        return (completed * 100) / total;
    }

    // =========================
    // STUDY HOURS (DERIVED)
    // =========================

    /**
     * Returns an estimated study hours value based on completed tasks.
     * Each completed task counts as 2 hours of study.
     */
    public double getStudyHours(int userId) {
        return getCompletedTaskCount(userId) * 2.0;
    }
}