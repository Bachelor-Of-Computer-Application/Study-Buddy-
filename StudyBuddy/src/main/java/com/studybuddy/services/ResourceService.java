package com.studybuddy.services;

import com.studybuddy.dao.ResourceDAO;
import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;

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
}