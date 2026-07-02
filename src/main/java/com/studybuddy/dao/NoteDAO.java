package com.studybuddy.dao;

import com.studybuddy.models.Note;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Note operations.
 * All SQL column names match the actual Notes table schema:
 *   id, userId, title, subject, source, uploadDate, fileType,
 *   fileName, description, isPrivate, filePath
 */
public class NoteDAO {

    // =========================
    // GET NOTES BY USER ID
    // =========================

    /**
     * Returns all notes belonging to the given user.
     * SQL: SELECT * FROM Notes WHERE userId = ? ORDER BY uploadDate DESC
     */
    public List<Note> getNotesByUserId(int userId) throws SQLException {
        // Uses actual SQL column name: userId (not user_id)
        String sql = "SELECT * FROM Notes WHERE userId = ? ORDER BY uploadDate DESC";
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Note note = mapNote(rs);

                    System.out.println(
                            "Loaded Note: " +
                                    note.getTitle() +
                                    " | UserID=" + note.getUserId()
                    );

                    notes.add(note);
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
        String sql = "SELECT * FROM Notes WHERE isPrivate = 0 ORDER BY uploadDate DESC";
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
        String sql = "SELECT TOP (?) * FROM Notes WHERE isPrivate = 0 ORDER BY uploadDate DESC";
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
        String sql = "SELECT * FROM Notes " +
                "WHERE isPrivate = 0 " +
                "  AND (? IS NULL OR title LIKE ? OR subject LIKE ? OR source LIKE ?) " +
                "  AND (? IS NULL OR subject = ?) " +
                "ORDER BY uploadDate DESC";

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
     * SQL columns: title, subject, source, uploadDate, fileType, fileName,
     *              filePath, description, userId, isPrivate
     */
    public void createNote(Note note) throws SQLException {
        // id is excluded — SQL Server auto-generates it via IDENTITY(1,1)
        // All column names match exact SQL schema
        String sql = "INSERT INTO Notes " +
                "(title, subject, source, uploadDate, fileType, fileName, filePath, description, userId, isPrivate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1,  note.getTitle());
            stmt.setString(2,  note.getSubject());
            stmt.setString(3,  note.getSource());
            stmt.setString(4,  note.getUploadDate());    // SQL column: uploadDate
            stmt.setString(5,  note.getFileType());      // SQL column: fileType
            stmt.setString(6,  note.getFileName());      // SQL column: fileName
            stmt.setString(7,  note.getFilePath());      // SQL column: filePath
            stmt.setString(8,  note.getDescription());
            stmt.setInt(9,     note.getUserId());         // SQL column: userId
            stmt.setBoolean(10, note.isPrivate());        // SQL column: isPrivate
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
        note.setSource(rs.getString("source"));
        note.setUploadDate(rs.getString("uploadDate"));    // SQL column: uploadDate
        note.setFileType(rs.getString("fileType"));        // SQL column: fileType
        note.setFileName(rs.getString("fileName"));        // SQL column: fileName
        note.setFilePath(rs.getString("filePath"));        // SQL column: filePath
        note.setDescription(rs.getString("description"));
        note.setUserId(rs.getInt("userId"));               // SQL column: userId
        note.setPrivate(rs.getBoolean("isPrivate"));       // SQL column: isPrivate
        return note;
    }
}
