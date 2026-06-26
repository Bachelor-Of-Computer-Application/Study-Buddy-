package com.studybuddy.admin.services;

import com.studybuddy.admin.dao.AdminDAO;
import com.studybuddy.dao.UserDAO;
import com.studybuddy.models.*;
import com.studybuddy.utils.PasswordHasher;
import com.studybuddy.utils.SessionManager;

import java.util.List;
import java.util.Map;

/**
 * Central admin service orchestrating all admin module operations.
 * Every mutating method also triggers an activity log entry.
 */
public class AdminService {

    private static AdminService instance;
    private final AdminDAO adminDAO = AdminDAO.getInstance();
    private final ActivityLogService logService = ActivityLogService.getInstance();

    private AdminService() {}

    public static synchronized AdminService getInstance() {
        if (instance == null) instance = new AdminService();
        return instance;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Authentication
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Validate admin credentials. On success, stores the User in SessionManager.
     *
     * @return true if login is valid
     */
    public boolean validateAdminLogin(String username, String password) {
        User user = new UserDAO().getUserByEmail(username);
        if (user != null && ("admin".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole()))) {
            boolean valid = PasswordHasher.verifyPassword(password, user.getPassword())
                         || password.equals(user.getPassword()); // plain-text fallback for dev
            if (valid) {
                SessionManager.setCurrentAdmin(user);
                logService.logAction("Admin Login", "Session", username);
                return true;
            }
        }
        // Hardcoded dev fallback
        if ("admin".equalsIgnoreCase(username) && "admin123".equals(password)) {
            User devAdmin = new User();
            devAdmin.setId(0); devAdmin.setName("Administrator"); devAdmin.setEmail(username);
            devAdmin.setRole("ADMIN"); devAdmin.setStatus("Active");
            SessionManager.setCurrentAdmin(devAdmin);
            return true;
        }
        return false;
    }

    /** Clear session on logout. */
    public void logout() {
        logService.logAction("Admin Logout", "Session",
                SessionManager.getCurrentAdmin() != null ? SessionManager.getCurrentAdmin().getEmail() : "?");
        SessionManager.clearSession();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Dashboard
    // ══════════════════════════════════════════════════════════════════════════

    public Map<String, Integer> getDashboardStats() {
        return adminDAO.getDashboardStats();
    }

    public List<User> getRecentUsers(int limit) {
        return adminDAO.getRecentUsers(limit);
    }

    public List<Note> getRecentUploads(int limit) {
        return adminDAO.getRecentUploads(limit);
    }

    public List<Question> getRecentQuestions(int limit) {
        return adminDAO.getRecentQuestions(limit);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // User Moderation
    // ══════════════════════════════════════════════════════════════════════════

    public List<User> getUsers() { return adminDAO.getAllUsers(); }

    public List<User> searchUsers(String query) { return adminDAO.searchUsers(query); }

    public boolean suspendUser(int userId, String userName) {
        boolean ok = adminDAO.updateUserStatus(userId, "Suspended");
        if (ok) logService.logAction("User Suspended", "User", userName);
        return ok;
    }

    public boolean activateUser(int userId, String userName) {
        boolean ok = adminDAO.updateUserStatus(userId, "Active");
        if (ok) logService.logAction("User Activated", "User", userName);
        return ok;
    }

    public boolean banUser(int userId, String userName) {
        boolean ok = adminDAO.updateUserStatus(userId, "Banned");
        if (ok) logService.logAction("User Banned", "User", userName);
        return ok;
    }

    public boolean editUserInfo(int userId, String name, String email, String role,
                                 String department, String semester) {
        boolean ok = adminDAO.updateUserInfo(userId, name, email, role, department, semester);
        if (ok) logService.logAction("User Info Edited", "User", email);
        return ok;
    }

    public boolean resetPassword(int userId, String email, String newPassword) {
        String hashed = PasswordHasher.hashPassword(newPassword);
        boolean ok = adminDAO.resetUserPassword(userId, hashed);
        if (ok) logService.logAction("Password Reset", "User", email);
        return ok;
    }

    public boolean promoteToAdmin(int userId, String userName) {
        boolean ok = adminDAO.promoteToAdmin(userId);
        if (ok) logService.logAction("User Promoted to Admin", "User", userName);
        return ok;
    }

    public boolean demoteToUser(int userId, String userName) {
        boolean ok = adminDAO.demoteToUser(userId);
        if (ok) logService.logAction("User Demoted to Student", "User", userName);
        return ok;
    }

    public boolean softDeleteUser(int userId, String userName) {
        boolean ok = adminDAO.softDeleteUser(userId);
        if (ok) logService.logAction("User Soft Deleted", "User", userName);
        return ok;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Notes Moderation
    // ══════════════════════════════════════════════════════════════════════════

    public List<Note> getNotes() { return adminDAO.getAllNotesAdmin(); }

    public boolean approveNote(int noteId, String title) {
        boolean ok = adminDAO.updateNoteStatus(noteId, "Approved");
        if (ok) logService.logAction("Note Approved", "Note", title);
        return ok;
    }

    public boolean rejectNote(int noteId, String title) {
        boolean ok = adminDAO.updateNoteStatus(noteId, "Rejected");
        if (ok) logService.logAction("Note Rejected", "Note", title);
        return ok;
    }

    public boolean makeNotePublic(int noteId, String title) {
        boolean ok = adminDAO.updateNoteVisibility(noteId, false);
        if (ok) logService.logAction("Note Made Public", "Note", title);
        return ok;
    }

    public boolean makeNotePrivate(int noteId, String title) {
        boolean ok = adminDAO.updateNoteVisibility(noteId, true);
        if (ok) logService.logAction("Note Made Private", "Note", title);
        return ok;
    }

    public boolean deleteNote(int noteId, String title) {
        boolean ok = adminDAO.softDeleteNote(noteId);
        if (ok) logService.logAction("Note Deleted", "Note", title);
        return ok;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Resource Moderation
    // ══════════════════════════════════════════════════════════════════════════

    public List<Resource> getResources() { return adminDAO.getAllResources(); }

    public boolean activateResource(int id, String title) {
        boolean ok = adminDAO.updateResourceStatus(id, true);
        if (ok) logService.logAction("Resource Activated", "Resource", title);
        return ok;
    }

    public boolean deactivateResource(int id, String title) {
        boolean ok = adminDAO.updateResourceStatus(id, false);
        if (ok) logService.logAction("Resource Deactivated", "Resource", title);
        return ok;
    }

    public boolean deleteResource(int id, String title) {
        boolean ok = adminDAO.deleteResource(id);
        if (ok) logService.logAction("Resource Deleted", "Resource", title);
        return ok;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Q&A Moderation
    // ══════════════════════════════════════════════════════════════════════════

    public List<Question> getQuestions() { return adminDAO.getAllQuestions(); }

    public List<Answer> getAnswersForQuestion(int questionId) {
        return adminDAO.getAnswersForQuestion(questionId);
    }

    public boolean deleteQuestion(int questionId, String questionText) {
        boolean ok = adminDAO.deleteQuestion(questionId);
        if (ok) logService.logAction("Question Deleted", "Question",
                questionText.length() > 60 ? questionText.substring(0, 60) + "…" : questionText);
        return ok;
    }

    public boolean deleteAnswer(int answerId, String questionText) {
        boolean ok = adminDAO.deleteAnswer(answerId);
        if (ok) logService.logAction("Answer Deleted", "Question", questionText);
        return ok;
    }

    public boolean lockQuestion(int questionId, String questionText) {
        boolean ok = adminDAO.setQuestionLocked(questionId, true);
        if (ok) logService.logAction("Question Locked", "Question",
                questionText.length() > 60 ? questionText.substring(0, 60) + "…" : questionText);
        return ok;
    }

    public boolean unlockQuestion(int questionId, String questionText) {
        boolean ok = adminDAO.setQuestionLocked(questionId, false);
        if (ok) logService.logAction("Question Unlocked", "Question",
                questionText.length() > 60 ? questionText.substring(0, 60) + "…" : questionText);
        return ok;
    }

    public boolean toggleLockDiscussion(int questionId) {
        return adminDAO.toggleLockDiscussion(questionId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Reports
    // ══════════════════════════════════════════════════════════════════════════

    public List<Map<String, Object>> getTopActiveUsers(int limit) {
        return adminDAO.getTopActiveUsers(limit);
    }

    public List<Map<String, Object>> getTopDownloadedResources(int limit) {
        return adminDAO.getTopDownloadedResources(limit);
    }

    public Map<String, Integer> getMonthlyUploads() {
        return adminDAO.getMonthlyUploads();
    }

    public Map<String, Integer> getMonthlyRegistrations() {
        return adminDAO.getMonthlyRegistrations();
    }

    public Map<String, Integer> getNotesBySubject() {
        return adminDAO.getNotesBySubject();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Global Search
    // ══════════════════════════════════════════════════════════════════════════

    public List<User>     globalSearchUsers(String q)     { return adminDAO.searchUsersGlobal(q); }
    public List<Note>     globalSearchNotes(String q)     { return adminDAO.searchNotes(q); }
    public List<Resource> globalSearchResources(String q) { return adminDAO.searchResources(q); }
    public List<Question> globalSearchQuestions(String q) { return adminDAO.searchQuestions(q); }
}
