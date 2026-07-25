package com.studybuddy.dao;

import com.studybuddy.models.User;
import com.studybuddy.utils.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

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
     * All new users are automatically assigned the "STUDENT" role if no role is specified.
     * This ensures consistent role assignment across the application.
     * 
     * The role is stored in the Users table's 'role' column during INSERT.
     * 
     * @param user User object to create (must have name, email, password, and role set)
     * @return generated userId if successful, -1 otherwise
     */
    public int createUser(User user) {
        // fullName is included so the Edit Profile page shows the name the user
        // entered at registration rather than an empty field.
        // Requirement 1: New users receive 100 achievement_points
        String sql = "INSERT INTO Users (name, fullName, email, password, role, achievement_points) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getName());
            // fullName mirrors name at registration; the user can change it later via Edit Profile.
            ps.setString(2, user.getFullName() != null ? user.getFullName() : user.getName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            // Default to STUDENT role if not specified
            ps.setString(5, user.getRole() == null || user.getRole().trim().isEmpty() ? "STUDENT" : user.getRole());
            // Set achievement_points (default 100 for new users)
            ps.setInt(6, user.getAchievementPoints() > 0 ? user.getAchievementPoints() : 100);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return -1;

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return -1;
        }
    }

    // =========================
    // GET USER BY USERNAME OR EMAIL (ADMIN LOGIN)
    // =========================

    /**
     * Look up a user by username OR email address.
     * Used by the admin login flow so admins can sign in with either credential.
     *
     * SQL: SELECT * FROM Users WHERE username = ? OR email = ?
     *
     * @param usernameOrEmail The value entered in the login field
     * @return Fully-populated User object, or null if not found
     */
    public User getUserByUsernameOrEmail(String usernameOrEmail) {
        String sql = "SELECT * FROM Users WHERE username = ? OR email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return null;
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update the core login `name` column.
     * Called alongside updateFullName() so both columns stay in sync when the
     * user edits their full name on the Edit Profile page.
     *
     * @param userId User's ID
     * @param name   New display name
     * @return true if update successful, false otherwise
     */
    public boolean updateName(int userId, String name) {
        String sql = "UPDATE Users SET name=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return false;
    }

    // =========================
    // ACHIEVEMENT POINTS
    // =========================

    /**
     * Get user's achievement points balance.
     * Requirement 6.1
     *
     * @param userId User's ID
     * @return Integer value of achievement_points, or null if not found
     */
    public Integer getAchievementPoints(int userId) {
        String sql = "SELECT achievement_points FROM Users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("achievement_points");
                }
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Add points to user's achievement points balance.
     * Requirement 6.2, 6.5
     *
     * @param userId User's ID
     * @param points Points to add (must be positive)
     * @return true if successful, false otherwise
     */
    public boolean addAchievementPoints(int userId, int points) {
        // Validate that points is positive
        if (points <= 0) {
            return false;
        }

        String sql = "UPDATE Users SET achievement_points = achievement_points + ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, points);
                    ps.setInt(2, userId);
                    boolean success = ps.executeUpdate() > 0;
                    conn.commit();
                    return success;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return false;
        }
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Deduct achievement points from user's balance.
     * Validates that the user has sufficient balance before deducting.
     * 
     * @param userId User's ID
     * @param points Points to deduct (must be positive)
     * @return true if successful, false if insufficient balance, negative amount, or error
     */
    public boolean deductAchievementPoints(int userId, int points) {
        // Validate: points must be positive
        if (points <= 0) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // First, check if user has sufficient balance within the transaction
                Integer currentBalance = getAchievementPoints(userId);
                if (currentBalance == null || currentBalance < points) {
                    conn.rollback();
                    return false;
                }

                // Deduct the points
                String sql = "UPDATE Users SET achievement_points = achievement_points - ? WHERE id = ? AND achievement_points >= ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, points);
                    ps.setInt(2, userId);
                    ps.setInt(3, points);
                    boolean success = ps.executeUpdate() > 0;
                    conn.commit();
                    return success;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(UserDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
        user.setName(getOptionalString(rs, "name"));
        user.setEmail(getOptionalString(rs, "email"));
        user.setPassword(getOptionalString(rs, "password"));
        user.setRole(getOptionalString(rs, "role"));

        // Profile information — optionally present depending on the database schema.
        user.setFullName(getOptionalString(rs, "fullName"));
        user.setUsername(getOptionalString(rs, "username"));
        user.setBio(getOptionalString(rs, "bio"));
        user.setProfileImagePath(getOptionalString(rs, "profileImagePath"));
        user.setPhoneNumber(getOptionalString(rs, "phoneNumber"));
        user.setDepartment(getOptionalString(rs, "department"));
        user.setSemester(getOptionalString(rs, "semester"));
        user.setSubject(getOptionalString(rs, "subject"));

        // Study preferences
        user.setPreferredSubjects(getOptionalString(rs, "preferredSubjects"));
        user.setStudyGoals(getOptionalString(rs, "studyGoals"));
        user.setLearningInterests(getOptionalString(rs, "learningInterests"));
        user.setNotificationsEnabled(getOptionalBoolean(rs, "notificationsEnabled"));

        // Notification settings
        user.setEmailNotificationsEnabled(getOptionalBoolean(rs, "emailNotificationsEnabled"));
        user.setResourceUpdateNotifications(getOptionalBoolean(rs, "resourceUpdateNotifications"));
        user.setSystemNotifications(getOptionalBoolean(rs, "systemNotifications"));

        // Statistics
        user.setAnswersCount(getOptionalInt(rs, "answersCount", 0));
        user.setQuestionsCount(getOptionalInt(rs, "questionsCount", 0));
        user.setAchievements(getOptionalInt(rs, "achievements", 0));
        user.setPoints(getOptionalInt(rs, "points", 0));
        user.setAchievementPoints(getOptionalInt(rs, "achievement_points", 0));

        // Creation timestamp
        Timestamp createdAt = getOptionalTimestamp(rs, "created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        return user;
    }

    private String getOptionalString(ResultSet rs, String columnName) throws SQLException {
        return hasColumn(rs, columnName) ? rs.getString(columnName) : null;
    }

    private int getOptionalInt(ResultSet rs, String columnName, int defaultValue) throws SQLException {
        return hasColumn(rs, columnName) ? rs.getInt(columnName) : defaultValue;
    }

    private boolean getOptionalBoolean(ResultSet rs, String columnName) throws SQLException {
        return hasColumn(rs, columnName) ? rs.getBoolean(columnName) : false;
    }

    private Timestamp getOptionalTimestamp(ResultSet rs, String columnName) throws SQLException {
        return hasColumn(rs, columnName) ? rs.getTimestamp(columnName) : null;
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}