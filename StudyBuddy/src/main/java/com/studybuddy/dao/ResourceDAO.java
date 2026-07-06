package com.studybuddy.dao;

import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResourceDAO {

    public void shareAsResource(Note note, String filePath) throws SQLException {
        String sql = """
                INSERT INTO Resources
                (noteId, uploadedBy, title, subject, source, description, uploadDate, filePath, fileType, downloads, isActive)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)
                """;

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
            stmt.setString(5, note.getSource());
            stmt.setString(6, note.getDescription());
            stmt.setString(7, note.getUploadDate());
            stmt.setString(8, filePath);
            stmt.setString(9, note.getFileType());
            stmt.executeUpdate();
        }
    }

    public List<Resource> getAllActiveResources() throws SQLException {
        String sql = """
                SELECT *
                FROM Resources
                WHERE isActive = 1
                ORDER BY uploadDate DESC
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

    public List<Resource> getResourcesByUser(int userId) throws SQLException {
        String sql = """
                SELECT *
                FROM Resources
                WHERE uploadedBy = ?
                ORDER BY uploadDate DESC
                """;
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

    public boolean deleteResource(int id) throws SQLException {
        String sql = "UPDATE Resources SET isActive = 0 WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public int countActiveResources() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Resources WHERE isActive = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

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

    private Resource mapResource(ResultSet rs) throws SQLException {
        Resource resource = new Resource();
        resource.setId(rs.getInt("id"));
        int noteIdVal = rs.getInt("noteId");
        resource.setNoteId(rs.wasNull() ? null : noteIdVal);
        resource.setUploadedBy(rs.getInt("uploadedBy"));
        resource.setTitle(rs.getString("title"));
        resource.setSubject(rs.getString("subject"));
        resource.setSource(rs.getString("source"));
        resource.setDescription(rs.getString("description"));
        resource.setUploadDate(rs.getString("uploadDate"));
        resource.setFilePath(rs.getString("filePath"));
        resource.setFileType(rs.getString("fileType"));
        resource.setDownloads(rs.getInt("downloads"));
        resource.setActive(rs.getBoolean("isActive"));
        return resource;
    }
}
