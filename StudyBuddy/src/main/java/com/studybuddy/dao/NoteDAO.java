package com.studybuddy.dao;

import com.studybuddy.models.Note;
import com.studybuddy.utils.EventBus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Note operations.
 * All SQL column names match the actual Notes table schema:
 *   id, userId, title, subject, subjectId, source, uploadDate, fileType,
 *   fileName, description, isPrivate, filePath, status
 */
public class NoteDAO {

    // =========================
    // GET NOTE BY ID
    // =========================

    /**
     * Returns a single note by its ID.
     * SQL: SELECT * FROM Notes WHERE id = ?
     */
    public Note getNoteById(int noteId) throws SQLException {
        String sql = "SELECT n.*, u.name AS userName, u.fullName, u.department, u.semester FROM Notes n " +
                "LEFT JOIN Users u ON n.userId = u.id " +
                "WHERE n.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, noteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapNote(rs);
                }
            }
        }

        return null;
    }

    // =========================
    // GET NOTES BY USER ID
    // =========================

    /**
     * Returns all notes belonging to the given user.
     * SQL: SELECT * FROM Notes WHERE userId = ? ORDER BY uploadDate DESC
     */
    public List<Note> getNotesByUserId(int userId) throws SQLException {
        // Uses actual SQL column name: userId (not user_id)
        // LEFT JOIN Users to populate the transient userName display field.
        String sql = "SELECT n.*, u.name AS userName, u.fullName, u.department, u.semester FROM Notes n " +
                "LEFT JOIN Users u ON n.userId = u.id " +
                "WHERE n.userId = ? ORDER BY n.uploadDate DESC";
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapNote(rs));
                }
            }
        }

        return notes;
    }

    // =========================
    // GET ALL PUBLIC NOTES (for Dashboard / Community)
    // =========================

    /**
     * Returns all non-private notes ordered by upload date.
     * Used by DashboardService to replace dummy data.
     * SQL: SELECT * FROM Notes WHERE isPrivate = 0 ORDER BY uploadDate DESC
     */
    public List<Note> getAllPublicNotes() throws SQLException {
        // Uses actual SQL column name: isPrivate (not is_private)
        String sql = "SELECT n.*, u.name AS userName, u.fullName, u.department, u.semester FROM Notes n " +
                "LEFT JOIN Users u ON n.userId = u.id " +
                "WHERE n.isPrivate = 0 AND n.status = 'Approved' ORDER BY n.uploadDate DESC";
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                notes.add(mapNote(rs));
            }
        }

        return notes;
    }

    // =========================
    // GET RECENT PUBLIC NOTES
    // =========================

    /**
     * Returns the most recent public notes up to the given limit.
     * SQL: SELECT TOP ? * FROM Notes WHERE isPrivate = 0 ORDER BY uploadDate DESC
     */
    public List<Note> getRecentNotes(int limit) throws SQLException {
        // Uses actual SQL column names: isPrivate, uploadDate
        String sql = "SELECT TOP (?) n.*, u.name AS userName, u.fullName, u.department, u.semester FROM Notes n " +
                "LEFT JOIN Users u ON n.userId = u.id " +
                "WHERE n.isPrivate = 0 AND n.status = 'Approved' ORDER BY n.uploadDate DESC";
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapNote(rs));
                }
            }
        }

        return notes;
    }

    // =========================
    // SEARCH PUBLIC NOTES
    // =========================

    /**
     * Searches public notes by keyword and/or subject.
     */
    public List<Note> searchPublicNotes(String query, String subject) throws SQLException {
        // Uses actual SQL column names: isPrivate, uploadDate
        String sql = "SELECT n.*, u.name AS userName, u.fullName, u.department, u.semester FROM Notes n " +
                "LEFT JOIN Users u ON n.userId = u.id " +
                "WHERE n.isPrivate = 0 AND n.status = 'Approved' " +
                "  AND (? IS NULL OR n.title LIKE ? OR n.subject LIKE ? OR n.source LIKE ?) " +
                "  AND (? IS NULL OR n.subject = ?) " +
                "ORDER BY n.uploadDate DESC";

        String normalizedQuery   = (query   == null || query.trim().isEmpty())   ? null : "%" + query.trim()   + "%";
        String normalizedSubject = (subject == null || subject.trim().isEmpty()) ? null : subject.trim();
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizedQuery);
            stmt.setString(2, normalizedQuery);
            stmt.setString(3, normalizedQuery);
            stmt.setString(4, normalizedQuery);
            stmt.setString(5, normalizedSubject);
            stmt.setString(6, normalizedSubject);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapNote(rs));
                }
            }
        }

        return notes;
    }

    // =========================
    // CREATE NOTE
    // =========================

    /**
     * Inserts a new note into the Notes table.
     * NOTE: 'id' is NOT included — it is IDENTITY(1,1) and auto-generated by SQL Server.
     * SQL columns: title, subject, subjectId, source, uploadDate, fileType, fileName,
     *              filePath, description, userId, isPrivate, status
     */
    public void createNote(Note note, boolean autoApprove) throws SQLException {
        // id is excluded — SQL Server auto-generates it via IDENTITY(1,1)
        String status = autoApprove ? "Approved" : "Pending";
        String sql = "INSERT INTO Notes " +
                "(title, subject, subjectId, departmentId, semesterId, source, uploadDate, fileType, fileName, filePath, description, userId, isPrivate, status, tags, downloads) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1,  note.getTitle());
            stmt.setString(2,  note.getSubject());

            if (note.getSubjectId() > 0) {
                stmt.setInt(3, note.getSubjectId());
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            setNullableInt(stmt, 4, note.getDepartmentId());
            setNullableInt(stmt, 5, note.getSemesterId());
            stmt.setString(6,  note.getSource());
            stmt.setString(7,  note.getUploadDate());
            stmt.setString(8,  note.getFileType());
            stmt.setString(9,  note.getFileName());
            stmt.setString(10, note.getFilePath());
            stmt.setString(11, note.getDescription());
            stmt.setInt(12,    note.getUserId());
            stmt.setBoolean(13, note.isPrivate());
            stmt.setString(14, status);
            if (note.getTags() != null) {
                stmt.setString(15, note.getTags());
            } else {
                stmt.setNull(15, java.sql.Types.VARCHAR);
            }
            stmt.executeUpdate();
            
            // Publish events for achievement progress updates
            EventBus.getInstance().publish(new EventBus.StatisticsChangedEvent());
            EventBus.getInstance().publish(new EventBus.NotesChangedEvent());
        }
    }

    /**
     * Updates an existing note's metadata (not file path unless provided).
     */
    public boolean updateNote(Note note) throws SQLException {
        String sql = "UPDATE Notes SET title = ?, subject = ?, subjectId = ?, departmentId = ?, semesterId = ?, source = ?, " +
                "fileType = ?, fileName = ?, filePath = ?, description = ?, isPrivate = ?, " +
                "status = ?, tags = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, note.getTitle());
            stmt.setString(2, note.getSubject());
            if (note.getSubjectId() > 0) {
                stmt.setInt(3, note.getSubjectId());
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            setNullableInt(stmt, 4, note.getDepartmentId());
            setNullableInt(stmt, 5, note.getSemesterId());
            stmt.setString(6, note.getSource());
            stmt.setString(7, note.getFileType());
            stmt.setString(8, note.getFileName());
            stmt.setString(9, note.getFilePath());
            stmt.setString(10, note.getDescription());
            stmt.setBoolean(11, note.isPrivate());
            stmt.setString(12, note.getStatus());
            stmt.setString(13, note.getTags());
            stmt.setInt(14, note.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean duplicateExists(String title, int userId, Integer excludeId) throws SQLException {
        String sql = excludeId != null
                ? "SELECT COUNT(*) FROM Notes WHERE title = ? AND userId = ? AND id <> ? AND status <> 'Deleted'"
                : "SELECT COUNT(*) FROM Notes WHERE title = ? AND userId = ? AND status <> 'Deleted'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setInt(2, userId);
            if (excludeId != null) {
                stmt.setInt(3, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void incrementDownloads(int noteId) throws SQLException {
        String sql = "UPDATE Notes SET downloads = ISNULL(downloads, 0) + 1 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, noteId);
            stmt.executeUpdate();
        }
    }

    /**
     * Hard delete a note by ID.
     */
    public void hardDeleteNote(int id) throws SQLException {
        String sql = "DELETE FROM Notes WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // =========================
    // DELETE NOTE
    // =========================

    /**
     * Deletes a note by its integer ID.
     */
    public void deleteNote(int id) throws SQLException {
        String sql = "DELETE FROM Notes WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // =========================
    // COUNT NOTES BY USER
    // =========================

    /**
     * Counts the total number of notes for a given user.
     * SQL: SELECT COUNT(*) FROM Notes WHERE userId = ?
     */
    public int countNotesByUser(int userId) throws SQLException {
        // Uses actual SQL column name: userId (not user_id)
        String sql = "SELECT COUNT(*) FROM Notes WHERE userId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Returns map of subject name to count of notes for a user.
     * SQL: SELECT subject, COUNT(*) AS noteCount FROM Notes WHERE userId = ? GROUP BY subject
     */
    public java.util.Map<String, Integer> getNotesCountBySubjectForUser(int userId) throws SQLException {
        String sql = "SELECT subject, COUNT(*) AS noteCount FROM Notes WHERE userId = ? GROUP BY subject";
        java.util.Map<String, Integer> subjectCountMap = new java.util.HashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String subject = rs.getString("subject");
                    int count = rs.getInt("noteCount");
                    if (subject != null && !subject.isEmpty()) {
                        subjectCountMap.put(subject, count);
                    }
                }
            }
        }

        return subjectCountMap;
    }

    // =========================
    // GET NOTES BY SUBJECT ID
    // =========================

    /**
     * Returns all public notes for a specific subject.
     * SQL: SELECT * FROM Notes WHERE subjectId = ? AND isPrivate = 0 ORDER BY uploadDate DESC
     */
    public List<Note> getNotesBySubjectId(int subjectId) throws SQLException {
        String sql = "SELECT n.*, u.name AS userName, u.fullName, u.department, u.semester FROM Notes n " +
                "LEFT JOIN Users u ON n.userId = u.id " +
                "WHERE n.subjectId = ? AND n.isPrivate = 0 AND n.status = 'Approved' ORDER BY n.uploadDate DESC";
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapNote(rs));
                }
            }
        }

        return notes;
    }

    // =========================
    // SEARCH NOTES WITH SUBJECT HIERARCHY
    // =========================

    /**
     * Advanced search supporting Department/Semester/Subject filtering.
     * Joins with Subjects, Semesters, and Departments tables.
     */
    public List<Note> searchNotesWithHierarchy(String query, Integer departmentId, Integer semesterId, Integer subjectId) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT n.*, u.name AS userName, u.fullName, u.department, u.semester FROM Notes n ");
        sql.append("LEFT JOIN Users u ON n.userId = u.id ");
        sql.append("LEFT JOIN Subjects s ON n.subjectId = s.id ");
        sql.append("LEFT JOIN Semesters sem ON s.semesterId = sem.id ");
        sql.append("LEFT JOIN Departments d ON sem.departmentId = d.id ");
        sql.append("WHERE n.isPrivate = 0 AND n.status = 'Approved' ");
        
        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (n.title LIKE ? OR n.subject LIKE ? OR n.source LIKE ?) ");
        }
        if (departmentId != null) {
            sql.append("AND (COALESCE(n.departmentId, d.id) = ? OR (n.departmentId IS NULL AND n.subjectId IS NULL)) ");
        }
        if (semesterId != null) {
            sql.append("AND (COALESCE(n.semesterId, sem.id) = ? OR (n.semesterId IS NULL AND n.subjectId IS NULL)) ");
        }
        if (subjectId != null) {
            sql.append("AND s.id = ? ");
        }
        
        sql.append("ORDER BY n.uploadDate DESC");

        List<Note> notes = new ArrayList<>();
        String normalizedQuery = (query != null && !query.trim().isEmpty()) ? "%" + query.trim() + "%" : null;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            
            if (normalizedQuery != null) {
                stmt.setString(paramIndex++, normalizedQuery);
                stmt.setString(paramIndex++, normalizedQuery);
                stmt.setString(paramIndex++, normalizedQuery);
            }
            if (departmentId != null) {
                stmt.setInt(paramIndex++, departmentId);
            }
            if (semesterId != null) {
                stmt.setInt(paramIndex++, semesterId);
            }
            if (subjectId != null) {
                stmt.setInt(paramIndex++, subjectId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapNote(rs));
                }
            }
        }

        return notes;
    }

    // =========================
    // HELPER — MAP ResultSet TO Note
    // =========================

    /**
     * Maps a ResultSet row to a Note object.
     * All column names match the actual SQL schema exactly.
     */
    private Note mapNote(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getInt("id"));
        note.setTitle(rs.getString("title"));
        note.setSubject(rs.getString("subject"));
        
        // Safely read subjectId — column may be NULL or not exist in older DB versions
        try { 
            int subjectId = rs.getInt("subjectId");
            if (!rs.wasNull()) {
                note.setSubjectId(subjectId);
            }
        } catch (SQLException ignored) { 
            // Column doesn't exist or is NULL - maintain backward compatibility
        }

        readNullableIntColumn(rs, "departmentId", note::setDepartmentId);
        readNullableIntColumn(rs, "semesterId", note::setSemesterId);
        
        note.setSource(rs.getString("source"));
        note.setUploadDate(rs.getString("uploadDate"));    // SQL column: uploadDate
        note.setFileType(rs.getString("fileType"));        // SQL column: fileType
        note.setFileName(rs.getString("fileName"));        // SQL column: fileName
        note.setFilePath(rs.getString("filePath"));        // SQL column: filePath
        note.setDescription(rs.getString("description"));
        note.setUserId(rs.getInt("userId"));               // SQL column: userId
        note.setPrivate(rs.getBoolean("isPrivate"));       // SQL column: isPrivate
        // Safely read status — column may not exist in older DB versions
        try { note.setStatus(rs.getString("status")); } catch (SQLException ignored) { note.setStatus("Pending"); }
        try { note.setTags(rs.getString("tags")); } catch (SQLException ignored) {}
        try {
            int dl = rs.getInt("downloads");
            if (!rs.wasNull()) note.setDownloads(dl);
        } catch (SQLException ignored) {}

        // Load user info (full name, department, semester)
        try {
            String fullName = rs.getString("fullName");
            if (fullName == null || fullName.isEmpty()) {
                fullName = rs.getString("name"); // Fallback to name field
            }
            note.setUserFullName(fullName != null ? fullName : "Unknown User");
        } catch (SQLException ignored) {
            note.setUserFullName("Unknown User");
        }

        try {
            note.setUserDepartment(rs.getString("department"));
        } catch (SQLException ignored) {
            note.setUserDepartment(null);
        }

        try {
            note.setUserSemester(rs.getString("semester"));
        } catch (SQLException ignored) {
            note.setUserSemester(null);
        }
        return note;
    }

    private static void setNullableInt(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null || value == 0) {
            stmt.setNull(index, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }

    private static void readNullableIntColumn(ResultSet rs, String column,
                                              java.util.function.Consumer<Integer> setter) {
        try {
            int val = rs.getInt(column);
            if (!rs.wasNull()) {
                setter.accept(val);
            }
        } catch (SQLException ignored) {
            // column may not exist before migration
        }
    }
}
