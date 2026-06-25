package com.studybuddy.dao;

import com.studybuddy.models.Note;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NoteDAO {

    public List<Note> getNotesByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM Notes WHERE user_id = ? ORDER BY upload_date DESC";
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

    public List<Note> getApprovedNotes() throws SQLException {
        String sql = "SELECT * FROM Notes WHERE status = 'Approved' AND is_private = 0 ORDER BY upload_date DESC";
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

    public List<Note> getFeaturedNotes() throws SQLException {
        String sql = """
                SELECT TOP 5 *
                FROM Notes
                WHERE status = 'Approved' AND is_featured = 1 AND is_private = 0
                ORDER BY upload_date DESC
                """;
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

    public List<Note> getRecentNotes(int limit) throws SQLException {
        String sql = """
                SELECT *
                FROM Notes
                WHERE status = 'Approved' AND is_private = 0
                ORDER BY upload_date DESC
                OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY
                """;
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

    public List<Note> searchApprovedNotes(String query, String subject) throws SQLException {
        String sql = """
                SELECT *
                FROM Notes
                WHERE status = 'Approved'
                  AND is_private = 0
                  AND (? IS NULL OR title LIKE ? OR subject LIKE ? OR source LIKE ?)
                  AND (? IS NULL OR subject = ?)
                ORDER BY upload_date DESC
                """;
        String normalizedQuery = query == null || query.trim().isEmpty() ? null : "%" + query.trim() + "%";
        String normalizedSubject = subject == null || subject.trim().isEmpty() ? null : subject.trim();
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

    public void createNote(Note note) throws SQLException {
        String sql = """
                INSERT INTO Notes
                (id, title, subject, source, upload_date, file_type, file_name, description, user_id, is_private, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending')
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, note.getId());
            stmt.setString(2, note.getTitle());
            stmt.setString(3, note.getSubject());
            stmt.setString(4, note.getSource());
            stmt.setString(5, note.getUploadDate());
            stmt.setString(6, note.getFileType());
            stmt.setString(7, note.getFileName());
            stmt.setString(8, note.getDescription());
            stmt.setInt(9, Integer.parseInt(note.getUserId()));
            stmt.setBoolean(10, note.isPrivate());
            stmt.executeUpdate();
        }
    }

    public void updateNoteStatus(String noteId, String status) throws SQLException {
        String sql = "UPDATE Notes SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, noteId);
            stmt.executeUpdate();
        }
    }

    public void deleteNote(String id) throws SQLException {
        String sql = "DELETE FROM Notes WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        }
    }

    public int countNotesByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Notes WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Note mapNote(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getString("id"));
        note.setTitle(rs.getString("title"));
        note.setSubject(rs.getString("subject"));
        note.setSource(rs.getString("source"));
        note.setUploadDate(rs.getString("upload_date"));
        note.setFileType(rs.getString("file_type"));
        note.setFileName(rs.getString("file_name"));
        note.setDescription(rs.getString("description"));
        note.setUserId(String.valueOf(rs.getInt("user_id")));
        note.setPrivate(rs.getBoolean("is_private"));
        return note;
    }
}
