package com.studybuddy.services;

import com.studybuddy.dao.TaskDAO;
import com.studybuddy.models.Task;
import java.util.List;

/**
 * Service layer for handling Task related operations.
 */
public class TaskService {
    private final TaskDAO taskDAO = new TaskDAO();

    // TODO: Add database operations and statistics logging

    public List<Task> getTasksForUser(int userId) {
        return taskDAO.getTasksByUserId(userId);
    }

    public int getTotalTaskCount(int userId) {
        return taskDAO.getTaskCount(userId);
    }

    public int getCompletedTaskCount(int userId) {
        return taskDAO.getCompletedTaskCount(userId);
    }

    public int getCompletedPercentage(int userId) {
        return taskDAO.getCompletedPercentage(userId);
    }

    public double getStudyHours(int userId) {
        return taskDAO.getStudyHours(userId);
    }

    public boolean addTask(Task task) {
        return taskDAO.createTask(task);
    }
}
