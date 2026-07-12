package com.studybuddy.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Answer operations.
 * Handles database operations specific to the Answers table.
 */
public class AnswerDAO {

    /**
     * Checks if an answer has been rewarded (marked as accepted/correct).
     * 
     * @param answerId the ID of the answer to check
     * @return true if the answer is rewarded, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean isAnswerRewarded(int answerId) throws SQLException {
        String sql = "SELECT is_rewarded FROM Answers WHERE answer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, answerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_rewarded");
                }
                return false;
            }
        }
    }

    /**
     * Marks an answer as rewarded (is_rewarded = 1).
     * This is typically called when an answer is selected as the best answer.
     * 
     * @param answerId the ID of the answer to mark as rewarded
     * @return true if the update was successful, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean markAnswerAsRewarded(int answerId) throws SQLException {
        String sql = "UPDATE Answers SET is_rewarded = 1 WHERE answer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, answerId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Counts the number of rewarded answers for a specific user.
     * Used for achievement tracking.
     * 
     * @param userId the ID of the user
     * @return the count of rewarded answers
     * @throws SQLException if a database error occurs
     */
    public int countRewardedAnswersByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Answers WHERE user_id = ? AND is_rewarded = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }

    /**
     * Gets all rewarded answers for a specific user.
     * 
     * @param userId the ID of the user
     * @return list of answer IDs that have been rewarded
     * @throws SQLException if a database error occurs
     */
    public List<Integer> getRewardedAnswerIdsByUserId(int userId) throws SQLException {
        String sql = "SELECT answer_id FROM Answers WHERE user_id = ? AND is_rewarded = 1";
        List<Integer> answerIds = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    answerIds.add(rs.getInt("answer_id"));
                }
            }
        }

        return answerIds;
    }
}