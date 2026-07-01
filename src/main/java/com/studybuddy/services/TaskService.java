package com.studybuddy.services;

import com.studybuddy.dao.TaskDAO;
import com.studybuddy.models.Task;
import java.util.List;

/**
 * Service layer for handling Task related operations.
 * Delegates all calls to TaskDAO.
 *
 * Architecture: Controller → TaskService → TaskDAO → DatabaseConnection → SQL Server
 */
public class TaskService {
    private final TaskDAO taskDAO = new TaskDAO();

    // =========================
    // GET TASKS FOR USER
    // =========================

    /**
     * Returns all tasks for the given user.
     * SQL: SELECT * FROM Tasks WHERE user_id = ?
     */
    public List<Task> getTasksForUser(int userId) {
        return taskDAO.getTasksByUserId(userId);
    }

    // =========================
    // ADD TASK
    // =========================

    /**
     * Creates a new task in the database.
     * SQL: INSERT INTO Tasks (user_id, title, description, status)
     */
    public boolean addTask(Task task) {
        return taskDAO.createTask(task);
    }

    // =========================
    // UPDATE TASK
    // =========================

    /**
     * Updates an existing task's title, description, and status.
     */
    public boolean updateTask(Task task) {
        return taskDAO.updateTask(task);
    }

    // =========================
    // DELETE TASK
    // =========================

    /**
     * Deletes a task by its ID.
     */
    public boolean deleteTask(int taskId) {
        return taskDAO.deleteTask(taskId);
    }

    // =========================
    // STATISTICS
    // =========================

    /**
     * Returns total task count for a user.
     * SQL: SELECT COUNT(*) FROM Tasks WHERE user_id = ?
     */
    public int getTotalTaskCount(int userId) {
        return taskDAO.getTaskCount(userId);
    }

    /**
     * Returns completed task count for a user.
     * SQL: SELECT COUNT(*) FROM Tasks WHERE user_id = ? AND status = 'completed'
     */
    public int getCompletedTaskCount(int userId) {
        return taskDAO.getCompletedTaskCount(userId);
    }

    /**
     * Returns completion percentage for a user.
     */
    public int getCompletedPercentage(int userId) {
        return taskDAO.getCompletedPercentage(userId);
    }

    /**
     * Returns estimated study hours based on completed task count.
     */
    public double getStudyHours(int userId) {
        return taskDAO.getStudyHours(userId);
    }
}
