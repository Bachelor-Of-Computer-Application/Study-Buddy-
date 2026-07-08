package com.studybuddy.dao;

import com.studybuddy.models.Semester;

import java.sql.*;
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
            System.err.println("Error fetching semesters by department: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Error fetching semester by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
