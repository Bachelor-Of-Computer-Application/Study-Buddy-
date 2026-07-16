package com.studybuddy.services;

import com.studybuddy.dao.ResourceDAO;
import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;
import com.studybuddy.models.User;
import com.studybuddy.utils.EventBus;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Service layer for Resource operations.
 * FIXED: Now delegates all database calls to ResourceDAO instead of
 * running its own JDBC queries directly.
 *
 * Architecture: Controller → ResourceService → ResourceDAO → DatabaseConnection → SQL Server
 */
public class ResourceService {

    private final ResourceDAO resourceDAO = new ResourceDAO();

    // =========================
    // SHARE AS RESOURCE
    // =========================

    /**
     * Shares a note as a public resource.
     * Delegates to ResourceDAO.shareAsResource().
     * SQL: INSERT INTO Resources (noteId, uploadedBy, title, subject, source,
     *      description, uploadDate, filePath, fileType, downloads, isActive)
     */
    public void shareAsResource(Note note, String filePath, boolean autoApprove) throws SQLException {
        resourceDAO.shareAsResource(note, filePath, autoApprove);
        EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
    }

    // =========================
    // GET ALL ACTIVE RESOURCES
    // =========================

    /**
     * Returns all active (approved) resources.
     * SQL: SELECT * FROM Resources WHERE isActive = 1 ORDER BY uploadDate DESC
     */
    public List<Resource> getAllActiveResources() throws SQLException {
        return resourceDAO.getAllActiveResources();
    }

    // =========================
    // GET RESOURCES BY USER
    // =========================

    /**
     * Returns all resources uploaded by a specific user.
     * SQL: SELECT * FROM Resources WHERE uploadedBy = ? ORDER BY uploadDate DESC
     */
    public List<Resource> getResourcesByUser(int userId) throws SQLException {
        return resourceDAO.getResourcesByUser(userId);
    }

    // =========================
    // DELETE RESOURCE (SOFT DELETE)
    // =========================

    /**
     * Soft-deletes a resource by setting isActive = 0.
     * SQL: UPDATE Resources SET isActive = 0 WHERE id = ?
     */
    public boolean deleteResource(int id) throws SQLException {
        return resourceDAO.deleteResource(id);
    }

    // =========================
    // COUNT ACTIVE RESOURCES
    // =========================

    /**
     * Returns the count of all active resources.
     * SQL: SELECT COUNT(*) FROM Resources WHERE isActive = 1
     */
    public int countActiveResources() throws SQLException {
        return resourceDAO.countActiveResources();
    }

    // =========================
    // COUNT RESOURCES BY USER
    // =========================

    /**
     * Returns the count of resources uploaded by a specific user.
     */
    public int countResourcesByUser(int userId) throws SQLException {
        return resourceDAO.countResourcesByUser(userId);
    }

    // =========================
    // GET ALL SUBJECT NAMES
    // =========================

    /**
     * Returns all distinct, active subject names for filter ComboBoxes.
     * Canonical Subjects table + backward-compat legacy subjects already in Resources.
     */
    public List<String> getAllSubjectNames() {
        try {
            return resourceDAO.getAllSubjectNames();
        } catch (SQLException e) {
            System.err.println("[ResourceService] Could not load subject names: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    public Resource getResourceById(int id) throws SQLException {
        return resourceDAO.getResourceById(id);
    }

    public int createResource(Resource resource, File file, boolean autoApprove) throws SQLException, IOException {
        if (resourceDAO.duplicateExists(resource.getTitle(), resource.getUploadedBy(), null)) {
            throw new IllegalArgumentException("A resource with this name already exists.");
        }
        FileStorageService storage = FileStorageService.getInstance();
        if (file != null) {
            String path = storage.storeFile(file, "resources");
            resource.setFilePath(path);
            resource.setFileType(extension(file.getName()));
        }
        resource.setStatus(autoApprove ? "Approved" : "Pending");
        resource.setActive(autoApprove);
        int id = resourceDAO.createResource(resource, autoApprove);
        EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
        com.studybuddy.admin.services.ActivityLogService.getInstance().logAction("Resource Uploaded", "Resource", resource.getTitle());
        return id;
    }

    public boolean updateResource(Resource resource) throws SQLException {
        return resourceDAO.updateResource(resource);
    }

    public boolean deleteResourceWithFile(int id, User user) throws SQLException {
        Resource r = resourceDAO.getResourceById(id);
        if (r == null) return false;
        AuthorizationService.getInstance().requireOwnership(user, r.getUploadedBy());
        resourceDAO.hardDeleteResource(id);
        FileStorageService.getInstance().deleteFile(r.getFilePath());
        com.studybuddy.admin.services.ActivityLogService.getInstance().logAction("Resource Deleted", "Resource", r.getTitle());
        return true;
    }

    public void incrementDownloads(int id) throws SQLException {
        resourceDAO.incrementDownloads(id);
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toUpperCase() : "FILE";
    }
}