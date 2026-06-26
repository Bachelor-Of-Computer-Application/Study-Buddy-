package com.studybuddy.services;

import com.studybuddy.utils.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionService {

    public boolean saveQuestion(String text, String subject, int points, String attachment) {

        String sql = """
INSERT INTO Questions
(user_id, author_name, subject, question_text,
 tags, attachment_path, reward_points)
VALUES (?, ?, ?, ?, ?, ?, ?)
""";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, getCurrentUserId());
            stmt.setString(2, subject);
            stmt.setString(3, text);
            stmt.setString(4, attachment);
            stmt.setInt(5, points);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            return false;
        }
    }

    public int getUserPoints() {

        String sql = "SELECT points FROM Users WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, getCurrentUserId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("points");
            }

        } catch (SQLException e) {
            return 115;
        }

        return 115;
    }

    public List<String> getAvailableSubjects() {
        return List.of(
                "Mathematics", "Science", "English", "Computer Science",
                "Database", "Networking", "Programming", "Physics",
                "Chemistry", "Other"
        );
    }

    private int getCurrentUserId() {
        return 1; // replace with SessionManager later
    }

    public boolean validateInputs(String q, String s, int p) {
        return q != null && !q.isEmpty()
                && s != null && !s.isEmpty()
                && p >= 5 && p <= 30;
    }
}