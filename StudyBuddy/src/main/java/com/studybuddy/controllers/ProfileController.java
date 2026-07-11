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
import com.studybuddy.utils.ImageLoader;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TextFormatter;

public class ProfileController {

    @FXML private TabPane profileTabPane;
    @FXML private Button uploadAvatarBtn;
    @FXML private Button removeAvatarBtn;
    @FXML private BorderPane rootPane;
    @FXML private ImageView profileImageView;
    @FXML private StackPane avatarStackPane;
    @FXML private VBox avatarHoverOverlay;
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
    private final ImageLoader imageLoader = ImageLoader.getInstance();
    private User currentUser;
    private static final double PROFILE_AVATAR_SIZE = 100;

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
            departmentField.setItems(
                    FXCollections.observableArrayList(
                            academicService.getAllActiveDepartments()
                    )
            );

            System.out.println("Departments loaded:");

            for (Department d : departmentField.getItems()) {
                System.out.println(
                        d.getId() + " - " + d.getName()
                );
            }

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


// Initialize Semester Combo Box
        semesterCombo.setDisable(true);


// Department → Semester loading
        departmentField.setOnAction(e -> {

            Department selectedDept = departmentField.getValue();

            semesterCombo.getItems().clear();
            semesterCombo.setValue(null);

            semesterCombo.setDisable(selectedDept == null);

            if (selectedDept != null) {
                try {

                    semesterCombo.setItems(
                            FXCollections.observableArrayList(
                                    academicService.getSemestersByDepartment(
                                            selectedDept.getId()
                                    )
                            )
                    );

                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
            // Allow only digits and limit phone number to 10 digits
            phoneNumberField.setTextFormatter(new TextFormatter<>(change -> {
                System.out.println("New text: " + change.getControlNewText());

                if (!change.getText().matches("[0-9]*")) {
                    return null;
                }

                if (change.getControlNewText().length() > 10) {
                    return null;
                }

                return change;
            }));
        });

        loadProfileData();
        loadActivityLog();
        setupAvatarInteractions();

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
            refreshCurrentUserFromDB();
            loadProfileData();
            loadActivityLog();
        });
    }

    private void setupAvatarInteractions() {
        imageLoader.configureCircularAvatar(profileImageView, PROFILE_AVATAR_SIZE);
        if (avatarStackPane != null) {
            avatarStackPane.setOnMouseClicked(this::handleAvatarAreaClick);
            avatarStackPane.setOnMouseEntered(e -> {
                if (avatarHoverOverlay != null) {
                    avatarHoverOverlay.setVisible(true);
                    avatarHoverOverlay.setManaged(true);
                }
            });
            avatarStackPane.setOnMouseExited(e -> {
                if (avatarHoverOverlay != null) {
                    avatarHoverOverlay.setVisible(false);
                    avatarHoverOverlay.setManaged(false);
                }
            });
        }
        if (avatarHoverOverlay != null) {
            avatarHoverOverlay.setVisible(false);
            avatarHoverOverlay.setManaged(false);
        }
    }

    private void handleAvatarAreaClick(MouseEvent event) {
        handleUploadAvatar();
    }

    private void refreshAvatarDisplay() {
        String path = currentUser != null ? currentUser.getProfileImagePath() : null;
        imageLoader.applyAvatarToView(profileImageView, path, PROFILE_AVATAR_SIZE);
        if (avatarPathLabel != null) {
            if (path != null && !path.isBlank()) {
                avatarPathLabel.setText(new File(path).getName());
            } else {
                avatarPathLabel.setText("Using default avatar");
            }
        }
        if (removeAvatarBtn != null) {
            removeAvatarBtn.setDisable(path == null || path.isBlank());
        }
    }

    private void publishProfileImageChanged() {
        EventBus.getInstance().publish(new EventBus.ProfileChangedEvent());
    }

    private void loadProfileData() {
        if (currentUser == null) return;

        // Header / Summary Card
        displayNameLabel.setText(currentUser.getDisplayFullName());
        usernameSubLabel.setText("@" + (currentUser.getUsername() != null ? currentUser.getUsername() : "username"));
        pointsBadgeLabel.setText(currentUser.getPoints() + " pts");
        userRoleLabel.setText("🎓 Role: " + (currentUser.getRole() != null ? currentUser.getRole() : "Student"));
        if (currentUser.getCreatedAt() != null) {
            joinedDateLabel.setText("📅 Joined: " + currentUser.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy")));
        } else {
            joinedDateLabel.setText("📅 Joined: Jan 2026");
        }

        // 1. Overview Tab Labels
        ovFullName.setText(currentUser.getDisplayFullName());
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
        refreshAvatarDisplay();
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
        if (!requireLoggedIn()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images (JPG, PNG, WebP)", "*.jpg", "*.jpeg", "*.png", "*.webp")
        );
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            imageLoader.validateProfileImageFile(file);
            String previousPath = currentUser.getProfileImagePath();
            String savedPath = imageLoader.saveProfileImage(file, currentUser.getId());
            boolean updated = userService.updateProfileImagePath(currentUser.getId(), savedPath);
            if (!updated) {
                imageLoader.deleteProfileImageFile(savedPath);
                showAlert(Alert.AlertType.ERROR, "Error", "Could not save profile picture to your account.");
                return;
            }
            if (previousPath != null && !previousPath.equals(savedPath)) {
                imageLoader.deleteProfileImageFile(previousPath);
            }
            refreshCurrentUserFromDB();
            refreshAvatarDisplay();
            publishProfileImageChanged();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile picture updated.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Image", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update profile picture: " + e.getMessage());
        }
    }

    @FXML
    public void handleRemoveAvatar() {
        if (!requireLoggedIn()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove your profile picture and use the default avatar?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Remove Photo");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        String previousPath = currentUser.getProfileImagePath();
        boolean cleared = userService.clearProfileImage(currentUser.getId());
        if (!cleared) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not remove profile picture.");
            return;
        }
        if (previousPath != null) {
            imageLoader.deleteProfileImageFile(previousPath);
        }
        refreshCurrentUserFromDB();
        refreshAvatarDisplay();
        publishProfileImageChanged();
        showAlert(Alert.AlertType.INFORMATION, "Success", "Profile picture removed.");
    }

    @FXML
    public void handleSavePersonal() {
        if (!requireLoggedIn()) return;

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

// Phone validation
        String phone = phoneNumberField.getText().trim();

        if (!phone.matches("^9[678]\\d{8}$")) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Phone Number",
                    "Please enter a valid Nepal mobile number."
            );
            return;
        }

        currentUser.setFullName(name);
        currentUser.setUsername(uname);
        currentUser.setPhoneNumber(phone);
        currentUser.setBio(bioField.getText().trim());
        currentUser.setDepartment(departmentField.getValue() != null ? departmentField.getValue().getName() : "");
        currentUser.setSemester(semesterCombo.getValue() != null ? semesterCombo.getValue().getName() : null);
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
        if (avatarPathLabel != null) {
            avatarPathLabel.setText(currentUser != null && currentUser.getProfileImagePath() != null
                    ? new File(currentUser.getProfileImagePath()).getName()
                    : "Using default avatar");
        }
    }

    @FXML
    public void handleSaveStudyPreferences() {
        if (!requireLoggedIn()) return;
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
        if (!requireLoggedIn()) return;
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
        if (!requireLoggedIn()) return;
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
        if (!requireLoggedIn()) return;
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

    private boolean requireLoggedIn() {
        if (currentUser != null) {
            return true;
        }
        showAlert(Alert.AlertType.WARNING, "Not Logged In", "Please log in to update your profile.");
        return false;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}