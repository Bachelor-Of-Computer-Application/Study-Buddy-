package com.studybuddy.admin.services;

import com.studybuddy.admin.dao.AdminDAO;
import com.studybuddy.services.FileStorageService;
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

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(AdminService.class.getName());

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
     * Validate admin credentials. Accepts either username or email address.
     * On success, stores the User in SessionManager and prints verification output.
     * @return true if login is valid
     */
    public boolean validateAdminLogin(String usernameOrEmail, String password) {
        System.out.println("[AdminService] Attempting login for: " + usernameOrEmail);

        // Support both username and email — try the combined lookup first.
        User user = new UserDAO().getUserByUsernameOrEmail(usernameOrEmail);

        // Fallback: plain email lookup (covers edge cases where username column is null)
        if (user == null) {
            user = new UserDAO().getUserByEmail(usernameOrEmail);
        }

        if (user == null) {
            System.out.println("[AdminService] User not found in database: " + usernameOrEmail);
            return false;
        }

        System.out.println("[AdminService] User found. ID: " + user.getId()
                + ", Name: "  + user.getName()
                + ", Role: "  + user.getRole()
                + ", Email: " + user.getEmail());

        // Accept both role values used in the DB (ADMIN and Administrator)
        String role = user.getRole();
        boolean isAdmin = role != null
                && (role.equalsIgnoreCase("ADMIN")
                    || role.equalsIgnoreCase("Administrator"));

        if (!isAdmin) {
            System.out.println("[AdminService] User is not an admin. Role: " + role);
            return false;
        }

        // Verify password — supports BCrypt hash and plain-text dev seeds
        boolean valid = PasswordHasher.verifyPassword(password, user.getPassword())
                || password.equals(user.getPassword());

        if (!valid) {
            System.out.println("[AdminService] Password verification failed");
            return false;
        }

        // ── Success ──────────────────────────────────────────────────────────
        SessionManager.setCurrentAdmin(user);

        // Verification output (as required)
        System.out.println("[AdminService] ✓ Admin login successful.");
        LOGGER.info("Authenticated Admin ID = " + user.getId());
        System.out.println("Username = "              + user.getUsername());
        System.out.println("Email = "                 + user.getEmail());
        System.out.println("Role = "                  + user.getRole());

        // Confirm SessionManager stored correctly
        User storedAdmin = SessionManager.getCurrentAdmin();
        if (storedAdmin != null) {
            LOGGER.info("[AdminService] ✓ SessionManager.getCurrentAdmin() ID: " + storedAdmin.getId());
        } else {
            LOGGER.warning("[AdminService] ✗ SessionManager.getCurrentAdmin() is NULL!");
        }

        logService.logAction("Admin Login", "Session",
                user.getUsername() != null ? user.getUsername() : user.getEmail());
        return true;
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
    public User getUserById(int userId) { return adminDAO.getUserById(userId); }
    public int getUserTotalNotes(int userId) { return adminDAO.getUserTotalNotes(userId); }
    public int getUserTotalResources(int userId) { return adminDAO.getUserTotalResources(userId); }
    public int getUserTotalQuestions(int userId) { return adminDAO.getUserTotalQuestions(userId); }
    public List<Note> getUserRecentNotes(int userId, int limit) { return adminDAO.getUserRecentNotes(userId, limit); }
    public List<Resource> getUserRecentResources(int userId, int limit) { return adminDAO.getUserRecentResources(userId, limit); }
    public List<Map<String, Object>> getUserApprovalHistory(int userId, int limit) { return adminDAO.getUserApprovalHistory(userId, limit); }

    public List<User> searchUsers(String query) { return adminDAO.searchUsers(query); }

    public boolean suspendUser(int userId, String userName) {
        boolean ok = adminDAO.updateUserStatus(userId, "Suspended");
        if (ok) {
            logService.logAction("User Suspended", "User", userName);
            com.studybuddy.utils.EventBus.getInstance().publish(new com.studybuddy.utils.EventBus.AdminChangesEvent());
        }
        return ok;
    }

    public boolean activateUser(int userId, String userName) {
        boolean ok = adminDAO.updateUserStatus(userId, "Active");
        if (ok) {
            logService.logAction("User Activated", "User", userName);
            com.studybuddy.utils.EventBus.getInstance().publish(new com.studybuddy.utils.EventBus.AdminChangesEvent());
        }
        return ok;
    }

    public boolean banUser(int userId, String userName) {
        boolean ok = adminDAO.updateUserStatus(userId, "Banned");
        if (ok) {
            logService.logAction("User Banned", "User", userName);
            com.studybuddy.utils.EventBus.getInstance().publish(new com.studybuddy.utils.EventBus.AdminChangesEvent());
        }
        return ok;
    }

    public boolean editUserInfo(int userId, String name, String email, String role,
                                String department, String semester) {
        // Get old user info for detailed logging
        User oldUser = adminDAO.getUserById(userId);
        
        boolean ok = adminDAO.updateUserInfo(userId, name, email, role, department, semester);
        
        if (ok) {
            // Build detailed change log
            StringBuilder changes = new StringBuilder();
            changes.append("User: ").append(email).append("\n");
            changes.append("Changed:\n");
            
            if (oldUser != null) {
                if (!name.equals(oldUser.getName())) {
                    changes.append("- Name: '").append(oldUser.getName()).append("' → '").append(name).append("'\n");
                }
                if (!email.equals(oldUser.getEmail())) {
                    changes.append("- Email: '").append(oldUser.getEmail()).append("' → '").append(email).append("'\n");
                }
                if (department != null && !department.equals(oldUser.getDepartment())) {
                    changes.append("- Department: '").append(oldUser.getDepartment()).append("' → '").append(department).append("'\n");
                }
                if (semester != null && !semester.equals(oldUser.getSemester())) {
                    changes.append("- Semester: '").append(oldUser.getSemester()).append("' → '").append(semester).append("'\n");
                }
            }
            
            logService.logAction("User Info Updated", "User", changes.toString());
            
            // Publish events for real-time UI updates
            com.studybuddy.utils.EventBus.getInstance().publish(new com.studybuddy.utils.EventBus.AdminChangesEvent());
            com.studybuddy.utils.EventBus.getInstance().publish(new com.studybuddy.utils.EventBus.StatisticsChangedEvent());
        }
        
        return ok;
    }

    public boolean resetPassword(int userId, String email, String newPassword) {
        String hashed = PasswordHasher.hashPassword(newPassword);
        boolean ok = adminDAO.resetUserPassword(userId, hashed);
        if (ok) logService.logAction("Password Reset", "User", email);
        return ok;
    }

    /**
     * @deprecated Role changes should not be performed through the UI.
     * This method is kept for backward compatibility only and is not exposed
     * in the AdminUsersController UI.
     */
    @Deprecated
    public boolean promoteToAdmin(int userId, String userName) {
        boolean ok = adminDAO.promoteToAdmin(userId);
        if (ok) logService.logAction("User Promoted to Admin", "User", userName);
        return ok;
    }

    /**
     * @deprecated Role changes should not be performed through the UI.
     * This method is kept for backward compatibility only and is not exposed
     * in the AdminUsersController UI.
     */
    @Deprecated
    public boolean demoteToUser(int userId, String userName) {
        boolean ok = adminDAO.demoteToUser(userId);
        if (ok) logService.logAction("User Demoted to Student", "User", userName);
        return ok;
    }

    public boolean softDeleteUser(int userId, String userName) {
        boolean ok = adminDAO.softDeleteUser(userId);
        if (ok) {
            logService.logAction("User Soft Deleted", "User", userName);
            // Publish event for UI updates
            com.studybuddy.utils.EventBus.getInstance().publish(new com.studybuddy.utils.EventBus.AdminChangesEvent());
            com.studybuddy.utils.EventBus.getInstance().publish(new com.studybuddy.utils.EventBus.StatisticsChangedEvent());
        }
        return ok;
    }
    
    /**
     * Permanently delete user and all related records with proper transaction handling.
     * Logs detailed information about all deleted items and publishes events for UI updates.
     * 
     * @param userId   User ID to delete
     * @param userName User's name for logging
     * @return DeletionResult with success status, counts, and error message if any
     */
    public AdminDAO.DeletionResult hardDeleteUser(int userId, String userName) {
        System.out.println(">>> SERVICE: Entering AdminService.hardDeleteUser() for user=" + userName + ", id=" + userId);
        
        AdminDAO.DeletionResult result = adminDAO.hardDeleteUser(userId);
        
        System.out.println(">>> SERVICE: AdminDAO.hardDeleteUser() returned. Success=" + result.success);
        
        if (result.success) {
            System.out.println(">>> SERVICE: Deletion successful, creating activity log...");
            
            // Log detailed deletion information
            StringBuilder details = new StringBuilder();
            details.append("User: ").append(userName).append("\n");
            details.append("Deleted:\n");
            details.append("- User account\n");
            if (result.notesCount > 0) details.append("- ").append(result.notesCount).append(" Note(s)\n");
            if (result.resourcesCount > 0) details.append("- ").append(result.resourcesCount).append(" Resource(s)\n");
            if (result.questionsCount > 0) details.append("- ").append(result.questionsCount).append(" Question(s)\n");
            if (result.answersCount > 0) details.append("- ").append(result.answersCount).append(" Answer(s)\n");
            if (result.tasksCount > 0) details.append("- ").append(result.tasksCount).append(" Task(s)\n");
            if (result.notificationsCount > 0) details.append("- ").append(result.notificationsCount).append(" Notification(s)\n");
            
            System.out.println(">>> SERVICE: Calling logService.logAction()...");
            logService.logAction("User Permanently Deleted", "User", details.toString());
            System.out.println(">>> SERVICE: Activity log completed");
            
            // NOTE: EventBus events are NOT published here because:
            // 1. This runs on a background thread
            // 2. AdminUsersController.handleDelete() already handles UI refresh via scheduleBackgroundReload()
            // 3. Publishing events from background thread causes subscribers to queue Platform.runLater()
            //    with DB calls, which execute during showAndWait() modal dialog blocking, freezing the UI.
            // The explicit scheduleBackgroundReload() in AdminUsersController provides a clean, controlled
            // reload after all dialogs have closed.
            System.out.println(">>> SERVICE: Skipping EventBus events (handled by AdminUsersController.scheduleBackgroundReload)");
        } else {
            System.out.println(">>> SERVICE: Deletion failed, logging failure...");
            logService.logAction("User Deletion Failed", "User", 
                userName + " - Error: " + result.errorMessage);
        }
        
        System.out.println(">>> SERVICE: Exiting AdminService.hardDeleteUser()");
        return result;
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

    /**
     * Permanently deletes a note and its linked resources. Returns null on full success,
     * or a user-facing message when partial failure occurs.
     */
    public String deleteNoteWithFile(int noteId, String title) {
        com.studybuddy.dao.NoteDAO noteDAO = new com.studybuddy.dao.NoteDAO();
        try {
            com.studybuddy.models.Note note = noteDAO.getNoteById(noteId);
            if (note == null) {
                return "Note not found (id=" + noteId + "). It may have already been deleted.";
            }
            boolean dbDeleted = adminDAO.hardDeleteNote(noteId);
            if (!dbDeleted) {
                return "Could not delete note from the database. Check server logs for FK or SQL errors.";
            }
            logService.logAction("Note Deleted", "Note", title);

            String filePath = note.getFilePath();
            if (filePath != null && !filePath.isBlank()) {
                boolean fileRemoved = FileStorageService.getInstance().deleteFile(filePath);
                if (!fileRemoved && new java.io.File(filePath).exists()) {
                    return "Note was removed from the database, but the file could not be deleted:\n" + filePath;
                }
            }
            return null;
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(AdminService.class.getName())
                    .log(java.util.logging.Level.SEVERE, "deleteNoteWithFile failed for noteId=" + noteId, e);
            return "Delete failed: " + e.getMessage();
        }
    }

    public boolean updateNoteStatus(int noteId, String status, String title) {
        boolean ok = adminDAO.updateNoteStatus(noteId, status);
        if (ok) logService.logAction("Note Status: " + status, "Note", title);
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

    public boolean deleteResourceWithFile(int id, String title) {
        try {
            com.studybuddy.dao.ResourceDAO dao = new com.studybuddy.dao.ResourceDAO();
            com.studybuddy.models.Resource r = dao.getResourceById(id);
            boolean ok = dao.hardDeleteResource(id);
            if (ok && r != null) {
                FileStorageService.getInstance().deleteFile(r.getFilePath());
                logService.logAction("Resource Deleted", "Resource", title);
            }
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateResourceStatus(int id, String status, String title) {
        boolean ok = adminDAO.updateResourceApprovalStatus(id, status);
        if (ok) logService.logAction("Resource Status: " + status, "Resource", title);
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