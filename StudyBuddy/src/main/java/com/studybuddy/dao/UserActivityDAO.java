
package com.studybuddy.dao;

import com.studybuddy.models.UserActivity;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserActivityDAO {
    public boolean logActivity(UserActivity activity) throws SQLException {
        String sql = "INSERT INTO UserActivities (user_id, user_full_name, action, target_type, target_name, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, GETDATE())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, activity.getUserId());
            stmt.setString(2, activity.getUserFullName());
            stmt.setString(3, activity.getAction());
            stmt.setString(4, activity.getTargetType());
            stmt.setString(5, activity.getTargetName());
            return stmt.executeUpdate() > 0;
        }
    }

    public List<UserActivity> getRecentActivities(int limit) throws SQLException {
        String sql = "SELECT TOP (?) ua.*, u.fullName " +
                     "FROM UserActivities ua " +
                     "LEFT JOIN Users u ON ua.user_id = u.id " +
                     "ORDER BY ua.created_at DESC";
        List<UserActivity> activities = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    activities.add(mapActivity(rs));
                }
            }
        }
        return activities;
    }

    public List<UserActivity> getUserActivities(int userId, int limit) throws SQLException {
        String sql = "SELECT TOP (?) * FROM UserActivities WHERE user_id = ? ORDER BY created_at DESC";
        List<UserActivity> activities = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    activities.add(mapActivity(rs));
                }
            }
        }
        return activities;
    }

    private UserActivity mapActivity(ResultSet rs) throws SQLException {
        UserActivity activity = new UserActivity();
        activity.setId(rs.getInt("id"));
        activity.setUserId(rs.getInt("user_id"));
        activity.setUserFullName(rs.getString("user_full_name"));
        activity.setAction(rs.getString("action"));
        activity.setTargetType(rs.getString("target_type"));
        activity.setTargetName(rs.getString("target_name"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            activity.setCreatedAt(ts.toLocalDateTime());
        }
        return activity;
    }
}
