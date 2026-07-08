package com.studybuddy.services;

import com.studybuddy.dao.DepartmentDAO;
import com.studybuddy.dao.SemesterDAO;
import com.studybuddy.dao.SubjectDAO;
import com.studybuddy.models.Department;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;

import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing academic entities (Departments, Semesters, Subjects).
 * Provides business logic layer between controllers and DAOs.
 */
public class AcademicService {

    private final DepartmentDAO departmentDAO;
    private final SemesterDAO semesterDAO;
    private final SubjectDAO subjectDAO;
    private static AcademicService instance;

    private AcademicService() {
        this.departmentDAO = new DepartmentDAO();
        this.semesterDAO = new SemesterDAO();
        this.subjectDAO = new SubjectDAO();
    }

    public static synchronized AcademicService getInstance() {
        if (instance == null) {
            instance = new AcademicService();
        }
        return instance;
    }

    // Department methods
    public List<Department> getAllActiveDepartments() {
        return departmentDAO.getAllActiveDepartments();
    }

    public Department getDepartmentById(int id) {
        return departmentDAO.getDepartmentById(id);
    }

    public Department getDepartmentByCode(String code) {
        return departmentDAO.getDepartmentByCode(code);
    }

    // Semester methods
    public List<Semester> getSemestersByDepartment(int departmentId) {
        return semesterDAO.getSemestersByDepartment(departmentId);
    }

    public List<Semester> getSemestersByDepartment(Department department) {
        if (department == null) {
            return new ArrayList<>();
        }
        return getSemestersByDepartment(department.getId());
    }

    public Semester getSemesterById(int id) {
        return semesterDAO.getSemesterById(id);
    }

    // Subject methods
    public List<Subject> getSubjectsBySemester(int semesterId) {
        return subjectDAO.getSubjectsBySemester(semesterId);
    }

    public List<Subject> getSubjectsBySemester(Semester semester) {
        if (semester == null) {
            return new ArrayList<>();
        }
        return getSubjectsBySemester(semester.getId());
    }

    public Subject getSubjectById(int id) {
        return subjectDAO.getSubjectById(id);
    }

    public List<Subject> searchSubjects(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return subjectDAO.searchSubjectsByName(searchTerm);
    }

    /**
     * Returns all active subjects — not filtered by department or semester.
     * Useful when a flat list is needed (e.g. simple ComboBox filter on an admin screen).
     */
    public List<Subject> getAllActiveSubjects() {
        return subjectDAO.getAllActiveSubjects();
    }

    /**
     * Returns all active subject names as a sorted string list.
     * Convenience method for populating a {@code ComboBox<String>}.
     */
    public List<String> getAllSubjectNames() {
        return getAllActiveSubjects().stream()
                .map(Subject::getName)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }
}
