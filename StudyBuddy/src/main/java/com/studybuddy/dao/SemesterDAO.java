package com.studybuddy.dao;

import com.studybuddy.models.Semester;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Semesters table.
 */
public class SemesterDAO {

    public List<Semester> getSemestersByDepartment(int departmentId) {
        List<Semester> semesters = new ArrayList<>();
        String query = "SELECT s.id, s.departmentId, s.semesterNumber, s.name, " +
                       "s.description, s.isActive, s.created_at, " +
                       "d.name AS departmentName, d.code AS departmentCode " +
                       "FROM Semesters s " +
                       "INNER JOIN Departments d ON s.departmentId = d.id " +
                       "WHERE s.departmentId = ? AND s.isActive = 1 " +
                       "ORDER BY s.semesterNumber";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, departmentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Semester semester = new Semester();
                semester.setId(rs.getInt("id"));
                semester.setDepartmentId(rs.getInt("departmentId"));
                semester.setSemesterNumber(rs.getInt("semesterNumber"));
                semester.setName(rs.getString("name"));
                semester.setDescription(rs.getString("description"));
                semester.setActive(rs.getBoolean("isActive"));
                semester.setDepartmentName(rs.getString("departmentName"));
                semester.setDepartmentCode(rs.getString("departmentCode"));
                
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                    semester.setCreatedAt(ts.toLocalDateTime());
                }
                
                semesters.add(semester);
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(SemesterDAO.class.getName()).warning("Error fetching semesters by department: " + e.getMessage());
            java.util.logging.Logger.getLogger(SemesterDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return semesters;
    }

    public Semester getSemesterById(int id) {
        String query = "SELECT s.id, s.departmentId, s.semesterNumber, s.name, " +
                       "s.description, s.isActive, s.created_at, " +
                       "d.name AS departmentName, d.code AS departmentCode " +
                       "FROM Semesters s " +
                       "INNER JOIN Departments d ON s.departmentId = d.id " +
                       "WHERE s.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Semester semester = new Semester();
                semester.setId(rs.getInt("id"));
                semester.setDepartmentId(rs.getInt("departmentId"));
                semester.setSemesterNumber(rs.getInt("semesterNumber"));
                semester.setName(rs.getString("name"));
                semester.setDescription(rs.getString("description"));
                semester.setActive(rs.getBoolean("isActive"));
                semester.setDepartmentName(rs.getString("departmentName"));
                semester.setDepartmentCode(rs.getString("departmentCode"));
                
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                    semester.setCreatedAt(ts.toLocalDateTime());
                }
                
                return semester;
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(SemesterDAO.class.getName()).warning("Error fetching semester by ID: " + e.getMessage());
            java.util.logging.Logger.getLogger(SemesterDAO.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }

        return null;
    }

    public List<Semester> getAllSemesters() {
        List<Semester> semesters = new ArrayList<>();
        String query = "SELECT s.id, s.departmentId, s.semesterNumber, s.name, " +
                       "s.description, s.isActive, s.created_at, " +
                       "d.name AS departmentName, d.code AS departmentCode " +
                       "FROM Semesters s " +
                       "INNER JOIN Departments d ON s.departmentId = d.id " +
                       "ORDER BY d.code, s.semesterNumber";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                semesters.add(mapSemester(rs));
            }

        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(SemesterDAO.class.getName()).warning("Error fetching all semesters: " + e.getMessage());
        }

        return semesters;
    }

    public int createSemester(Semester semester) throws SQLException {
        String query = "INSERT INTO Semesters (departmentId, semesterNumber, name, description, isActive) " +
                       "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, semester.getDepartmentId());
            stmt.setInt(2, semester.getSemesterNumber());
            stmt.setString(3, semester.getName());
            stmt.setString(4, semester.getDescription());
            stmt.setBoolean(5, semester.isActive());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean updateSemester(Semester semester) throws SQLException {
        String query = "UPDATE Semesters SET departmentId = ?, semesterNumber = ?, name = ?, " +
                       "description = ?, isActive = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, semester.getDepartmentId());
            stmt.setInt(2, semester.getSemesterNumber());
            stmt.setString(3, semester.getName());
            stmt.setString(4, semester.getDescription());
            stmt.setBoolean(5, semester.isActive());
            stmt.setInt(6, semester.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteSemester(int id) throws SQLException {
        String query = "DELETE FROM Semesters WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean setSemesterActive(int id, boolean active) throws SQLException {
        String query = "UPDATE Semesters SET isActive = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, active);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private Semester mapSemester(ResultSet rs) throws SQLException {
        Semester semester = new Semester();
        semester.setId(rs.getInt("id"));
        semester.setDepartmentId(rs.getInt("departmentId"));
        semester.setSemesterNumber(rs.getInt("semesterNumber"));
        semester.setName(rs.getString("name"));
        semester.setDescription(rs.getString("description"));
        semester.setActive(rs.getBoolean("isActive"));
        semester.setDepartmentName(rs.getString("departmentName"));
        semester.setDepartmentCode(rs.getString("departmentCode"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            semester.setCreatedAt(ts.toLocalDateTime());
        }
        return semester;
    }
}
