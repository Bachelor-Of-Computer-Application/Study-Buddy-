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
                
                departments.add(dept);
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

        } catch (SQLException e) {
            System.err.println("Error fetching department by code: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
