package com.studybuddy.services;

import com.studybuddy.models.Note;
import com.studybuddy.models.Category;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardService {

        private static final DashboardService INSTANCE = new DashboardService();
        private List<Note> allNotes;
        private List<Category> categories;

        public DashboardService() {
                initializeDummyData();
        }

        public static DashboardService getInstance() {
                return INSTANCE;
        }

        private void initializeDummyData() {
                // Initialize categories
                categories = new ArrayList<>();
                categories.add(new Category(1, "Engineering", "🏗️", "#2563EB", 245));
                categories.add(new Category(2, "Computer Science", "💻", "#10B981", 389));
                categories.add(new Category(3, "Mathematics", "📐", "#F59E0B", 178));
                categories.add(new Category(4, "Physics", "⚛️", "#EF4444", 203));
                categories.add(new Category(5, "Chemistry", "🧪", "#8B5CF6", 156));
                categories.add(new Category(6, "Civil Engineering", "🏢", "#06B6D4", 134));
                categories.add(new Category(7, "Electrical Engineering", "⚡", "#EC4899", 167));
                categories.add(new Category(8, "Mechanical", "⚙️", "#6366F1", 198));
                categories.add(new Category(9, "Architecture", "🏛️", "#14B8A6", 89));
                categories.add(new Category(10, "Biology", "🧬", "#22C55E", 145));
                categories.add(new Category(11, "Economics", "📊", "#F97316", 223));
                categories.add(new Category(12, "Business", "💼", "#0EA5E9", 312));
                categories.add(new Category(13, "Programming", "📝", "#A855F7", 456));
                categories.add(new Category(14, "Medical", "⚕️", "#EF4444", 178));
                categories.add(new Category(15, "Others", "📚", "#9CA3AF", 267));

                // Initialize dummy notes (ONLY APPROVED notes - using your Note model)
                allNotes = new ArrayList<>();

                allNotes.add(new Note(
                                1,
                                "Complete Physics Notes - Class 12",
                                "Physics",
                                "Tripos University",
                                "2026-05-15",
                                "PDF",
                                "physics_class12.pdf",
                                "",
                                "Full chapter-wise notes covering all topics for Class 12 Physics",
                                101,
                                false));
                allNotes.add(new Note(
                                2,
                                "Computer Science Final Exam Prep",
                                "Computer Science",
                                "Tech Institute",
                                "2026-05-20",
                                "PDF",
                                "cs_final_prep.pdf",
                                "",
                                "Comprehensive CS notes for final examination preparation",
                                102,
                                false));
                allNotes.add(new Note(
                                3,
                                "Engineering Mathematics Formula Sheet",
                                "Mathematics",
                                "Tripos University",
                                "2026-06-01",
                                "PDF",
                                "math_formula_sheet.pdf",
                                "",
                                "Quick reference formula sheet for all engineering math topics",
                                103,
                                false));

                allNotes.add(new Note(
                                4,
                                "Organic Chemistry Complete Guide",
                                "Chemistry",
                                "Science College",
                                "2026-05-25",
                                "PDF",
                                "organic_chemistry_guide.pdf",
                                "",
                                "Detailed notes on organic chemistry reactions and mechanisms",
                                104,
                                false));

                allNotes.add(new Note(
                                5,
                                "Civil Engineering Structural Analysis",
                                "Civil Engineering",
                                "Engineering University",
                                "2026-06-05",
                                "PDF",
                                "structural_analysis.pdf",
                                "",
                                "Complete structural analysis notes with examples",
                                105,
                                false));

                allNotes.add(new Note(
                                6,
                                "Electrical Circuits and Networks",
                                "Electrical Engineering",
                                "Tech Institute",
                                "2026-05-30",
                                "PDF",
                                "circuits_networks.pdf",
                                "",
                                "Full notes on electrical circuit theory and network analysis",
                                106,
                                false));

                allNotes.add(new Note(
                                7,
                                "Mechanical Engineering Thermodynamics",
                                "Mechanical",
                                "Engineering University",
                                "2026-06-10",
                                "PDF",
                                "thermodynamics.pdf",
                                "",
                                "Thermodynamics principles and applications in mechanical engineering",
                                107,
                                false));

                allNotes.add(new Note(
                                8,
                                "Architecture Design Principles",
                                "Architecture",
                                "Design College",
                                "2026-06-03",
                                "PDF",
                                "design_principles.pdf",
                                "",
                                "Fundamental design principles for architecture students",
                                108,
                                false));
                allNotes.add(new Note(
                                9,
                                "Biology Cell Structure Notes",
                                "Biology",
                                "Science College",
                                "2026-05-28",
                                "PDF",
                                "cell_structure.pdf",
                                "",
                                "Detailed notes on cell biology and structure",
                                109,
                                false));
                allNotes.add(new Note(
                                10,
                                "Economics Micro and Macro Theory",
                                "Economics",
                                "Business University",
                                "2026-06-08",
                                "PDF",
                                "economics_theory.pdf",
                                "",
                                "Complete economics notes covering micro and macro economics",
                                110,
                                false));

                allNotes.add(new Note(
                                11,
                                "Business Management Strategies",
                                "Business",
                                "Business University",
                                "2026-06-12",
                                "PDF",
                                "management_strategies.pdf",
                                "",
                                "Modern business management techniques and strategies",
                                111,
                                false));

                allNotes.add(new Note(
                                12,
                                "Java Programming Complete Course",
                                "Programming",
                                "Tech Institute",
                                "2026-06-15",
                                "PDF",
                                "java_complete_course.pdf",
                                "",
                                "Full Java programming course from basics to advanced",
                                112,
                                false));

                // Add a private note (should still appear if approved - you can filter by
                // isPrivate later)
                allNotes.add(new Note(
                                13,
                                "Private Study Notes",
                                "Mathematics",
                                "Tripos University",
                                "2026-06-14",
                                "PDF",
                                "private_notes.pdf",
                                "",
                                "Personal private study notes",
                                113,
                                true));
        }

        // Return all notes (you can add approval logic later in database)
        public List<Note> getApprovedNotes() {
                // Placeholder: For now, return all notes
                // Later: WHERE status = 'Approved' in MSSQL
                return allNotes.stream()
                                .filter(note -> !note.isPrivate()) // Optionally filter private notes
                                .collect(Collectors.toList());
        }

        public List<Note> getFeaturedNotes() {
                // Placeholder: Implement featured logic later in database
                return allNotes.stream()
                                .filter(note -> !note.isPrivate())
                                .limit(3)
                                .collect(Collectors.toList());
        }

        public List<Note> getTrendingSubjects() {
                return allNotes.stream()
                                .filter(note -> !note.isPrivate())
                                .collect(Collectors.groupingBy(Note::getSubject, Collectors.counting()))
                                .entrySet().stream()
                                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                                .limit(5)
                                .map(e -> {
                                        return allNotes.stream()
                                                        .filter(n -> n.getSubject().equals(e.getKey()))
                                                        .findFirst()
                                                        .orElse(null);
                                })
                                .filter(n -> n != null)
                                .collect(Collectors.toList());
        }

        public List<Note> getRecentNotes() {
                return allNotes.stream()
                                .filter(note -> !note.isPrivate())
                                .sorted((n1, n2) -> n2.getUploadDate().compareTo(n1.getUploadDate()))
                                .limit(5)
                                .collect(Collectors.toList());
        }

        public List<Category> getCategories() {
                return categories;
        }

        public List<Note> searchNotes(String query) {
                if (query == null || query.trim().isEmpty()) {
                        return getApprovedNotes();
                }
                String lowerQuery = query.toLowerCase();
                return allNotes.stream()
                                .filter(note -> !note.isPrivate() &&
                                                (note.getTitle().toLowerCase().contains(lowerQuery) ||
                                                                note.getSubject().toLowerCase().contains(lowerQuery) ||
                                                                note.getSource().toLowerCase().contains(lowerQuery)))
                                .collect(Collectors.toList());
        }

        public List<Note> filterNotes(String subject, String semester, String university) {
                return allNotes.stream()
                                .filter(note -> !note.isPrivate() &&
                                                (subject == null || subject.isEmpty()
                                                                || note.getSubject().equals(subject))
                                                &&
                                                (semester == null || semester.isEmpty()
                                                                || semester.equals(note.getUploadDate()))
                                                &&
                                                (university == null || university.isEmpty()
                                                                || note.getSource().equals(university)))
                                .collect(Collectors.toList());
        }

        public List<Note> sortNotes(List<Note> notes, String sortBy) {
                switch (sortBy) {
                        case "Most Popular":
                                return notes; // Placeholder - implement view count logic later
                        case "Most Downloaded":
                                return notes; // Placeholder - implement download count later
                        case "Highest Rated":
                                return notes; // Placeholder - implement rating later
                        case "Newest":
                                return notes.stream()
                                                .sorted((n1, n2) -> n2.getUploadDate().compareTo(n1.getUploadDate()))
                                                .collect(Collectors.toList());
                        case "Oldest":
                                return notes.stream()
                                                .sorted((n1, n2) -> n1.getUploadDate().compareTo(n2.getUploadDate()))
                                                .collect(Collectors.toList());
                        default:
                                return notes;
                }
        }

        public List<Note> getNotesByCategory(Category category) {
                // Map category to subject (placeholder mapping)
                return allNotes.stream()
                                .filter(note -> !note.isPrivate() && note.getSubject().equals(category.getName()))
                                .collect(Collectors.toList());
        }

        public List<Note> getNotesBySubject(String subject) {
                return allNotes.stream()
                                .filter(note -> !note.isPrivate() && note.getSubject().equals(subject))
                                .collect(Collectors.toList());
        }

        public List<Note> getNotesByUniversity(String university) {
                return allNotes.stream()
                                .filter(note -> !note.isPrivate() && note.getSource().equals(university))
                                .collect(Collectors.toList());
        }
}