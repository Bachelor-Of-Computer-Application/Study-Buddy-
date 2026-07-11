package com.studybuddy.utils;

import com.studybuddy.models.Department;
import com.studybuddy.models.Semester;
import com.studybuddy.models.Subject;
import com.studybuddy.models.User;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.AuthorizationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Virtual "All Departments" / "All Semesters" options and content visibility rules.
 * Sentinel id {@value #ALL_ID} is never stored in the database — maps to SQL NULL.
 */
public final class AcademicFilterHelper {

    public static final int ALL_ID = 0;

    private AcademicFilterHelper() {}

    public static Department allDepartments() {
        Department d = new Department();
        d.setId(ALL_ID);
        d.setName("All Departments");
        d.setCode("ALL");
        d.setActive(true);
        return d;
    }

    public static Semester allSemesters() {
        Semester s = new Semester();
        s.setId(ALL_ID);
        s.setName("All Semesters");
        s.setActive(true);
        return s;
    }

    public static boolean isAllDepartments(Department dept) {
        return dept == null || dept.getId() == ALL_ID;
    }

    public static boolean isAllSemesters(Semester sem) {
        return sem == null || sem.getId() == ALL_ID;
    }

    /** Returns null when "All Departments" — store NULL in DB. */
    public static Integer resolveDepartmentId(Department dept) {
        return isAllDepartments(dept) ? null : dept.getId();
    }

    /** Returns null when "All Semesters" — store NULL in DB. */
    public static Integer resolveSemesterId(Semester sem) {
        return isAllSemesters(sem) ? null : sem.getId();
    }

    public static ObservableList<Department> departmentsForFilter(AcademicService academic) {
        List<Department> items = new ArrayList<>();
        items.add(allDepartments());
        try {
            items.addAll(academic.getAllActiveDepartments());
        } catch (Exception e) {
            System.err.println("[AcademicFilterHelper] Failed to load departments: " + e.getMessage());
        }
        return FXCollections.observableArrayList(items);
    }

    public static ObservableList<Semester> semestersForFilter(AcademicService academic, Department dept) {
        List<Semester> items = new ArrayList<>();
        items.add(allSemesters());
        try {
            if (!isAllDepartments(dept)) {
                items.addAll(academic.getSemestersByDepartment(dept.getId()));
            } else {
                items.addAll(academic.getAllSemesters().stream()
                        .filter(Semester::isActive)
                        .toList());
            }
        } catch (Exception e) {
            System.err.println("[AcademicFilterHelper] Failed to load semesters: " + e.getMessage());
        }
        return FXCollections.observableArrayList(items);
    }

    /**
     * Wires Department → Semester → Subject cascade for upload/filter ComboBoxes.
     */
    public static void wireCascade(
            AcademicService academic,
            ComboBox<Department> deptBox,
            ComboBox<Semester> semBox,
            ComboBox<Subject> subjectBox,
            Runnable onSubjectsReset) {

        if (deptBox == null) return;

        deptBox.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Department item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        deptBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Department item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        if (semBox != null) {
            semBox.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(Semester item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getName());
                }
            });
            semBox.setButtonCell(new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(Semester item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getName());
                }
            });
            semBox.setDisable(deptBox.getValue() == null);
        }
        if (subjectBox != null) {
            subjectBox.setDisable(deptBox.getValue() == null);
        }

        deptBox.setOnAction(e -> {
            Department dept = deptBox.getValue();
            if (semBox != null) {
                semBox.setItems(semestersForFilter(academic, dept));
                semBox.setValue(allSemesters());
                semBox.setDisable(dept == null);
            }
            if (subjectBox != null) {
                subjectBox.getSelectionModel().clearSelection();
            }
            if (onSubjectsReset != null) onSubjectsReset.run();
        });

        if (semBox != null) {
            semBox.setOnAction(e -> {
                if (subjectBox != null) {
                    subjectBox.getSelectionModel().clearSelection();
                }
                if (onSubjectsReset != null) onSubjectsReset.run();
            });
        }
    }

    /**
     * Loads subjects for department/semester filters into a {@code ComboBox<Subject>}.
     * {@code null} or {@link #ALL_ID} on department/semester means "All" for that filter.
     */
    public static void loadSubjects(
            AcademicService academic,
            Department dept,
            Semester sem,
            ComboBox<Subject> subjectBox) {
        if (subjectBox == null || academic == null) {
            return;
        }
        if (sem == null || isAllSemesters(sem)) {
            subjectBox.getItems().clear();
            subjectBox.setDisable(false);
            return;
        }
        try {
            List<Subject> subjects = academic.getSubjects(resolveDepartmentId(dept), resolveSemesterId(sem));
            subjectBox.setItems(FXCollections.observableArrayList(subjects));
            subjectBox.setDisable(dept == null);
        } catch (Exception e) {
            System.err.println("[AcademicFilterHelper] Failed to load subjects: " + e.getMessage());
        }
    }

    public static void loadSubjectsForSemester(
            AcademicService academic,
            Semester sem,
            ComboBox<Subject> subjectBox) {
        if (subjectBox == null || academic == null) {
            return;
        }
        if (sem == null || isAllSemesters(sem)) {
            subjectBox.getItems().clear();
            subjectBox.setDisable(true);
            return;
        }
        try {
            List<Subject> subjects = academic.getSubjectsBySemester(sem.getId());
            subjectBox.setItems(FXCollections.observableArrayList(subjects));
            subjectBox.setDisable(false);
        } catch (Exception e) {
            System.err.println("[AcademicFilterHelper] Failed to load semester subjects: " + e.getMessage());
        }
    }

    /**
     * Student visibility: (dept match OR global dept) AND (sem match OR global sem).
     * Admins see everything.
     */
    public static boolean isVisibleToUser(
            Integer contentDeptId,
            Integer contentSemId,
            Integer contentSubjectId,
            Map<Integer, Subject> subjectMap,
            User user,
            AcademicService academic) {

        if (user == null) return true;
        if (AuthorizationService.getInstance().isAdmin(user)) return true;

        Integer effectiveDept = normalizeId(contentDeptId);
        Integer effectiveSem = normalizeId(contentSemId);

        if (contentSubjectId != null && contentSubjectId > 0 && subjectMap != null) {
            Subject sub = subjectMap.get(contentSubjectId);
            if (sub != null) {
                if (effectiveDept == null) effectiveDept = sub.getDepartmentId();
                if (effectiveSem == null) effectiveSem = sub.getSemesterId();
            }
        }

        if (effectiveDept != null && !matchesUserDepartment(effectiveDept, user, academic)) {
            return false;
        }
        if (effectiveSem != null && !matchesUserSemester(effectiveSem, user, academic)) {
            return false;
        }
        return true;
    }

    private static Integer normalizeId(Integer id) {
        if (id == null || id == ALL_ID) return null;
        return id;
    }

    private static boolean matchesUserDepartment(int deptId, User user, AcademicService academic) {
        String userDept = user.getDepartment();
        if (userDept == null || userDept.isBlank()) return true;
        try {
            Department d = academic.getDepartmentById(deptId);
            if (d == null) return true;
            return d.getName().equalsIgnoreCase(userDept.trim())
                    || d.getCode().equalsIgnoreCase(userDept.trim());
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean matchesUserSemester(int semId, User user, AcademicService academic) {
        String userSem = user.getSemester();
        if (userSem == null || userSem.isBlank()) return true;
        try {
            Semester s = academic.getSemesterById(semId);
            if (s == null) return true;
            return s.getName().equalsIgnoreCase(userSem.trim())
                    || String.valueOf(s.getSemesterNumber()).equals(userSem.trim());
        } catch (Exception e) {
            return true;
        }
    }

    /** Filter bar: include item when dept/sem filter matches or content is global. */
    public static boolean matchesDeptSemFilter(
            Integer contentDeptId,
            Integer contentSemId,
            int contentSubjectId,
            Department filterDept,
            Semester filterSem,
            Map<Integer, Subject> subjectMap,
            List<Subject> allSubjects,
            String contentSubjectName) {

        Integer effectiveDept = normalizeId(contentDeptId);
        Integer effectiveSem = normalizeId(contentSemId);

        if (contentSubjectId > 0 && subjectMap != null) {
            Subject sub = subjectMap.get(contentSubjectId);
            if (sub != null) {
                if (effectiveDept == null) effectiveDept = sub.getDepartmentId();
                if (effectiveSem == null) effectiveSem = sub.getSemesterId();
            }
        } else if (contentSubjectName != null && !contentSubjectName.isBlank()) {
            for (Subject s : allSubjects) {
                if (s.getName().equalsIgnoreCase(contentSubjectName)) {
                    if (effectiveDept == null) effectiveDept = s.getDepartmentId();
                    if (effectiveSem == null) effectiveSem = s.getSemesterId();
                    break;
                }
            }
        }

        if (!isAllDepartments(filterDept)) {
            if (effectiveDept == null) {
                // global content — passes dept filter
            } else if (effectiveDept != filterDept.getId()) {
                return false;
            }
        }
        if (!isAllSemesters(filterSem)) {
            if (effectiveSem == null) {
                // global semester — passes
            } else if (effectiveSem != filterSem.getId()) {
                return false;
            }
        }
        return true;
    }

    public static void setupFilterBar(
            AcademicService academic,
            ComboBox<Department> deptBox,
            ComboBox<Semester> semBox,
            ComboBox<Subject> subjectBox) {

        if (deptBox != null) {
            deptBox.setItems(departmentsForFilter(academic));
        }
        if (deptBox != null) deptBox.setValue(allDepartments());

        Runnable refreshSubjects = () -> refreshSubjectFilter(academic, deptBox, semBox, subjectBox);
        wireCascade(academic, deptBox, semBox, subjectBox, refreshSubjects);

        if (semBox != null) {
            semBox.setItems(semestersForFilter(academic, allDepartments()));
            semBox.setValue(allSemesters());
            semBox.setDisable(false);
        }

        refreshSubjects.run();
    }

    private static void refreshSubjectFilter(
            AcademicService academic,
            ComboBox<Department> deptBox,
            ComboBox<Semester> semBox,
            ComboBox<Subject> subjectBox) {

        if (subjectBox == null) return;

        subjectBox.getSelectionModel().clearSelection();
        Department dept = deptBox != null ? deptBox.getValue() : null;
        Semester sem = semBox != null ? semBox.getValue() : null;
        loadSubjects(academic, dept, sem, subjectBox);
    }

    public static void resetFilters(
            AcademicService academic,
            ComboBox<Department> deptBox,
            ComboBox<Semester> semBox,
            ComboBox<Subject> subjectBox) {
        if (deptBox != null) {
            deptBox.setValue(allDepartments());
        }
        if (semBox != null) {
            semBox.setItems(semestersForFilter(academic, allDepartments()));
            semBox.setValue(allSemesters());
            semBox.setDisable(false);
        }
        if (subjectBox != null) {
            subjectBox.getSelectionModel().clearSelection();
            refreshSubjectFilter(academic, deptBox, semBox, subjectBox);
        }
    }
}
