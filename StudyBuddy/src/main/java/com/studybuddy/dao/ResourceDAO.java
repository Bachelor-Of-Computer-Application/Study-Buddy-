package com.studybuddy.dao;

import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Resource operations.
 *
 * Resources table schema assumed:
 *   id, noteId, uploadedBy, title, subject, subjectId, source, description,
 *   uploadDate, filePath, fileType, downloads, isActive
 *
 * subjectId is a nullable FK to Subjects(id) — introduced by the Dept→Sem→Subject
 * migration.  Rows created before the migration have subjectId = NULL and are
 * handled with backward-compatible null-checks.
 */
public class ResourceDAO {

    // =========================
    // GET RESOURCE BY ID
    // =========================

    /**
     * Returns a single resource by its ID.
     * SQL: SELECT * FROM Resources WHERE id = ?
     */
    public Resource getResourceById(int resourceId) throws SQLException {
        String sql = "SELECT r.*, u.name, u.fullName, u.department, u.semester " +
                "FROM Resources r LEFT JOIN Users u ON r.uploadedBy = u.id " +
                "WHERE r.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, resourceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResource(rs);
                }
            }
        }

        return null;
    }

    // =========================
    // SHARE AS RESOURCE
    // =========================

    /**
     * Inserts a new resource row linked to a shared Note.
     * Both subject (display name) and subjectId (FK) are persisted so the row
     * is usable by both old and new filter queries.
     *
     * SQL: INSERT INTO Resources
     *      (noteId, uploadedBy, title, subject, subjectId, source, description,
     *       uploadDate, filePath, fileType, downloads, isActive)
     *      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)
     */
    public void shareAsResource(Note note, String filePath, boolean autoApprove) throws SQLException {
        int isActiveValue = autoApprove ? 1 : 0;
        String sql =
            "INSERT INTO Resources " +
            "(noteId, uploadedBy, title, subject, subjectId, source, description, " +
            " uploadDate, filePath, fileType, downloads, isActive) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (note.getId() <= 0) {
                stmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(1, note.getId());
            }
            stmt.setInt(2, note.getUserId());
            stmt.setString(3, note.getTitle());
            stmt.setString(4, note.getSubject());

            // subjectId — NULL when not selected (backward-compatible)
            if (note.getSubjectId() > 0) {
                stmt.setInt(5, note.getSubjectId());
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
            }

            stmt.setString(6, note.getSource());
            stmt.setString(7, note.getDescription());
            stmt.setString(8, note.getUploadDate());
            stmt.setString(9, filePath);
            stmt.setString(10, note.getFileType());
            stmt.setInt(11, isActiveValue);
            stmt.executeUpdate();
        }
    }

    // =========================
    // GET ALL ACTIVE RESOURCES
    // =========================

    /**
     * Returns all active (approved) resources ordered by upload date descending.
     * SQL: SELECT * FROM Resources WHERE isActive = 1 ORDER BY uploadDate DESC
     */
    public List<Resource> getAllActiveResources() throws SQLException {
        String sql = "SELECT r.*, u.name, u.fullName, u.department, u.semester " +
                "FROM Resources r LEFT JOIN Users u ON r.uploadedBy = u.id " +
                "WHERE r.isActive = 1 ORDER BY r.uploadDate DESC";
        List<Resource> resources = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                resources.add(mapResource(rs));
            }
        }

        return resources;
    }

    // =========================
    // GET RESOURCES BY USER
    // =========================

    /**
     * Returns all resources uploaded by a specific user.
     * SQL: SELECT * FROM Resources WHERE uploadedBy = ? ORDER BY uploadDate DESC
     */
    public List<Resource> getResourcesByUser(int userId) throws SQLException {
        String sql = "SELECT r.*, u.name, u.fullName, u.department, u.semester " +
                "FROM Resources r LEFT JOIN Users u ON r.uploadedBy = u.id " +
                "WHERE r.uploadedBy = ? ORDER BY r.uploadDate DESC";
        List<Resource> resources = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapResource(rs));
                }
            }
        }

        return resources;
    }

    // =========================
    // DELETE RESOURCE (SOFT DELETE)
    // =========================

    /**
     * Soft-deletes a resource by setting isActive = 0.
     * SQL: UPDATE Resources SET isActive = 0 WHERE id = ?
     */
    public boolean deleteResource(int id) throws SQLException {
        String sql = "UPDATE Resources SET isActive = 0 WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // =========================
    // COUNT ACTIVE RESOURCES
    // =========================

    /**
     * Returns the count of all active resources.
     * SQL: SELECT COUNT(*) FROM Resources WHERE isActive = 1
     */
    public int countActiveResources() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Resources WHERE isActive = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // =========================
    // COUNT RESOURCES BY USER
    // =========================

    /**
     * Returns the count of resources uploaded by a specific user.
     */
    public int countResourcesByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Resources WHERE uploadedBy = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // =========================
    // GET ALL SUBJECT NAMES
    // =========================

    /**
     * Returns all distinct, active subject names from the canonical Subjects table.
     * Used to populate subject filter ComboBoxes in the Resources screens.
     * Falls back to subjects already stored in the Resources table for backward-
     * compatibility with pre-migration rows.
     *
     * SQL: canonical Subjects UNION legacy resource subjects not in Subjects
     */
    public List<String> getAllSubjectNames() throws SQLException {
        String sql =
            "SELECT name AS subject FROM Subjects WHERE isActive = 1 " +
            "UNION " +
            "SELECT DISTINCT subject FROM Resources " +
            "WHERE subject IS NOT NULL " +
            "  AND subject NOT IN (SELECT name FROM Subjects WHERE isActive = 1) " +
            "ORDER BY subject";

        List<String> subjects = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                subjects.add(rs.getString("subject"));
            }
        }

        return subjects;
    }

    // =========================
    // HELPER — MAP ResultSet TO Resource
    // =========================

    private Resource mapResource(ResultSet rs) throws SQLException {
        Resource resource = new Resource();
        resource.setId(rs.getInt("id"));

        int noteIdVal = rs.getInt("noteId");
        resource.setNoteId(rs.wasNull() ? null : noteIdVal);

        resource.setUploadedBy(rs.getInt("uploadedBy"));
        resource.setTitle(rs.getString("title"));
        resource.setSubject(rs.getString("subject"));

        // subjectId — nullable; guard for pre-migration rows that lack the column
        try {
            int subjectId = rs.getInt("subjectId");
            if (!rs.wasNull()) {
                resource.setSubjectId(subjectId);
            }
        } catch (SQLException ignored) {
            // column not yet present in this DB schema
        }

        resource.setSource(rs.getString("source"));
        resource.setDescription(rs.getString("description"));
        resource.setUploadDate(rs.getString("uploadDate"));
        resource.setFilePath(rs.getString("filePath"));
        resource.setFileType(rs.getString("fileType"));
        resource.setDownloads(rs.getInt("downloads"));
        resource.setActive(rs.getBoolean("isActive"));

        // Load user info (full name, department, semester)
        try {
            String fullName = rs.getString("fullName");
            if (fullName == null || fullName.isEmpty()) {
                fullName = rs.getString("name"); // Fallback to name field
            }
            resource.setUserFullName(fullName != null ? fullName : "Unknown User");
        } catch (SQLException ignored) {
            resource.setUserFullName("Unknown User");
        }

        try {
            resource.setUserDepartment(rs.getString("department"));
        } catch (SQLException ignored) {
            resource.setUserDepartment(null);
        }

        try {
            resource.setUserSemester(rs.getString("semester"));
        } catch (SQLException ignored) {
            resource.setUserSemester(null);
        }

        return resource;
    }
}
