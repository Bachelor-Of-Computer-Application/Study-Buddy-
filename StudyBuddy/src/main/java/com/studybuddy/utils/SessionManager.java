package com.studybuddy.utils;

import com.studybuddy.models.User;

/**
 * SessionManager – manages authenticated user sessions.
 *
 * Supports TWO usage patterns:
 *
 * 1. Instance-based (used by AuthService / student-facing code):
 *    SessionManager session = SessionManager.getInstance();
 *    session.login(user);  session.logout();  session.getCurrentUser();  session.isLoggedIn();
 *
 * 2. Static admin API (used by the admin module):
 *    SessionManager.setCurrentAdmin(user);
 *    SessionManager.getCurrentAdmin();
 *    SessionManager.clearSession();
 */
public class SessionManager {

    // ── Singleton instance (for AuthService compatibility) ─────────────────────
    private static final SessionManager INSTANCE = new SessionManager();

    private SessionManager() {}

    /** Returns the singleton instance used by AuthService. */
    public static SessionManager getInstance() {
        return INSTANCE;
    }

    // ── Instance-based session (student/user app) ──────────────────────────────
    private User currentUser;

    /** Store the currently logged-in (student) user. */
    public void login(User user) {
        this.currentUser = user;
    }

    /** Clear the student session. */
    public void logout() {
        this.currentUser = null;
    }

    /** Returns the currently logged-in student user, or null. */
    public User getCurrentUser() {
        return currentUser;
    }

    /** Returns true if a student user is logged in. */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // ── Static admin API (admin module) ───────────────────────────────────────
    private static User currentAdmin;

    /** Store the admin user after successful admin login. */
    public static void setCurrentAdmin(User user) {
        currentAdmin = user;
    }

    /** Retrieve the currently logged-in admin. May be null if not authenticated. */
    public static User getCurrentAdmin() {
        return currentAdmin;
    }

    /** Returns true if an admin is currently logged in. */
    public static boolean isAdminLoggedIn() {
        return currentAdmin != null;
    }

    /** Clear the admin session on logout. */
    public static void clearSession() {
        currentAdmin = null;
    }
}
