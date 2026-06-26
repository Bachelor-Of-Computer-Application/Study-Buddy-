package com.studybuddy.dao;

import com.studybuddy.models.Task;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    // =========================
    // CREATE TASK
    // =========================
    public boolean createTask(Task task) {

        String sql =
                "INSERT INTO Tasks(userId,title,description,status) VALUES(?,?,?,?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, task.getUserId());
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
    // READ TASKS
    // =========================
    public List<Task> getTasksByUserId(int userId) {

        List<Task> tasks = new ArrayList<>();

        String sql = "SELECT * FROM Tasks WHERE userId=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Task task = new Task();

                task.setId(rs.getInt("id"));
                task.setUserId(rs.getInt("userId"));
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
    public boolean updateTask(Task task) {

        String sql =
                "UPDATE Tasks SET title=?, description=?, status=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStatus());
            ps.setInt(4, task.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================
    // DELETE TASK
    // =========================
    public boolean deleteTask(int taskId) {

        String sql = "DELETE FROM Tasks WHERE id=?";

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
    public int getTaskCount(int userId) {

        String sql =
                "SELECT COUNT(*) FROM Tasks WHERE userId=?";

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
    public int getCompletedTaskCount(int userId) {

        String sql =
                "SELECT COUNT(*) FROM Tasks WHERE userId=? AND status='completed'";

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


    // COMPLETION %

    public int getCompletedPercentage(int userId) {

        int total = getTaskCount(userId);

        if (total == 0) {
            return 0;
        }

        int completed = getCompletedTaskCount(userId);

        return (completed * 100) / total;
    }


    // STUDY HOURS

    public double getStudyHours(int userId) {

        // Dummy value for presentation
        return getCompletedTaskCount(userId) * 2.0;
    }
}