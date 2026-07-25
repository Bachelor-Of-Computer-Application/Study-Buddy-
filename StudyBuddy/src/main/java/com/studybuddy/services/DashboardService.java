package com.studybuddy.services;

import com.studybuddy.dao.NoteDAO;
import com.studybuddy.models.Category;
import com.studybuddy.models.Note;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Dashboard-level data operations.
 *
 * FIXED: Removed all hardcoded dummy note data.
 * All note data now comes from the SQL Server database via NoteDAO.
 *
 * Categories remain as a static in-memory list since there is no
 * Categories table in the SQL schema.
 */
public class DashboardService {

    private static final DashboardService INSTANCE = new DashboardService();

    // NoteDAO is used to fetch real data from the Notes table
    private final NoteDAO noteDAO = new NoteDAO();

    // Categories are static display data — no SQL table exists for them
    private final List<Category> categories;

    public DashboardService() {
        categories = buildCategories();
    }

    public static DashboardService getInstance() {
        return INSTANCE;
    }

    // =========================
    // GET APPROVED / PUBLIC NOTES
    // =========================

    /**
     * Returns all non-private notes from the database.
     * FIXED: Was returning hardcoded dummy list. Now queries SQL Server.
     * SQL: SELECT * FROM Notes WHERE isPrivate = 0 ORDER BY uploadDate DESC
     */
    public List<Note> getApprovedNotes() {
        try {
            return noteDAO.getAllPublicNotes();
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(DashboardService.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // =========================
    // GET RECENT NOTES
    // =========================

    /**
     * Returns the 5 most recently uploaded public notes.
     * FIXED: Was returning hardcoded dummy list. Now queries SQL Server.
     * SQL: SELECT TOP 5 * FROM Notes WHERE isPrivate = 0 ORDER BY uploadDate DESC
     */
    public List<Note> getRecentNotes() {
        try {
            return noteDAO.getRecentNotes(5);
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(DashboardService.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // =========================
    // GET FEATURED NOTES
    // =========================

    /**
     * Returns the 3 most recently uploaded public notes as "featured".
     * (No featured column in Notes table — returns most recent 3 instead.)
     */
    public List<Note> getFeaturedNotes() {
        try {
            return noteDAO.getRecentNotes(3);
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(DashboardService.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // =========================
    // SEARCH NOTES
    // =========================

    /**
     * Searches public notes by keyword (title, subject, source).
     * FIXED: Was filtering an in-memory hardcoded list. Now queries SQL Server.
     */
    public List<Note> searchNotes(String query) {
        try {
            return noteDAO.searchPublicNotes(query, null);
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(DashboardService.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // =========================
    // FILTER NOTES
    // =========================

    /**
     * Filters approved notes by subject, semester (uploadDate), and university (source).
     * Fetches from DB then filters in memory for flexibility.
     */
    public List<Note> filterNotes(String subject, String semester, String university) {
        List<Note> all = getApprovedNotes();
        return all.stream()
                .filter(note ->
                        (subject == null || subject.isEmpty() || note.getSubject().equals(subject)) &&
                        (semester == null || semester.isEmpty() || semester.equals(note.getUploadDate())) &&
                        (university == null || university.isEmpty() || note.getSource().equals(university)))
                .collect(Collectors.toList());
    }

    // =========================
    // SORT NOTES
    // =========================

    /**
     * Sorts a list of notes by the given sort option.
     */
    public List<Note> sortNotes(List<Note> notes, String sortBy) {
        if (notes == null) return new ArrayList<>();
        switch (sortBy) {
            case "Newest":
                return notes.stream()
                        .sorted((n1, n2) -> n2.getUploadDate().compareTo(n1.getUploadDate()))
                        .collect(Collectors.toList());
            case "Oldest":
                return notes.stream()
                        .sorted((n1, n2) -> n1.getUploadDate().compareTo(n2.getUploadDate()))
                        .collect(Collectors.toList());
            default:
                // Most Popular, Most Downloaded, Highest Rated — return as-is
                return notes;
        }
    }

    // =========================
    // GET TRENDING SUBJECTS
    // =========================

    /**
     * Returns notes representing the most common subjects.
     */
    public List<Note> getTrendingSubjects() {
        List<Note> all = getApprovedNotes();
        return all.stream()
                .collect(Collectors.groupingBy(Note::getSubject, Collectors.counting()))
                .entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .map(e -> all.stream()
                        .filter(n -> n.getSubject().equals(e.getKey()))
                        .findFirst()
                        .orElse(null))
                .filter(n -> n != null)
                .collect(Collectors.toList());
    }

    // =========================
    // GET NOTES BY CATEGORY / SUBJECT / UNIVERSITY
    // =========================

    public List<Note> getNotesByCategory(Category category) {
        return getNotesBySubject(category.getName());
    }

    public List<Note> getNotesBySubject(String subject) {
        List<Note> all = getApprovedNotes();
        return all.stream()
                .filter(note -> note.getSubject().equals(subject))
                .collect(Collectors.toList());
    }

    public List<Note> getNotesByUniversity(String university) {
        List<Note> all = getApprovedNotes();
        return all.stream()
                .filter(note -> note.getSource().equals(university))
                .collect(Collectors.toList());
    }

    // =========================
    // GET CATEGORIES (Static Display Data)
    // =========================

    /**
     * Returns the list of study categories.
     * These are UI display items — no Categories table exists in SQL.
     */
    public List<Category> getCategories() {
        return categories;
    }

    // =========================
    // PRIVATE HELPER
    // =========================

    private List<Category> buildCategories() {
        List<Category> list = new ArrayList<>();
        list.add(new Category(1,  "Engineering",             "🏗️", "#2563EB", 245));
        list.add(new Category(2,  "Computer Science",        "💻", "#10B981", 389));
        list.add(new Category(3,  "Mathematics",             "📐", "#F59E0B", 178));
        list.add(new Category(4,  "Physics",                 "⚛️", "#EF4444", 203));
        list.add(new Category(5,  "Chemistry",               "🧪", "#8B5CF6", 156));
        list.add(new Category(6,  "Civil Engineering",       "🏢", "#06B6D4", 134));
        list.add(new Category(7,  "Electrical Engineering",  "⚡", "#EC4899", 167));
        list.add(new Category(8,  "Mechanical",              "⚙️", "#6366F1", 198));
        list.add(new Category(9,  "Architecture",            "🏛️", "#14B8A6",  89));
        list.add(new Category(10, "Biology",                 "🧬", "#22C55E", 145));
        list.add(new Category(11, "Economics",               "📊", "#F97316", 223));
        list.add(new Category(12, "Business",                "💼", "#0EA5E9", 312));
        list.add(new Category(13, "Programming",             "📝", "#A855F7", 456));
        list.add(new Category(14, "Medical",                 "⚕️", "#EF4444", 178));
        list.add(new Category(16, "Software Engineering",   "🚀", "#3B82F6", 298));
        list.add(new Category(17, "Management",              "📈", "#F43F5E", 185));
        list.add(new Category(15, "Others",                  "📚", "#9CA3AF", 267));
        return list;
    }
}