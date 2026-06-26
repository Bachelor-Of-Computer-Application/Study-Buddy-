package com.studybuddy.admin.services;

import com.studybuddy.admin.dao.AdminDAO;
import com.studybuddy.models.User;
import com.studybuddy.models.Resource;
import com.studybuddy.models.Question;
import com.studybuddy.models.Note;
import com.studybuddy.utils.PasswordHasher;

import java.util.List;

public class AdminService {
    private static AdminService instance;
    private final AdminDAO adminDAO = AdminDAO.getInstance();

    private AdminService() {}

    public static synchronized AdminService getInstance() {
        if (instance == null) {
            instance = new AdminService();
        }
        return instance;
    }

    // ===========================
    // User Moderation
    // ===========================
    public List<User> getUsers() {
        return adminDAO.getAllUsers();
    }

    public void suspendUser(int userId) {
        adminDAO.updateUserStatus(userId, "Suspended");
    }

    public void activateUser(int userId) {
        adminDAO.updateUserStatus(userId, "Active");
    }

    public void banUser(int userId) {
        adminDAO.updateUserStatus(userId, "Banned");
    }

    public void editUserInfo(int userId, String name, String email, String role) {
        adminDAO.updateUserInfo(userId, name, email, role);
    }

    public void deleteUser(int userId) {
        adminDAO.deleteUser(userId);
    }

    // ===========================
    // Content Moderation
    // ===========================
    public List<Resource> getResources() {
        return adminDAO.getAllResources();
    }

    public void approveResource(int resourceId) {
        adminDAO.updateResourceStatus(resourceId, true);
    }

    public void rejectResource(int resourceId) {
        adminDAO.updateResourceStatus(resourceId, false);
    }

    public List<Note> getNotes() {
        return adminDAO.getAllNotes();
    }

    public void approveNote(int noteId) {
        adminDAO.updateNoteStatus(noteId, true);
    }

    public void rejectNote(int noteId) {
        adminDAO.updateNoteStatus(noteId, false);
    }

    // ===========================
    // Q&A Moderation
    // ===========================
    public List<Question> getQuestions() {
        return adminDAO.getAllQuestions();
    }

    public void deleteQuestion(int questionId) {
        adminDAO.deleteQuestion(questionId);
    }

    public void deleteAnswer(int answerId) {
        adminDAO.deleteAnswer(answerId);
    }

    public void toggleLockDiscussion(int questionId) {
        adminDAO.toggleLockDiscussion(questionId);
    }

    // ===========================
    // Login Verification
    // ===========================
    public boolean validateAdminLogin(String username, String password) {
        User user = new com.studybuddy.dao.UserDAO().getUserByEmail(username);
        if (user != null && ("admin".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole()))) {
            if (PasswordHasher.verifyPassword(password, user.getPassword())) {
                return true;
            }
            // Fallback for plain-text password if inserted directly via testing script without hashing
            if (password.equals(user.getPassword())) {
                return true;
            }
        }
        // Fallback for default hardcoded admin from previous system config
        return "admin".equalsIgnoreCase(username) && "admin123".equals(password);
    }
}
