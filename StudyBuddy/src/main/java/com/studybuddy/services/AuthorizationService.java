package com.studybuddy.services;

import com.studybuddy.models.Note;
import com.studybuddy.models.Question;
import com.studybuddy.models.Resource;
import com.studybuddy.models.User;

/**
 * Central role-based authorization checks for content operations.
 */
public class AuthorizationService {

    private static AuthorizationService instance;

    private AuthorizationService() {}

    public static synchronized AuthorizationService getInstance() {
        if (instance == null) {
            instance = new AuthorizationService();
        }
        return instance;
    }

    public boolean isAdmin(User user) {
        return user != null && user.getRole() != null
                && ("admin".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole()));
    }

    public boolean isStudent(User user) {
        return user != null && !isAdmin(user);
    }

    public boolean canManageDepartments(User user) {
        return isAdmin(user);
    }

    public boolean canManageSemesters(User user) {
        return isAdmin(user);
    }

    public boolean canManageNotifications(User user) {
        return isAdmin(user);
    }

    public boolean canApproveContent(User user) {
        return isAdmin(user);
    }

    public boolean canEditNote(User user, Note note) {
        if (user == null || note == null) return false;
        if (isAdmin(user)) return true;
        return note.getUserId() == user.getId();
    }

    public boolean canDeleteNote(User user, Note note) {
        return canEditNote(user, note);
    }

    public boolean canEditResource(User user, Resource resource) {
        if (user == null || resource == null) return false;
        if (isAdmin(user)) return true;
        return resource.getUploadedBy() == user.getId();
    }

    public boolean canDeleteResource(User user, Resource resource) {
        return canEditResource(user, resource);
    }

    public boolean canEditQuestion(User user, Question question) {
        if (user == null || question == null) return false;
        if (isAdmin(user)) return true;
        return question.getUserId() == user.getId();
    }

    public boolean canDeleteQuestion(User user, Question question) {
        return canEditQuestion(user, question);
    }

    public boolean canViewApprovedOnly(User user) {
        return isStudent(user);
    }

    public void requireAdmin(User user) throws SecurityException {
        if (!isAdmin(user)) {
            throw new SecurityException("Administrator privileges required.");
        }
    }

    public void requireOwnership(User user, int ownerId) throws SecurityException {
        if (user == null) {
            throw new SecurityException("Not authenticated.");
        }
        if (!isAdmin(user) && user.getId() != ownerId) {
            throw new SecurityException("You can only modify your own content.");
        }
    }
}
