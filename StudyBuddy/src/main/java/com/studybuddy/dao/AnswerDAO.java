package com.studybuddy.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}