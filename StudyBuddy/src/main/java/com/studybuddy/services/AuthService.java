package com.studybuddy.services;

import com.studybuddy.dao.TaskDAO;
import com.studybuddy.dao.UserDAO;
import com.studybuddy.models.User;
import com.studybuddy.utils.PasswordHasher;
import com.studybuddy.utils.SessionManager;

public class AuthService {

    private static final UserDAO userDAO = new UserDAO();
    private static final TaskDAO taskDAO = new TaskDAO();
    private static final SessionManager session = SessionManager.getInstance();

    /**
     * Register a new user with automatic Student role assignment.
     * 
     * All newly registered users are automatically assigned the "STUDENT" role.
     * This ensures consistent role assignment and prevents users from choosing
     * or modifying their role during registration.
     * 
     * Existing administrator accounts remain unchanged and retain their role.
     * 
     * @param name     User's full name
     * @param email    User's email address (must be unique)
     * @param password User's password (minimum 6 characters)
     * @return true if registration successful, false otherwise
     */
    public static boolean registerUser(String name, String email, String password) {

        if (name == null || name.trim().isEmpty()) return false;
        if (email == null || email.trim().isEmpty()) return false;
        if (password == null || password.length() < 6) return false;

        if (userDAO.getUserByEmail(email) != null) return false;

        User user = new User();
        user.setName(name);
        // Also populate the fullName profile column so that the Edit Profile
        // page shows the name the user entered at registration, not an empty field.
        user.setFullName(name);
        user.setEmail(email);
        // Automatically assign STUDENT role to all new registrations
        user.setRole("STUDENT");
        user.setPassword(PasswordHasher.hashPassword(password));
        // Requirement 1: New students receive 100 achievement_points
        user.setAchievementPoints(100);

        int userId = userDAO.createUser(user);
        if (userId == -1) return false;

        // Add default tasks for the new user
        taskDAO.addDefaultTasks(userId);

        return true;
    }

    public static User login(String email, String password) {

        if (email == null || email.isEmpty()) return null;
        if (password == null || password.isEmpty()) return null;

        User user = userDAO.getUserByEmail(email);

        if (user != null && PasswordHasher.verifyPassword(password, user.getPassword())) {
            session.login(user);
            return user;
        }

        return null;
    }

    public static void logout() {
        session.logout();
    }

    public static User getCurrentUser() {
        return session.getCurrentUser();
    }

    public static boolean isLoggedIn() {
        return session.isLoggedIn();
    }
}