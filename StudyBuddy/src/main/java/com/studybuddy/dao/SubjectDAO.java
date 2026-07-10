package com.studybuddy.dao;

import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Subjects table.
 */
public class SubjectDAO {

    private static final String SUBJECT_SELECT =
            "SELECT s.id, s.semesterId, s.name, s.code, s.description, " +
            "s.credits, s.isActive, s.created_at, " +
            "sem.name AS semesterName, sem.semesterNumber, " +
            "d.id AS departmentId, d.name AS departmentName, d.code AS departmentCode " +
            "FROM Subjects s " +
            "INNER JOIN Semesters sem ON s.semesterId = sem.id " +
            "INNER JOIN Departments d ON sem.departmentId = d.id ";

    public List<Subject> getSubjectsBySemester(int semesterId) {
        return getSubjectsByDepartmentAndSemester(null, semesterId);
    }

    public List<Subject> getSubjectsByDepartment(int departmentId) {
        List<Subject> subjects = new ArrayList<>();
        String query = SUBJECT_SELECT +
                       "WHERE s.isActive = 1 AND sem.departmentId = ? " +
                       "ORDER BY sem.semesterNumber, s.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, departmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    subjects.add(extractSubjectFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching subjects by department: " + e.getMessage());
            e.printStackTrace();
        }

        return subjects;
    }

    public List<Subject> getSubjectsBySemesterNumber(int semesterNumber) {
        List<Subject> subjects = new ArrayList<>();
        String query = SUBJECT_SELECT +
                       "WHERE s.isActive = 1 AND sem.semesterNumber = ? " +
                       "ORDER BY d.name, s.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, semesterNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    subjects.add(extractSubjectFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching subjects by semester number: " + e.getMessage());
            e.printStackTrace();
        }

        return subjects;
    }

    public List<Subject> getSubjectsByDepartmentAndSemester(Integer departmentId, Integer semesterId) {
        List<Subject> subjects = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        conditions.add("s.isActive = 1");

        if (departmentId != null) {
            conditions.add("sem.departmentId = ?");
            params.add(departmentId);
        }

        if (semesterId != null) {
            Semester sem = new SemesterDAO().getSemesterById(semesterId);
            if (sem != null) {
                if (departmentId != null) {
                    conditions.add("s.semesterId = ?");
                    params.add(semesterId);
                } else {
                    conditions.add("sem.semesterNumber = ?");
                    params.add(sem.getSemesterNumber());
                }
            }
        }

        String query = SUBJECT_SELECT + " WHERE " + String.join(" AND ", conditions) + " ORDER BY d.name, sem.semesterNumber, s.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (int i = 0; i < params.size(); i++) {
                if (params.get(i) instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) params.get(i));
                } else if (params.get(i) instanceof String) {
                    stmt.setString(i + 1, (String) params.get(i));
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    subjects.add(extractSubjectFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching subjects with filters: " + e.getMessage());
            e.printStackTrace();
        }

        return subjects;
    }

    public Subject getSubjectById(int id) {
        String query = "SELECT s.id, s.semesterId, s.name, s.code, s.description, " +
                       "s.credits, s.isActive, s.created_at, " +
                       "sem.name AS semesterName, sem.semesterNumber, " +
                       "d.id AS departmentId, d.name AS departmentName, d.code AS departmentCode " +
                       "FROM Subjects s " +
                       "INNER JOIN Semesters sem ON s.semesterId = sem.id " +
                       "INNER JOIN Departments d ON sem.departmentId = d.id " +
                       "WHERE s.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractSubjectFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching subject by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public List<Subject> searchSubjectsByName(String searchTerm) {
        List<Subject> subjects = new ArrayList<>();
        String query = "SELECT s.id, s.semesterId, s.name, s.code, s.description, " +
                       "s.credits, s.isActive, s.created_at, " +
                       "sem.name AS semesterName, sem.semesterNumber, " +
                       "d.id AS departmentId, d.name AS departmentName, d.code AS departmentCode " +
                       "FROM Subjects s " +
                       "INNER JOIN Semesters sem ON s.semesterId = sem.id " +
                       "INNER JOIN Departments d ON sem.departmentId = d.id " +
                       "WHERE s.isActive = 1 AND (s.name LIKE ? OR s.code LIKE ?) " +
                       "ORDER BY s.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            String pattern = "%" + searchTerm + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Subject subject = extractSubjectFromResultSet(rs);
                subjects.add(subject);
            }

        } catch (SQLException e) {
            System.err.println("Error searching subjects: " + e.getMessage());
            e.printStackTrace();
        }

        return subjects;
    }

    /**
     * Returns all active subjects ordered by name — used by NoteService and
     * other services that need a flat subject list without filtering by semester.
     */
    public List<Subject> getAllActiveSubjects() {
        List<Subject> subjects = new ArrayList<>();
        String query = SUBJECT_SELECT + "WHERE s.isActive = 1 ORDER BY d.name, sem.semesterNumber, s.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                subjects.add(extractSubjectFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching all active subjects: " + e.getMessage());
            e.printStackTrace();
        }

        return subjects;
    }



    private Subject extractSubjectFromResultSet(ResultSet rs) throws SQLException {
        Subject subject = new Subject();
        subject.setId(rs.getInt("id"));
        subject.setSemesterId(rs.getInt("semesterId"));
        subject.setName(rs.getString("name"));
        subject.setCode(rs.getString("code"));
        subject.setDescription(rs.getString("description"));
        subject.setCredits(rs.getInt("credits"));
        subject.setActive(rs.getBoolean("isActive"));
        subject.setSemesterName(rs.getString("semesterName"));
        subject.setSemesterNumber(rs.getInt("semesterNumber"));
        subject.setDepartmentId(rs.getInt("departmentId"));
        subject.setDepartmentName(rs.getString("departmentName"));
        subject.setDepartmentCode(rs.getString("departmentCode"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            subject.setCreatedAt(ts.toLocalDateTime());
        }

        return subject;
    }
}
