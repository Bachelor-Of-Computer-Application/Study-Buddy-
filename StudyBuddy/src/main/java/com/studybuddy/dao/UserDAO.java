package com.studybuddy.dao;

import com.studybuddy.models.User;
import com.studybuddy.utils.PasswordHasher;
import java.sql.*;

/**
 * Data Access Object for User operations.
 * Extended with methods for Edit Profile functionality including
 * personal information, study preferences, email settings, and security.
 *
 * All SQL column names match the actual Users table schema exactly.
 */
public class UserDAO {

    // =========================
    // CREATE USER (REGISTER)
    // =========================

    /**
     * Create a new user in the database.
     * 
     * @param user User object to create
     * @return true if creation successful, false otherwise
     */
    public boolean createUser(User user) {
        String sql = "INSERT INTO Users (name, email, password, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole() == null ? "user" : user.getRole());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================
    // GET USER BY EMAIL (LOGIN)
    // =========================

    /**
     * Get fully-populated user by email address.
     *
     * FIXED: Previously only mapped 6 core fields (id, name, email, password, role,
     * created_at), leaving all profile columns null after login — causing the UI to
     * always show empty/stale profile data after restart.
     *
     * Now delegates to mapResultSetToUser() (the same helper used by getUserById())
     * so that the User object stored in App.currentUser after login is complete:
     * fullName, username, bio, profileImagePath, phoneNumber, department, semester,
     * preferredSubjects, studyGoals, learningInterests, notification settings,
     * points, achievements.
     *
     * SQL: SELECT * FROM Users WHERE email = ?
     *
     * @param email User's email address
     * @return Fully-populated User object or null if not found
     */
    public User getUserByEmail(String email) {
        String sql = "SELECT * FROM Users WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // FIXED: was mapping only 6 fields inline;
                    // now uses mapResultSetToUser() to populate ALL columns.
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================
    // GET USER BY ID
    // =========================

    /**
     * Get user by ID.
     * 
     * @param userId User's ID
     * @return User object or null if not found
     */
    public User getUserById(int userId) {
        String sql = "SELECT * FROM Users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================
    // UPDATE USER (BASIC)
    // =========================

    /**
     * Update basic user information.
     * 
     * @param user User object with updated information
     * @return true if update successful, false otherwise
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE Users SET name=?, email=?, password=?, role=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole());
            ps.setInt(5, user.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================
    // UPDATE PERSONAL INFORMATION
    // =========================

    /**
     * Update user's personal information.
     * FIXED: Parameter indices 1-8 now map correctly (index 4 was previously
     * missing,
     * causing profileImagePath to be skipped).
     * SQL columns: fullName, username, bio, profileImagePath, phoneNumber,
     * department, semester
     * 
     * @param user User object with updated personal details
     * @return true if update successful, false otherwise
     */
    public boolean updatePersonalInformation(User user) {
        String sql = "UPDATE Users SET " +
                "fullName=?, " + // param 1 → SQL column: fullName
                "username=?, " + // param 2 → SQL column: username
                "bio=?, " + // param 3 → SQL column: bio
                "profileImagePath=?, " + // param 4 → SQL column: profileImagePath (was missing!)
                "phoneNumber=?, " + // param 5 → SQL column: phoneNumber
                "department=?, " + // param 6 → SQL column: department
                "semester=? " + // param 7 → SQL column: semester
                "WHERE id=?"; // param 8 → Users.id

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName()); // fullName
            ps.setString(2, user.getUsername()); // username
            ps.setString(3, user.getBio()); // bio
            ps.setString(4, user.getProfileImagePath()); // profileImagePath (FIXED — was setString(5,...))
            ps.setString(5, user.getPhoneNumber()); // phoneNumber (FIXED — was setString(6,...))
            ps.setString(6, user.getDepartment()); // department (FIXED — was setString(7,...))
            ps.setString(7, user.getSemester()); // semester (FIXED — was setString(8,...))
            ps.setInt(8, user.getId()); // WHERE id=? (FIXED — was setInt(9,...))

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user's full name.
     * 
     * @param userId   User's ID
     * @param fullName New full name
     * @return true if update successful, false otherwise
     */
    public boolean updateFullName(int userId, String fullName) {
        String sql = "UPDATE Users SET fullName=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fullName);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user's username.
     * 
     * @param userId   User's ID
     * @param username New username
     * @return true if update successful, false otherwise
     */
    public boolean updateUsername(int userId, String username) {
        String sql = "UPDATE Users SET username=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user's bio.
     * 
     * @param userId User's ID
     * @param bio    New bio text
     * @return true if update successful, false otherwise
     */
    public boolean updateBio(int userId, String bio) {
        String sql = "UPDATE Users SET bio=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bio);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user's profile image path.
     * 
     * @param userId           User's ID
     * @param profileImagePath New profile image path
     * @return true if update successful, false otherwise
     */
    public boolean updateProfileImagePath(int userId, String profileImagePath) {
        String sql = "UPDATE Users SET profileImagePath=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, profileImagePath);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================
    // UPDATE STUDY PREFERENCES
    // =========================

    /**
     * Update user's study preferences.
     * SQL columns: preferredSubjects, studyGoals, learningInterests,
     * notificationsEnabled
     * 
     * @param user User object with updated study preferences
     * @return true if update successful, false otherwise
     */
    public boolean updateStudyPreferences(User user) {
        String sql = "UPDATE Users SET preferredSubjects=?, studyGoals=?, learningInterests=?, notificationsEnabled=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getPreferredSubjects());
            ps.setString(2, user.getStudyGoals());
            ps.setString(3, user.getLearningInterests());
            ps.setBoolean(4, user.isNotificationsEnabled());
            ps.setInt(5, user.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user's preferred subjects.
     * 
     * @param userId            User's ID
     * @param preferredSubjects New preferred subjects
     * @return true if update successful, false otherwise
     */
    public boolean updatePreferredSubjects(int userId, String preferredSubjects) {
        String sql = "UPDATE Users SET preferredSubjects=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, preferredSubjects);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user's study goals.
     * 
     * @param userId     User's ID
     * @param studyGoals New study goals
     * @return true if update successful, false otherwise
     */
    public boolean updateStudyGoals(int userId, String studyGoals) {
        String sql = "UPDATE Users SET studyGoals=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studyGoals);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user's learning interests.
     * 
     * @param userId            User's ID
     * @param learningInterests New learning interests
     * @return true if update successful, false otherwise
     */
    public boolean updateLearningInterests(int userId, String learningInterests) {
        String sql = "UPDATE Users SET learningInterests=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, learningInterests);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user's notifications enabled status.
     * 
     * @param userId               User's ID
     * @param notificationsEnabled Whether notifications are enabled
     * @return true if update successful, false otherwise
     */
    public boolean updateNotificationsEnabled(int userId, boolean notificationsEnabled) {
        String sql = "UPDATE Users SET notificationsEnabled=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, notificationsEnabled);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================
    // UPDATE EMAIL SETTINGS
    // =========================

    /**
     * Update user's email.
     * 
     * @param userId   User's ID
     * @param newEmail New email address
     * @return true if update successful, false otherwise
     */
    public boolean updateEmail(int userId, String newEmail) {
        String sql = "UPDATE Users SET email=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newEmail);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user's email notification settings.
     * SQL columns: email, emailNotificationsEnabled, resourceUpdateNotifications,
     * systemNotifications
     * 
     * @param user User object with updated email settings
     * @return true if update successful, false otherwise
     */
    public boolean updateEmailSettings(User user) {
        String sql = "UPDATE Users SET email=?, emailNotificationsEnabled=?, resourceUpdateNotifications=?, systemNotifications=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getEmail());
            ps.setBoolean(2, user.isEmailNotificationsEnabled());
            ps.setBoolean(3, user.isResourceUpdateNotifications());
            ps.setBoolean(4, user.isSystemNotifications());
            ps.setInt(5, user.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update email notifications enabled status.
     * 
     * @param userId                    User's ID
     * @param emailNotificationsEnabled Whether email notifications are enabled
     * @return true if update successful, false otherwise
     */
    public boolean updateEmailNotificationsEnabled(int userId, boolean emailNotificationsEnabled) {
        String sql = "UPDATE Users SET emailNotificationsEnabled=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, emailNotificationsEnabled);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update resource update notifications status.
     * 
     * @param userId                      User's ID
     * @param resourceUpdateNotifications Whether resource update notifications are
     *                                    enabled
     * @return true if update successful, false otherwise
     */
    public boolean updateResourceUpdateNotifications(int userId, boolean resourceUpdateNotifications) {
        String sql = "UPDATE Users SET resourceUpdateNotifications=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, resourceUpdateNotifications);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update system notifications status.
     * 
     * @param userId              User's ID
     * @param systemNotifications Whether system notifications are enabled
     * @return true if update successful, false otherwise
     */
    public boolean updateSystemNotifications(int userId, boolean systemNotifications) {
        String sql = "UPDATE Users SET systemNotifications=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, systemNotifications);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================
    // UPDATE PASSWORD
    // =========================

    /**
     * Update user's password.
     * 
     * @param userId      User's ID
     * @param newPassword New password (should be hashed before calling)
     * @return true if update successful, false otherwise
     */
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE Users SET password=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPassword);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verify user's current password.
     * 
     * @param userId          User's ID
     * @param currentPassword Current password to verify
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(int userId, String currentPassword) {
        String sql = "SELECT password FROM Users WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                return PasswordHasher.verifyPassword(currentPassword, storedHash);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================
    // GET USER STATISTICS
    // =========================

    /**
     * Get user statistics.
     * SQL columns: answersCount, questionsCount, achievements, points
     * 
     * @param userId User's ID
     * @return User object with statistics data
     */
    public User getUserStatistics(int userId) {
        String sql = "SELECT id, answersCount, questionsCount, achievements, points FROM Users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setAnswersCount(rs.getInt("answersCount"));
                user.setQuestionsCount(rs.getInt("questionsCount"));
                user.setAchievements(rs.getInt("achievements"));
                user.setPoints(rs.getInt("points"));
                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Increment user's points.
     * 
     * @param userId      User's ID
     * @param pointsToAdd Points to add
     * @return true if update successful, false otherwise
     */
    public boolean incrementPoints(int userId, int pointsToAdd) {
        String sql = "UPDATE Users SET points = points + ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, pointsToAdd);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================
    // DELETE USER
    // =========================

    /**
     * Delete user from database.
     * 
     * @param userId User's ID
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM Users WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================
    // HELPER METHOD
    // =========================

    /**
     * Map ResultSet to User object.
     * All column names match the actual Users table schema.
     * 
     * @param rs ResultSet containing user data
     * @return User object
     * @throws SQLException if mapping fails
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));

        // Profile information — all SQL column names are camelCase
        user.setFullName(rs.getString("fullName"));
        user.setUsername(rs.getString("username"));
        user.setBio(rs.getString("bio"));
        user.setProfileImagePath(rs.getString("profileImagePath"));
        user.setPhoneNumber(rs.getString("phoneNumber"));
        user.setDepartment(rs.getString("department"));
        user.setSemester(rs.getString("semester"));
        user.setSubject(rs.getString("subject"));

        // Study preferences
        user.setPreferredSubjects(rs.getString("preferredSubjects"));
        user.setStudyGoals(rs.getString("studyGoals"));
        user.setLearningInterests(rs.getString("learningInterests"));
        user.setNotificationsEnabled(rs.getBoolean("notificationsEnabled"));

        // Notification settings
        user.setEmailNotificationsEnabled(rs.getBoolean("emailNotificationsEnabled"));
        user.setResourceUpdateNotifications(rs.getBoolean("resourceUpdateNotifications"));
        user.setSystemNotifications(rs.getBoolean("systemNotifications"));

        // Statistics
        user.setAnswersCount(rs.getInt("answersCount"));
        user.setQuestionsCount(rs.getInt("questionsCount"));
        user.setAchievements(rs.getInt("achievements"));
        user.setPoints(rs.getInt("points"));

        // Creation timestamp
        if (rs.getTimestamp("created_at") != null) {
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }

        return user;
    }
}