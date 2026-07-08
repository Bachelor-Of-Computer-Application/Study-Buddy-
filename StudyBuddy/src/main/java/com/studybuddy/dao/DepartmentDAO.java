package com.studybuddy.dao;

import com.studybuddy.models.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Departments table.
 */
public class DepartmentDAO {

    public List<Department> getAllActiveDepartments() {
        List<Department> departments = new ArrayList<>();
        String query = "SELECT id, name, code, description, isActive, created_at " +
                       "FROM Departments WHERE isActive = 1 ORDER BY code";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                departments.add(mapDepartment(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching departments: " + e.getMessage());
            e.printStackTrace();
        }

        return departments;
    }

    public Department getDepartmentById(int id) {
        String query = "SELECT id, name, code, description, isActive, created_at " +
                       "FROM Departments WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapDepartment(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching department by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public Department getDepartmentByCode(String code) {
        String query = "SELECT id, name, code, description, isActive, created_at " +
                       "FROM Departments WHERE code = ? AND isActive = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapDepartment(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching department by code: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public List<Department> getAllDepartments() {
        List<Department> departments = new ArrayList<>();
        String query = "SELECT id, name, code, description, isActive, created_at " +
                       "FROM Departments ORDER BY code";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                departments.add(mapDepartment(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching all departments: " + e.getMessage());
        }

        return departments;
    }

    public int createDepartment(Department dept) throws SQLException {
        String query = "INSERT INTO Departments (name, code, description, isActive) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, dept.getName());
            stmt.setString(2, dept.getCode());
            stmt.setString(3, dept.getDescription());
            stmt.setBoolean(4, dept.isActive());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean updateDepartment(Department dept) throws SQLException {
        String query = "UPDATE Departments SET name = ?, code = ?, description = ?, isActive = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, dept.getName());
            stmt.setString(2, dept.getCode());
            stmt.setString(3, dept.getDescription());
            stmt.setBoolean(4, dept.isActive());
            stmt.setInt(5, dept.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteDepartment(int id) throws SQLException {
        String query = "DELETE FROM Departments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean setDepartmentActive(int id, boolean active) throws SQLException {
        String query = "UPDATE Departments SET isActive = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, active);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean codeExists(String code, Integer excludeId) throws SQLException {
        String query = excludeId != null
                ? "SELECT COUNT(*) FROM Departments WHERE code = ? AND id <> ?"
                : "SELECT COUNT(*) FROM Departments WHERE code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, code);
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Department mapDepartment(ResultSet rs) throws SQLException {
        Department dept = new Department();
        dept.setId(rs.getInt("id"));
        dept.setName(rs.getString("name"));
        dept.setCode(rs.getString("code"));
        dept.setDescription(rs.getString("description"));
        dept.setActive(rs.getBoolean("isActive"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            dept.setCreatedAt(ts.toLocalDateTime());
        }
        return dept;
    }
}
