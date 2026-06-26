package com.studybuddy.services;

import com.studybuddy.models.Note;
import com.studybuddy.utils.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoteService {

    public List<Note> getNotesByUserId(int userId) throws SQLException {

        List<Note> notes = new ArrayList<>();

        String sql =
                "SELECT * FROM Notes WHERE userId=? ORDER BY uploadDate DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                notes.add(mapNote(rs));
            }
        }

        return notes;
    }

    public void createNote(Note note) throws SQLException {

        String sql =
                "INSERT INTO Notes(title,subject,source,uploadDate,fileType,fileName,filePath,description,userId,isPrivate) VALUES(?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, note.getTitle());
            stmt.setString(2, note.getSubject());
            stmt.setString(3, note.getSource());
            stmt.setString(4, note.getUploadDate());
            stmt.setString(5, note.getFileType());
            stmt.setString(6, note.getFileName());
            stmt.setString(7, note.getFilePath());
            stmt.setString(8, note.getDescription());
            stmt.setInt(9, note.getUserId());
            stmt.setBoolean(10, note.isPrivate());

            stmt.executeUpdate();
        }
    }

    public void deleteNote(int id) throws SQLException {

        String sql = "DELETE FROM Notes WHERE id=?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private Note mapNote(ResultSet rs) throws SQLException {

        Note note = new Note();

        note.setId(rs.getInt("id"));
        note.setTitle(rs.getString("title"));
        note.setSubject(rs.getString("subject"));
        note.setSource(rs.getString("source"));
        note.setUploadDate(rs.getString("uploadDate"));
        note.setFileType(rs.getString("fileType"));
        note.setFileName(rs.getString("fileName"));
        note.setFilePath(rs.getString("filePath"));
        note.setDescription(rs.getString("description"));
        note.setUserId(rs.getInt("userId"));
        note.setPrivate(rs.getBoolean("isPrivate"));

        return note;
    }
}