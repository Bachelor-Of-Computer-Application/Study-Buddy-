package com.studybuddy.services;

import com.studybuddy.dao.UserDAO;
import com.studybuddy.models.User;
import com.studybuddy.utils.PasswordHasher;
import com.studybuddy.utils.SessionManager;

public class AuthService {

    private static final UserDAO userDAO = new UserDAO();
    private static final SessionManager session = SessionManager.getInstance();

    public static boolean registerUser(String name, String email, String password) {

        if (name == null || name.trim().isEmpty()) return false;
        if (email == null || email.trim().isEmpty()) return false;
        if (password == null || password.length() < 6) return false;

        if (userDAO.getUserByEmail(email) != null) return false;

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRole("user");
        user.setPassword(PasswordHasher.hashPassword(password));

        return userDAO.createUser(user);
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