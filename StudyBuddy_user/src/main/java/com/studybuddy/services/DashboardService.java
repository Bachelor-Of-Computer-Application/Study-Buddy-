package com.studybuddy.services;

import com.studybuddy.models.Note;

import java.sql.SQLException;
import java.util.List;

public class DashboardService {

    private static final DashboardService INSTANCE = new DashboardService();
    private final NoteService noteService = new NoteService();

    public static DashboardService getInstance() {
        return INSTANCE;
    }

    public List<Note> getApprovedNotes() throws SQLException {
        return noteService.getApprovedNotes();
    }

    public List<Note> getFeaturedNotes() throws SQLException {
        return noteService.getFeaturedNotes();
    }

    public List<Note> getRecentNotes() throws SQLException {
        return noteService.getRecentNotes(5);
    }

    public List<Note> searchNotes(String query, String subject) throws SQLException {
        return noteService.searchApprovedNotes(query, subject);
    }

    public List<Note> sortNotes(List<Note> notes, String sortBy) {
        if (notes == null) {
            return List.of();
        }

        return switch (sortBy) {
            case "Newest" -> notes.stream()
                    .sorted((n1, n2) -> n2.getUploadDate().compareTo(n1.getUploadDate()))
                    .toList();
            case "Oldest" -> notes.stream()
                    .sorted((n1, n2) -> n1.getUploadDate().compareTo(n2.getUploadDate()))
                    .toList();
            default -> notes;
        };
    }
}
