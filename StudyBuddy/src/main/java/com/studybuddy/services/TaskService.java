package com.studybuddy.services;

import com.studybuddy.dao.TaskDAO;
import com.studybuddy.models.Task;
import java.util.List;

/**
 * Service layer for Task-related operations.
 * Delegates all database calls to TaskDAO.
 *
 * Architecture: Controller → TaskService → TaskDAO → DatabaseConnection → SQL Server
 *
 * Tasks table SQL column names:
 *   userId      (NOT user_id)
 *   created_at
 *   status
 */
public class TaskService {
    private final TaskDAO taskDAO = new TaskDAO();

    // Achievement points for task completion (Requirement 4)
    private static final int POINTS_LOW = 3;
    private static final int POINTS_MEDIUM = 5;
    private static final int POINTS_HIGH = 10;

    // =========================
    // GET ALL TASKS FOR USER
    // =========================

    /**
     * Returns all tasks for the given user, newest first.
     * SQL: SELECT * FROM Tasks WHERE userId = ? ORDER BY created_at DESC
     */
    public List<Task> getTasksForUser(int userId) {
        return taskDAO.getTasksByUserId(userId);
    }

    // =========================
    // GET RECENT TASKS FOR USER
    // =========================

    /**
     * Returns the 10 most recent tasks for a user.
     *
     * SQL:
     *   SELECT TOP (10) id, userId, title, description, status, created_at
     *   FROM Tasks WHERE userId = ?
     *   ORDER BY created_at DESC
     */
    public List<Task> getRecentTasksForUser(int userId) {
        return taskDAO.getRecentTasksByUserId(userId);
    }

    // =========================
    // ADD TASK
    // =========================

    /**
     * Creates a new task in the database.
     * SQL: INSERT INTO Tasks (userId, title, description, status) VALUES (?, ?, ?, ?)
     */
    public boolean addTask(Task task) {
        boolean ok = taskDAO.createTask(task);
        if (ok) {
            com.studybuddy.admin.services.ActivityLogService.getInstance().logAction("Task Created", "Task", task.getTitle());
        }
        return ok;
    }

    // =========================
    // UPDATE TASK WITH POINTS (Requirement 4)
    // =========================

    /**
     * Updates an existing task and awards achievement points atomically when completing for the first time.
     */
    public boolean updateTask(Task task) {
        boolean isCompleting = "completed".equalsIgnoreCase(task.getStatus());
        boolean ok;
        if (isCompleting && !task.isRewarded()) {
            int points = getPointsForPriority(task.getPriority());
            ok = taskDAO.updateTaskWithReward(task, points);
            if (ok) {
                com.studybuddy.admin.services.ActivityLogService.getInstance().logAction("Task Completed", "Task", task.getTitle());
            }
        } else {
            ok = taskDAO.updateTask(task);
            if (ok) {
                com.studybuddy.admin.services.ActivityLogService.getInstance().logAction("Task Updated", "Task", task.getTitle());
            }
        }
        return ok;
    }

    /**
     * Gets the achievement points for a task based on its priority.
     * Requirement 4: Low=3, Medium=5, High=10
     *
     * @param priority The task priority (Low, Medium, High)
     * @return The points to award
     */
    private int getPointsForPriority(String priority) {
        if (priority == null) return POINTS_MEDIUM;

        return switch (priority.toLowerCase()) {
            case "high" -> POINTS_HIGH;
            case "low" -> POINTS_LOW;
            default -> POINTS_MEDIUM;
        };
    }

    // =========================
    // DELETE TASK
    // =========================

    /**
     * Deletes a task by its ID.
     */
    public boolean deleteTask(int taskId) {
        boolean ok = taskDAO.deleteTask(taskId);
        if (ok) {
            com.studybuddy.admin.services.ActivityLogService.getInstance().logAction("Task Deleted", "Task", "Task#" + taskId);
        }
        return ok;
    }

    // =========================
    // STATISTICS
    // =========================

    /**
     * Returns total task count for a user.
     * SQL: SELECT COUNT(*) FROM Tasks WHERE userId = ?
     */
    public int getTotalTaskCount(int userId) {
        return taskDAO.getTaskCount(userId);
    }

    /**
     * Returns the number of completed tasks for a user.
     * SQL: SELECT COUNT(*) FROM Tasks WHERE userId = ? AND status = 'completed'
     */
    public int getCompletedTaskCount(int userId) {
        return taskDAO.getCompletedTaskCount(userId);
    }

    /**
     * Returns the number of pending tasks for a user.
     */
    public int getPendingTaskCount(int userId) {
        return taskDAO.getPendingTaskCount(userId);
    }

    /**
     * Returns the number of in-progress tasks for a user.
     */
    public int getInProgressTaskCount(int userId) {
        return taskDAO.getInProgressTaskCount(userId);
    }

    /**
     * Returns the study progress percentage for a user using a single SQL aggregate query.
     *
     * SQL:
     *   SELECT
     *       COUNT(*) AS TotalTasks,
     *       SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) AS CompletedTasks
     *   FROM Tasks WHERE userId = ?
     *
     * Returns 0 when there are no tasks.
     */
    public int getStudyProgress(int userId) {
        return taskDAO.getStudyProgress(userId);
    }

    /**
     * Returns completion percentage for a user.
     * Delegates to the efficient single-query getStudyProgress().
     */
    public int getCompletedPercentage(int userId) {
        return taskDAO.getCompletedPercentage(userId);
    }

    /**
     * Returns estimated study hours based on completed task count.
     * Each completed task is estimated as 2 hours of study.
     */
    public double getStudyHours(int userId) {
        return taskDAO.getStudyHours(userId);
    }

    public boolean addDefaultTasks(int userId) {
        return taskDAO.addDefaultTasks(userId);
    }
}
