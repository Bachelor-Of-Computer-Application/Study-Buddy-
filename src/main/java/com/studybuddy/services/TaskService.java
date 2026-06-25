package com.studybuddy.services;

import com.studybuddy.dao.TaskDAO;
import com.studybuddy.models.Task;

import java.sql.SQLException;
import java.util.List;

public class TaskService {
    private final TaskDAO taskDAO = new TaskDAO();

    public List<Task> getTasksForUser(int userId) throws SQLException {
        return taskDAO.getTasksByUserId(userId);
    }

    public int getTotalTaskCount(int userId) throws SQLException {
        return taskDAO.getTaskCount(userId);
    }

    public int getCompletedTaskCount(int userId) throws SQLException {
        return taskDAO.getCompletedTaskCount(userId);
    }

    public int getCompletedPercentage(int userId) throws SQLException {
        return taskDAO.getCompletedPercentage(userId);
    }

    public double getStudyHours(int userId) throws SQLException {
        return taskDAO.getStudyHours(userId);
    }

    public boolean addTask(Task task) throws SQLException {
        return taskDAO.createTask(task);
    }
}
