package com.studybuddy.services;

import com.studybuddy.dao.NoteDAO;
import com.studybuddy.models.Note;

import java.sql.SQLException;
import java.util.List;

public class NoteService {

    private final NoteDAO noteDAO = new NoteDAO();

    public List<Note> getNotesByUserId(int userId) throws SQLException {
        return noteDAO.getNotesByUserId(userId);
    }

    public List<Note> getApprovedNotes() throws SQLException {
        return noteDAO.getApprovedNotes();
    }

    public List<Note> getFeaturedNotes() throws SQLException {
        return noteDAO.getFeaturedNotes();
    }

    public List<Note> getRecentNotes(int limit) throws SQLException {
        return noteDAO.getRecentNotes(limit);
    }

    public List<Note> searchApprovedNotes(String query, String subject) throws SQLException {
        return noteDAO.searchApprovedNotes(query, subject);
    }

    public void createNote(Note note) throws SQLException {
        noteDAO.createNote(note);
    }

    public void updateNoteStatus(String noteId, String status) throws SQLException {
        noteDAO.updateNoteStatus(noteId, status);
    }

    public void deleteNote(String id) throws SQLException {
        noteDAO.deleteNote(id);
    }

    public int countNotesByUser(int userId) throws SQLException {
        return noteDAO.countNotesByUser(userId);
    }
}
