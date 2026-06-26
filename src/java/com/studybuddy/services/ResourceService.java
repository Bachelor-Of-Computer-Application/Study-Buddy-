package com.studybuddy.services;

import com.studybuddy.models.Note;
import com.studybuddy.models.Resource;
import com.studybuddy.utils.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResourceService {

    public void shareAsResource(Note note, String filePath) throws SQLException {

        String sql = """
            INSERT INTO Resources
            (
                noteId,
                uploadedBy,
                title,
                subject,
                source,
                description,
                uploadDate,
                filePath,
                fileType,
                downloads,
                isActive
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 1)
            """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (note.getId() <= 0) {
                stmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                if (note.getId() <= 0) {
                    stmt.setNull(1, java.sql.Types.INTEGER);
                } else {
                    stmt.setInt(1, note.getId());
                }
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

        List<Resource> list = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Resource r = new Resource();

                r.setId(rs.getInt("id"));
                r.setNoteId(rs.getInt("noteId"));
                r.setUploadedBy(rs.getInt("uploadedBy"));
                r.setTitle(rs.getString("title"));
                r.setSubject(rs.getString("subject"));
                r.setSource(rs.getString("source"));
                r.setDescription(rs.getString("description"));
                r.setUploadDate(rs.getString("uploadDate"));
                r.setFilePath(rs.getString("filePath"));
                r.setFileType(rs.getString("fileType"));

                list.add(r);
            }
        }

        return list;
    }

    public boolean deleteResource(int id) throws SQLException {

        String sql = "UPDATE Resources SET isActive = 0 WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            return stmt.executeUpdate() > 0;
        }
    }

    public int countActiveResources() throws SQLException {

        String sql = "SELECT COUNT(*) FROM Resources WHERE isActive = 1";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    public List<Resource> getResourcesByUser(int userId) throws SQLException {

        String sql = """
            SELECT *
            FROM Resources
            WHERE uploadedBy = ?
            ORDER BY uploadDate DESC
            """;

        List<Resource> list = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    Resource r = new Resource();

                    r.setId(rs.getInt("id"));
                    r.setNoteId(rs.getInt("noteId"));
                    r.setUploadedBy(rs.getInt("uploadedBy"));
                    r.setTitle(rs.getString("title"));
                    r.setSubject(rs.getString("subject"));
                    r.setSource(rs.getString("source"));
                    r.setDescription(rs.getString("description"));
                    r.setUploadDate(rs.getString("uploadDate"));
                    r.setFilePath(rs.getString("filePath"));
                    r.setFileType(rs.getString("fileType"));

                    list.add(r);
                }
            }
        }

        return list;
    }
}