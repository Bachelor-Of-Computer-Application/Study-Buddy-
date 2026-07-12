package com.studybuddy.services;

import com.studybuddy.dao.UserDAO;
import com.studybuddy.models.User;
import com.studybuddy.utils.PasswordHasher;

/**
 * Service layer for handling User profile operations.
 * Extended with methods for Edit Profile functionality including
 * personal information, study preferences, email settings, and security.
 */
public class UserService {

    private final UserDAO userDAO = new UserDAO();

    // =========================
    // GET PROFILE
    // =========================

    /**
     * Get user profile by email.
     * @param email User's email address
     * @return User object with profile data
     */
    public User getUserProfile(String email) {
        return userDAO.getUserByEmail(email);
    }

    /**
     * Get user profile by user ID.
     * @param userId User's ID
     * @return User object with profile data
     */
    public User getUserProfileById(int userId) {
        return userDAO.getUserById(userId);
    }

    // =========================
    // UPDATE PROFILE - PERSONAL INFORMATION
    // =========================

    /**
     * Update user personal information.
     * @param user User object with updated personal details
     * @return true if update successful, false otherwise
     */
    public boolean updatePersonalInformation(User user) {
        return userDAO.updatePersonalInformation(user);
    }

    /**
     * Update user's full name.
     * @param userId User's ID
    /**
     * Update the core login `name` column so it stays in sync with fullName.
     * @param userId User's ID
     * @param name   New display name
     * @return true if update successful, false otherwise
     */
    public boolean updateName(int userId, String name) {
        return userDAO.updateName(userId, name);
    }

    /**
     * @param fullName New full name
     * @return true if update successful, false otherwise
     */
    public boolean updateFullName(int userId, String fullName) {
        return userDAO.updateFullName(userId, fullName);
    }

    /**
     * Update user's username.
     * @param userId User's ID
     * @param username New username
     * @return true if update successful, false otherwise
     */
    public boolean updateUsername(int userId, String username) {
        return userDAO.updateUsername(userId, username);
    }

    /**
     * Update user's bio/about section.
     * @param userId User's ID
     * @param bio New bio text
     * @return true if update successful, false otherwise
     */
    public boolean updateBio(int userId, String bio) {
        return userDAO.updateBio(userId, bio);
    }


    /**
     * Update user's profile image path.
     * @param userId User's ID
     * @param profileImagePath New profile image path
     * @return true if update successful, false otherwise
     */
    public boolean updateProfileImagePath(int userId, String profileImagePath) {
        return userDAO.updateProfileImagePath(userId, profileImagePath);
    }

    /**
     * Clears the user's profile image path in the database.
     */
    public boolean clearProfileImage(int userId) {
        return userDAO.updateProfileImagePath(userId, null);
    }

    // =========================
    // UPDATE PROFILE - STUDY PREFERENCES
    // =========================

    /**
     * Update user's study preferences.
     * @param user User object with updated study preferences
     * @return true if update successful, false otherwise
     */
    public boolean updateStudyPreferences(User user) {
        return userDAO.updateStudyPreferences(user);
    }

    /**
     * Update user's preferred subjects.
     * @param userId User's ID
     * @param preferredSubjects New preferred subjects
     * @return true if update successful, false otherwise
     */
    public boolean updatePreferredSubjects(int userId, String preferredSubjects) {
        return userDAO.updatePreferredSubjects(userId, preferredSubjects);
    }

    /**
     * Update user's study goals.
     * @param userId User's ID
     * @param studyGoals New study goals
     * @return true if update successful, false otherwise
     */
    public boolean updateStudyGoals(int userId, String studyGoals) {
        return userDAO.updateStudyGoals(userId, studyGoals);
    }

    /**
     * Update user's learning interests.
     * @param userId User's ID
     * @param learningInterests New learning interests
     * @return true if update successful, false otherwise
     */
    public boolean updateLearningInterests(int userId, String learningInterests) {
        return userDAO.updateLearningInterests(userId, learningInterests);
    }

    /**
     * Update user's notification preferences.
     * @param userId User's ID
     * @param notificationsEnabled Whether notifications are enabled
     * @return true if update successful, false otherwise
     */
    public boolean updateNotificationsEnabled(int userId, boolean notificationsEnabled) {
        return userDAO.updateNotificationsEnabled(userId, notificationsEnabled);
    }

    // =========================
    // UPDATE PROFILE - EMAIL SETTINGS
    // =========================

    /**
     * Update user's email.
     * @param userId User's ID
     * @param newEmail New email address
     * @return true if update successful, false otherwise
     */
    public boolean updateEmail(int userId, String newEmail) {
        return userDAO.updateEmail(userId, newEmail);
    }

    /**
     * Update user's email notification settings.
     * @param user User object with updated email settings
     * @return true if update successful, false otherwise
     */
    public boolean updateEmailSettings(User user) {
        return userDAO.updateEmailSettings(user);
    }

    /**
     * Update email notifications enabled status.
     * @param userId User's ID
     * @param emailNotificationsEnabled Whether email notifications are enabled
     * @return true if update successful, false otherwise
     */
    public boolean updateEmailNotificationsEnabled(int userId, boolean emailNotificationsEnabled) {
        return userDAO.updateEmailNotificationsEnabled(userId, emailNotificationsEnabled);
    }

    /**
     * Update resource update notifications status.
     * @param userId User's ID
     * @param resourceUpdateNotifications Whether resource update notifications are enabled
     * @return true if update successful, false otherwise
     */
    public boolean updateResourceUpdateNotifications(int userId, boolean resourceUpdateNotifications) {
        return userDAO.updateResourceUpdateNotifications(userId, resourceUpdateNotifications);
    }

    /**
     * Update system notifications status.
     * @param userId User's ID
     * @param systemNotifications Whether system notifications are enabled
     * @return true if update successful, false otherwise
     */
    public boolean updateSystemNotifications(int userId, boolean systemNotifications) {
        return userDAO.updateSystemNotifications(userId, systemNotifications);
    }

    // =========================
    // UPDATE PROFILE - SECURITY & PASSWORD
    // =========================

    /**
     * Update user's password.
     * @param userId User's ID
     * @param newPassword New password (should be encrypted before passing)
     * @return true if update successful, false otherwise
     */
    public boolean updatePassword(int userId, String newPassword) {

        String hashedPassword = PasswordHasher.hashPassword(newPassword);

        return userDAO.updatePassword(userId, hashedPassword);
    }
    /**
     * Verify user's current password.
     * @param userId User's ID
     * @param currentPassword Current password to verify
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(int userId, String currentPassword) {
        return userDAO.verifyPassword(userId, currentPassword);
    }

    // =========================
    // USER STATISTICS
    // =========================

    /**
     * Get user statistics.
     * @param userId User's ID
     * @return User object with statistics data
     */
    public User getUserStatistics(int userId) {
        return userDAO.getUserStatistics(userId);
    }

    /**
     * Get user's answers count.
     * @param userId User's ID
     * @return Answers count
     */
    public int getAnswersCount(int userId) {
        User user = userDAO.getUserStatistics(userId);
        return user != null ? user.getAnswersCount() : 0;
    }

    /**
     * Get user's questions count.
     * @param userId User's ID
     * @return Questions count
     */
    public int getQuestionsCount(int userId) {
        User user = userDAO.getUserStatistics(userId);
        return user != null ? user.getQuestionsCount() : 0;
    }

    /**
     * Get user's achievements count.
     * @param userId User's ID
     * @return Achievements count
     */
    public int getAchievements(int userId) {
        User user = userDAO.getUserStatistics(userId);
        return user != null ? user.getAchievements() : 0;
    }

    /**
     * Get user's points.
     * @param userId User's ID
     * @return Points value
     */
    public int getPoints(int userId) {
        User user = userDAO.getUserStatistics(userId);
        return user != null ? user.getPoints() : 0;
    }

    /**
     * Increment user's points.
     * @param userId User's ID
     * @param pointsToAdd Points to add
     * @return true if update successful, false otherwise
     */
    public boolean incrementPoints(int userId, int pointsToAdd) {
        return userDAO.incrementPoints(userId, pointsToAdd);
    }

    // =========================
    // DELETE PROFILE
    // =========================

    /**
     * Delete user profile.
     * @param userId User's ID
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteProfile(int userId) {
        return userDAO.deleteUser(userId);
    }

    // =========================
    // BATCH OPERATIONS
    // =========================

    /**
     * Update all profile information at once.
     * @param user User object with all updated information
     * @return true if update successful, false otherwise
     */
    public boolean updateAllProfile(User user) {
        boolean personalUpdated = updatePersonalInformation(user);
        boolean preferencesUpdated = updateStudyPreferences(user);
        boolean emailUpdated = updateEmailSettings(user);

        return personalUpdated && preferencesUpdated && emailUpdated;
    }

    /**
     * Reset user profile to original data from database.
     * @param userId User's ID
     * @return User object with original data
     */
    public User resetProfile(int userId) {
        return userDAO.getUserById(userId);
    }
}