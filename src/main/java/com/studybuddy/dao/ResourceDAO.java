package com.studybuddy.dao;

import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResourceDAO {

    public void shareAsResource(Note note, String filePath) throws SQLException {
        String fileSql = """
                INSERT INTO UploadedFiles
                (id, file_name, file_path, file_type, uploaded_by, upload_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        String resourceSql = """
                INSERT INTO Resources
                (id, note_id, title, subject, source, description, file_id, uploaded_by, upload_date, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """;

        Connection conn = DatabaseConnection.getConnection();
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            String fileId = UUID.randomUUID().toString();

            try (PreparedStatement stmt = conn.prepareStatement(fileSql)) {
                stmt.setString(1, fileId);
                stmt.setString(2, note.getFileName());
                stmt.setString(3, filePath);
                stmt.setString(4, note.getFileType());
                stmt.setInt(5, Integer.parseInt(note.getUserId()));
                stmt.setString(6, note.getUploadDate());
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(resourceSql)) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, note.getId());
                stmt.setString(3, note.getTitle());
                stmt.setString(4, note.getSubject());
                stmt.setString(5, note.getSource());
                stmt.setString(6, note.getDescription());
                stmt.setString(7, fileId);
                stmt.setInt(8, Integer.parseInt(note.getUserId()));
                stmt.setString(9, note.getUploadDate());
                stmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    public List<Resource> getAllActiveResources() throws SQLException {
        String sql = """
                SELECT r.*, uf.file_path, uf.file_type
                FROM Resources r
                JOIN UploadedFiles uf ON r.file_id = uf.id
                WHERE r.is_active = 1
                ORDER BY r.upload_date DESC
                """;
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

    public List<Resource> getResourcesByUser(String userId) throws SQLException {
        String sql = """
                SELECT r.*, uf.file_path, uf.file_type
                FROM Resources r
                JOIN UploadedFiles uf ON r.file_id = uf.id
                WHERE r.uploaded_by = ?
                ORDER BY r.upload_date DESC
                """;
        List<Resource> resources = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(userId));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapResource(rs));
                }
            }
        }

        return resources;
    }

    public boolean deleteResource(String id) throws SQLException {
        String sql = "UPDATE Resources SET is_active = 0 WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public int countActiveResources() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Resources WHERE is_active = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countResourcesByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Resources WHERE uploaded_by = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Resource mapResource(ResultSet rs) throws SQLException {
        Resource resource = new Resource();
        resource.setId(rs.getString("id"));
        resource.setNoteId(rs.getString("note_id"));
        resource.setTitle(rs.getString("title"));
        resource.setSubject(rs.getString("subject"));
        resource.setSource(rs.getString("source"));
        resource.setDescription(rs.getString("description"));
        resource.setFileId(rs.getString("file_id"));
        resource.setUploadedBy(String.valueOf(rs.getInt("uploaded_by")));
        resource.setUploadDate(rs.getString("upload_date"));
        resource.setIsActive(rs.getBoolean("is_active"));
        resource.setFilePath(rs.getString("file_path"));
        resource.setFileType(rs.getString("file_type"));
        return resource;
    }
}
