package com.studybuddy.dao;

import com.studybuddy.models.Achievement;

import java.sql.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Achievement operations.
 * Handles database operations specific to the UserAchievements table.
 */
public class AchievementDAO {

    /**
     * Gets all achievements for a specific user.
     * 
     * @param userId the ID of the user
     * @return list of achievements for the user
     * @throws SQLException if a database error occurs
     */
    public List<Achievement> getAchievementsByUserId(int userId) throws SQLException {
        String sql = "SELECT id, user_id, achievement_id, name, description, icon, reward_points, " +
                     "current_progress, target_progress, unlocked, unlocked_at, created_at, updated_at " +
                     "FROM UserAchievements WHERE user_id = ? ORDER BY unlocked ASC, created_at DESC";
        List<Achievement> achievements = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    achievements.add(mapAchievement(rs));
                }
            }
        }

        return achievements;
    }

    /**
     * Gets a specific achievement for a user by achievement ID.
     * 
     * @param userId the ID of the user
     * @param achievementId the unique identifier for the achievement type
     * @return the achievement if found, null otherwise
     * @throws SQLException if a database error occurs
     */
    public Achievement getUserAchievement(int userId, String achievementId) throws SQLException {
        String sql = "SELECT id, user_id, achievement_id, name, description, icon, reward_points, " +
                     "current_progress, target_progress, unlocked, unlocked_at, created_at, updated_at " +
                     "FROM UserAchievements WHERE user_id = ? AND achievement_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, achievementId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapAchievement(rs);
                }
            }
        }

        return null;
    }

    /**
     * Creates a new user achievement entry.
     * 
     * @param achievement the achievement to create
     * @return true if creation was successful, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean createAchievement(Achievement achievement) throws SQLException {
        String sql = "INSERT INTO UserAchievements (user_id, achievement_id, name, description, icon, " +
                     "reward_points, current_progress, target_progress, unlocked, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, achievement.getUserId());
            stmt.setString(2, achievement.getAchievementId());
            stmt.setString(3, achievement.getName());
            stmt.setString(4, achievement.getDescription());
            stmt.setString(5, achievement.getIcon());
            stmt.setInt(6, achievement.getRewardPoints());
            stmt.setInt(7, achievement.getCurrentProgress());
            stmt.setInt(8, achievement.getTargetProgress());
            stmt.setBoolean(9, achievement.isUnlocked());

            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        achievement.setId(keys.getInt(1));
                    }
                }
            }
            return success;
        }
    }

    /**
     * Updates an existing user achievement's progress and unlock status.
     * 
     * @param achievement the achievement to update
     * @return true if update was successful, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean updateAchievement(Achievement achievement) throws SQLException {
        String sql = "UPDATE UserAchievements SET current_progress = ?, unlocked = ?, " +
                     "unlocked_at = CASE WHEN ? = 1 AND unlocked = 0 THEN GETDATE() ELSE unlocked_at END, " +
                     "updated_at = GETDATE() WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, achievement.getCurrentProgress());
            stmt.setBoolean(2, achievement.isUnlocked());
            stmt.setBoolean(3, achievement.isUnlocked());
            stmt.setInt(4, achievement.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Increments the progress of a user achievement.
     * If the progress reaches or exceeds the target, the achievement is unlocked.
     * 
     * @param userId the ID of the user
     * @param achievementId the unique identifier for the achievement type
     * @param increment the amount to increment progress by
     * @return true if update was successful, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean incrementAchievementProgress(int userId, String achievementId, int increment) throws SQLException {
        String sql = "UPDATE UserAchievements " +
                     "SET current_progress = current_progress + ?, " +
                     "unlocked = CASE WHEN current_progress + ? >= target_progress THEN 1 ELSE unlocked END, " +
                     "unlocked_at = CASE WHEN current_progress + ? >= target_progress AND unlocked = 0 THEN GETDATE() ELSE unlocked_at END, " +
                     "updated_at = GETDATE() " +
                     "WHERE user_id = ? AND achievement_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, increment);
            stmt.setInt(2, increment);
            stmt.setInt(3, increment);
            stmt.setInt(4, userId);
            stmt.setString(5, achievementId);

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Counts the number of unlocked achievements for a user.
     * 
     * @param userId the ID of the user
     * @return the count of unlocked achievements
     * @throws SQLException if a database error occurs
     */
    public int countUnlockedAchievements(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM UserAchievements WHERE user_id = ? AND unlocked = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    /**
     * Gets the total reward points earned from unlocked achievements for a user.
     * 
     * @param userId the ID of the user
     * @return the total reward points from achievements
     * @throws SQLException if a database error occurs
     */
    public int getTotalAchievementRewardPoints(int userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(reward_points), 0) FROM UserAchievements WHERE user_id = ? AND unlocked = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    /**
     * Maps a ResultSet row to an Achievement object.
     */
    private Achievement mapAchievement(ResultSet rs) throws SQLException {
        Achievement achievement = new Achievement();
        achievement.setId(rs.getInt("id"));
        achievement.setUserId(rs.getInt("user_id"));
        achievement.setAchievementId(rs.getString("achievement_id"));
        achievement.setName(rs.getString("name"));
        achievement.setDescription(rs.getString("description"));
        achievement.setIcon(rs.getString("icon"));
        achievement.setRewardPoints(rs.getInt("reward_points"));
        achievement.setCurrentProgress(rs.getInt("current_progress"));
        achievement.setTargetProgress(rs.getInt("target_progress"));
        achievement.setUnlocked(rs.getBoolean("unlocked"));

        Timestamp unlockedAt = rs.getTimestamp("unlocked_at");
        if (unlockedAt != null) {
            achievement.setUnlockedAt(unlockedAt.toLocalDateTime());
        }

        return achievement;
    }
}
