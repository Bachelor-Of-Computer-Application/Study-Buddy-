package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Department;
import com.studybuddy.models.Semester;
import com.studybuddy.models.User;
import com.studybuddy.models.UserActivity;
import com.studybuddy.services.AcademicService;
import com.studybuddy.services.StatisticsService;
import com.studybuddy.services.UserService;
import com.studybuddy.utils.EventBus;
import com.studybuddy.utils.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProfileController {

    @FXML private BorderPane rootPane;
    @FXML private ImageView profileImageView;
    @FXML private Label displayNameLabel;
    @FXML private Label usernameSubLabel;
    @FXML private Label pointsBadgeLabel;
    @FXML private Label joinedDateLabel;
    @FXML private Label userRoleLabel;

    // Overview Tab
    @FXML private Label ovFullName;
    @FXML private Label ovUsername;
    @FXML private Label ovEmail;
    @FXML private Label ovPhone;
    @FXML private Label ovBio;
    @FXML private Label ovDept;
    @FXML private Label ovSemester;
    @FXML private Label ovInterests;

    // Activity Stats
    @FXML private Label statNotesCreated;
    @FXML private Label statResourcesUploaded;
    @FXML private Label statQuestionsAsked;
    @FXML private Label statAnswersPosted;
    @FXML private Label statStudyHours;
    @FXML private Label statAchievements;

    // Edit Profile Tab
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField phoneNumberField;
    @FXML private TextArea bioField;
    @FXML private ComboBox<Department> departmentField;
    @FXML private ComboBox<Semester> semesterCombo;
    @FXML private Label avatarPathLabel;

    // Study Interests Tab
    @FXML private TextField preferredSubjectsField;
    @FXML private TextArea studyGoalsField;
    @FXML private TextField learningInterestsField;

    // Account Settings Tab
    @FXML private TextField emailField;

    // Password Tab
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    // Notifications Tab
    @FXML private CheckBox emailNotificationsCheck;
    @FXML private CheckBox resourceNotificationsCheck;
    @FXML private CheckBox systemNotificationsCheck;

    // Activity Tab
    @FXML private ListView<String> activityListView;

    private final UserService userService = new UserService();
    private final StatisticsService statsService = new StatisticsService();
    private final AcademicService academicService = AcademicService.getInstance();
    private final com.studybuddy.dao.UserActivityDAO activityDAO = new com.studybuddy.dao.UserActivityDAO();
    private final com.studybuddy.services.TaskService taskService = new com.studybuddy.services.TaskService();
    private User currentUser;
    private File selectedAvatarFile;

    @FXML
    public void initialize() {
        // Always re-fetch the fully-populated user from SQL Server so that
        // the profile page always reflects the latest data — never stale data
        // from the in-memory session or a previous app run.
        User sessionUser = App.getCurrentUser();

        if (sessionUser != null && sessionUser.getId() > 0) {
            // Re-fetch from DB by ID to guarantee all columns are current
            User freshUser = userService.getUserProfileById(sessionUser.getId());
            if (freshUser != null) {
                currentUser = freshUser;
                App.setCurrentUser(freshUser);   // keep App session in sync
            } else {
                // DB fetch failed — fall back to whatever is in the session
                currentUser = sessionUser;
            }
        } else {
            // No user is logged in; show empty profile rather than hardcoded mock.
            // The UI will display "-" placeholders via loadProfileData() null-checks.
            currentUser = null;
        }

        // Initialize Department Combo Box with real departments from database
        try {
            departmentField.setItems(FXCollections.observableArrayList(academicService.getAllActiveDepartments()));
        } catch (Exception e) {
            System.err.println("Failed to load departments: " + e.getMessage());
        }
        
        // Set cell factory for department ComboBox
        departmentField.setCellFactory(param -> new javafx.scene.control.ListCell<Department>() {
            @Override
            protected void updateItem(Department item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        departmentField.setButtonCell(new javafx.scene.control.ListCell<Department>() {
            @Override
            protected void updateItem(Department item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        
        // Set cell factory for semester ComboBox
        semesterCombo.setCellFactory(param -> new javafx.scene.control.ListCell<Semester>() {
            @Override
            protected void updateItem(Semester item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        semesterCombo.setButtonCell(new javafx.scene.control.ListCell<Semester>() {
            @Override
            protected void updateItem(Semester item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        // Initialize Semester Combo Box: load semesters when department is selected
        semesterCombo.setDisable(true);
        departmentField.setOnAction(e -> {
            Department selectedDept = departmentField.getValue();
            semesterCombo.getItems().clear();
            semesterCombo.setValue(null);
            semesterCombo.setDisable(selectedDept == null);
            if (selectedDept != null) {
                try {
                    semesterCombo.setItems(FXCollections.observableArrayList(academicService.getSemestersByDepartment(selectedDept.getId())));
                } catch (Exception ex) {
                    System.err.println("Failed to load semesters: " + ex.getMessage());
                }
            }
        });

        loadProfileData();
        loadActivityLog();
        
        // Subscribe to EventBus events
        EventBus.getInstance().subscribe(EventBus.NotesChangedEvent.class, (_event) -> {
            loadProfileData();
            loadActivityLog();
        });
        EventBus.getInstance().subscribe(EventBus.ResourcesChangedEvent.class, (_event) -> {
            loadProfileData();
            loadActivityLog();
        });
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, (_event) -> {
            loadProfileData();
            loadActivityLog();
        });
        EventBus.getInstance().subscribe(EventBus.ProfileChangedEvent.class, (_event) -> {
            loadProfileData();
            loadActivityLog();
        });
    }

    private void loadProfileData() {
        if (currentUser == null) return;

        // Header / Summary Card
        displayNameLabel.setText(currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getName());
        usernameSubLabel.setText("@" + (currentUser.getUsername() != null ? currentUser.getUsername() : "username"));
        pointsBadgeLabel.setText(currentUser.getPoints() + " pts");
        userRoleLabel.setText("🎓 Role: " + (currentUser.getRole() != null ? currentUser.getRole() : "Student"));
        if (currentUser.getCreatedAt() != null) {
            joinedDateLabel.setText("📅 Joined: " + currentUser.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy")));
        } else {
            joinedDateLabel.setText("📅 Joined: Jan 2026");
        }

        // 1. Overview Tab Labels
        ovFullName.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "-");
        ovUsername.setText(currentUser.getUsername() != null ? currentUser.getUsername() : "-");
        ovEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "-");
        ovPhone.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "-");
        ovBio.setText(currentUser.getBio() != null ? currentUser.getBio() : "-");
        ovDept.setText(currentUser.getDepartment() != null ? currentUser.getDepartment() : "-");
        ovSemester.setText(currentUser.getSemester() != null ? currentUser.getSemester() : "-");
        ovInterests.setText(currentUser.getPreferredSubjects() != null ? currentUser.getPreferredSubjects() : "-");

        // Stats Labels
        try {
            statNotesCreated.setText(String.valueOf(statsService.getNotesUploaded(currentUser.getId())));
            statResourcesUploaded.setText(String.valueOf(statsService.getResourcesUploaded(currentUser.getId())));
            statQuestionsAsked.setText(String.valueOf(statsService.getQuestionsAsked(currentUser.getId())));
            statAnswersPosted.setText(String.valueOf(statsService.getAnswersSubmitted(currentUser.getId())));
        } catch (SQLException e) {
            statNotesCreated.setText("0");
            statResourcesUploaded.setText("0");
            statQuestionsAsked.setText("0");
            statAnswersPosted.setText("0");
        }
        double studyHours = taskService.getStudyHours(currentUser.getId());
        statStudyHours.setText(String.format("%.1fh", studyHours));
        statAchievements.setText(String.valueOf(currentUser.getAchievements()));

        // Populate Edit Profile Fields
        fullNameField.setText(currentUser.getFullName());
        usernameField.setText(currentUser.getUsername());
        phoneNumberField.setText(currentUser.getPhoneNumber());
        bioField.setText(currentUser.getBio());
        
        // Set department and semester from database
        if (currentUser.getDepartment() != null) {
            // Find the Department object with matching name
            for (Department dept : departmentField.getItems()) {
                if (dept.getName().equals(currentUser.getDepartment())) {
                    departmentField.setValue(dept);
                    // Load semesters for this department
                    try {
                        semesterCombo.setItems(FXCollections.observableArrayList(academicService.getSemestersByDepartment(dept.getId())));
                        semesterCombo.setDisable(false);
                        
                        // Find the Semester object with matching name
                        if (currentUser.getSemester() != null) {
                            for (Semester sem : semesterCombo.getItems()) {
                                if (sem.getName().equals(currentUser.getSemester())) {
                                    semesterCombo.setValue(sem);
                                    break;
                                }
                            }
                        }
                        break;
                    } catch (Exception e) {
                        System.err.println("Failed to load semesters: " + e.getMessage());
                    }
                }
            }
        }

        // Populate Study Interests Fields
        preferredSubjectsField.setText(currentUser.getPreferredSubjects());
        studyGoalsField.setText(currentUser.getStudyGoals());
        learningInterestsField.setText(currentUser.getLearningInterests());

        // Populate Account / Email Fields
        emailField.setText(currentUser.getEmail());

        // Populate Notification Checkboxes
        emailNotificationsCheck.setSelected(currentUser.isEmailNotificationsEnabled());
        resourceNotificationsCheck.setSelected(currentUser.isResourceUpdateNotifications());
        systemNotificationsCheck.setSelected(currentUser.isSystemNotifications());

        // Avatar Image
        try {
            if (currentUser.getProfileImagePath() != null && new File(currentUser.getProfileImagePath()).exists()) {
                profileImageView.setImage(new Image(new File(currentUser.getProfileImagePath()).toURI().toString()));
            }
        } catch (Exception e) {
            System.err.println("Failed to load user avatar: " + e.getMessage());
        }
    }

    private void loadActivityLog() {
        List<String> activities = new ArrayList<>();
        if (currentUser != null) {
            try {
                List<UserActivity> userActivities = activityDAO.getUserActivities(currentUser.getId(), 20);
                for (UserActivity activity : userActivities) {
                    String displayText = String.format(
                        "%s (%s)",
                        formatActivityText(activity),
                        activity.getCreatedAt() != null ? activity.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")) : "N/A"
                    );
                    activities.add(displayText);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                activities.add("Failed to load activity log");
            }
        }
        if (activities.isEmpty()) {
            activities.add("No recent activity");
        }
        activityListView.setItems(FXCollections.observableArrayList(activities));
    }

    private String formatActivityText(UserActivity activity) {
        String action = activity.getAction();
        String targetType = activity.getTargetType();
        String targetName = activity.getTargetName();
        
        switch (action) {
            case "UPLOAD_NOTE":
                return String.format("Uploaded note: '%s'", targetName);
            case "UPLOAD_RESOURCE":
                return String.format("Uploaded resource: '%s'", targetName);
            case "ASK_QUESTION":
                return String.format("Asked question: '%s'", targetName);
            case "SUBMIT_ANSWER":
                return String.format("Answered question: '%s'", targetName);
            case "APPROVE_NOTE":
                return String.format("Approved note: '%s'", targetName);
            case "REJECT_NOTE":
                return String.format("Rejected note: '%s'", targetName);
            default:
                return String.format("%s: %s", action, targetName);
        }
    }

    @FXML
    public void handleUploadAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            selectedAvatarFile = file;
            avatarPathLabel.setText(file.getName());
        }
    }

    @FXML
    public void handleSavePersonal() {

        String name = fullNameField.getText().trim();
        String uname = usernameField.getText().trim();

        if (name.isEmpty() || uname.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Validation Error",
                    "Full Name and Username cannot be empty."
            );
            return;
        }

        currentUser.setFullName(name);
        currentUser.setUsername(uname);
        currentUser.setPhoneNumber(phoneNumberField.getText().trim());
        currentUser.setBio(bioField.getText().trim());
        currentUser.setDepartment(departmentField.getValue() != null ? departmentField.getValue().getName() : "");
        currentUser.setSemester(semesterCombo.getValue() != null ? semesterCombo.getValue().getName() : null);

        if (selectedAvatarFile != null) {
            currentUser.setProfileImagePath(
                    selectedAvatarFile.getAbsolutePath()
            );
        }

        boolean success = userService.updatePersonalInformation(currentUser);
        userService.updateFullName(currentUser.getId(), name);
        userService.updateUsername(currentUser.getId(), uname);

        if (success) {
            // Re-fetch from DB so App.currentUser always reflects the latest SQL data
            refreshCurrentUserFromDB();
            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Personal information updated successfully."
            );
            loadProfileData();
        } else {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Failed to update personal information."
            );
        }
    }

    @FXML
    public void handleResetPersonal() {
        loadProfileData();
        selectedAvatarFile = null;
        avatarPathLabel.setText("No file chosen");
    }

    @FXML
    public void handleSaveStudyPreferences() {
        currentUser.setPreferredSubjects(preferredSubjectsField.getText().trim());
        currentUser.setStudyGoals(studyGoalsField.getText().trim());
        currentUser.setLearningInterests(learningInterestsField.getText().trim());

        boolean success = userService.updateStudyPreferences(currentUser);

        if (success) {
            // Re-fetch from DB so App.currentUser always reflects the latest SQL data
            refreshCurrentUserFromDB();
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", "Study preferences updated successfully.");
        loadProfileData();
    }

    @FXML
    public void handleSaveAccount() {
        String email = emailField.getText().trim();

        if (email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a valid email address.");
            return;
        }

        currentUser.setEmail(email);
        boolean success = userService.updateEmail(currentUser.getId(), email);

        if (success) {
            // Re-fetch from DB so App.currentUser always reflects the latest SQL data
            refreshCurrentUserFromDB();
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", "Account email updated successfully.");
        loadProfileData();
    }

    @FXML
    public void handleSavePassword() {
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "All password fields are required.");
            return;
        }

        if (newPass.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "New password must be at least 6 characters.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Confirm password does not match new password.");
            return;
        }

        // Verify current password via UserService
        boolean currentMatch = userService.verifyPassword(currentUser.getId(), currentPass);
        if (!currentMatch) {
            showAlert(Alert.AlertType.ERROR, "Security Error", "Current password does not match.");
            return;
        }

        // Save new password
        boolean success = userService.updatePassword(currentUser.getId(), newPass);
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Password updated successfully.");
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update password.");
        }
    }

    @FXML
    public void handleSaveNotifications() {
        currentUser.setEmailNotificationsEnabled(emailNotificationsCheck.isSelected());
        currentUser.setResourceUpdateNotifications(resourceNotificationsCheck.isSelected());
        currentUser.setSystemNotifications(systemNotificationsCheck.isSelected());

        boolean success = userService.updateEmailSettings(currentUser);

        if (success) {
            // Re-fetch from DB so App.currentUser always reflects the latest SQL data
            refreshCurrentUserFromDB();
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", "Notification preferences saved.");
        loadProfileData();
    }

    /**
     * Re-fetches the current user's full profile from SQL Server and stores it
     * in both {@code currentUser} and {@code App.currentUser}.
     *
     * Called after every successful profile save so the in-memory session is
     * never stale — subsequent page navigation will always see updated data.
     *
     * SQL: SELECT * FROM Users WHERE id = ?  (via UserService → UserDAO.getUserById)
     */
    private void refreshCurrentUserFromDB() {
        if (currentUser == null || currentUser.getId() <= 0) return;

        User fresh = userService.getUserProfileById(currentUser.getId());
        if (fresh != null) {
            currentUser = fresh;
            App.setCurrentUser(fresh);   // keep App session in sync with SQL Server
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}