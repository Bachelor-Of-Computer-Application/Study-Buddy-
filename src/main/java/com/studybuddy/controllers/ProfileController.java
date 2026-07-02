package com.studybuddy.controllers;

import com.studybuddy.utils.SessionManager;
import com.studybuddy.models.User;
import com.studybuddy.services.UserService;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProfileController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private ImageView profileImageView;
    @FXML
    private Label displayNameLabel;
    @FXML
    private Label usernameSubLabel;
    @FXML
    private Label pointsBadgeLabel;
    @FXML
    private Label joinedDateLabel;
    @FXML
    private Label userRoleLabel;

    // Overview Tab
    @FXML
    private Label ovFullName;
    @FXML
    private Label ovUsername;
    @FXML
    private Label ovEmail;
    @FXML
    private Label ovPhone;
    @FXML
    private Label ovBio;
    @FXML
    private Label ovDept;
    @FXML
    private Label ovSemester;
    @FXML
    private Label ovInterests;

    // Activity Stats
    @FXML
    private Label statNotesCreated;
    @FXML
    private Label statResourcesUploaded;
    @FXML
    private Label statQuestionsAsked;
    @FXML
    private Label statAnswersPosted;
    @FXML
    private Label statStudyHours;
    @FXML
    private Label statAchievements;

    // Edit Profile Tab
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField phoneNumberField;
    @FXML
    private TextArea bioField;
    @FXML
    private TextField departmentField;
    @FXML
    private ComboBox<String> semesterCombo;
    @FXML
    private Label avatarPathLabel;

    // Study Interests Tab
    @FXML
    private TextField preferredSubjectsField;
    @FXML
    private TextArea studyGoalsField;
    @FXML
    private TextField learningInterestsField;

    // Account Settings Tab
    @FXML
    private TextField emailField;

    // Password Tab
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    // Notifications Tab
    @FXML
    private CheckBox emailNotificationsCheck;
    @FXML
    private CheckBox resourceNotificationsCheck;
    @FXML
    private CheckBox systemNotificationsCheck;

    // Activity Tab
    @FXML
    private ListView<String> activityListView;

    private final UserService userService = new UserService();
    private User currentUser;
    private File selectedAvatarFile;

    @FXML
    public void initialize() {

        currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser == null) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Session Expired",
                    "Please login again."
            );
            return;
        }

        semesterCombo.setItems(FXCollections.observableArrayList(
                "Semester 1", "Semester 2", "Semester 3", "Semester 4",
                "Semester 5", "Semester 6", "Semester 7", "Semester 8"
        ));

        loadProfileData();
        loadActivityLog();
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
        statNotesCreated.setText("4"); // Mock stats for display
        statResourcesUploaded.setText(String.valueOf(currentUser.getAchievements()));
        statQuestionsAsked.setText(String.valueOf(currentUser.getQuestionsCount()));
        statAnswersPosted.setText(String.valueOf(currentUser.getAnswersCount()));
        statStudyHours.setText("12.5h");
        statAchievements.setText(String.valueOf(currentUser.getAchievements()));

        // Populate Edit Profile Fields
        fullNameField.setText(currentUser.getFullName());
        usernameField.setText(currentUser.getUsername());
        phoneNumberField.setText(currentUser.getPhoneNumber());
        bioField.setText(currentUser.getBio());
        departmentField.setText(currentUser.getDepartment());
        semesterCombo.setValue(currentUser.getSemester());

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
        activities.add("Logged in successfully (Today, 09:15 AM)");
        activities.add("Opened 'Computer Architecture' notes (Today, 10:02 AM)");
        activities.add("Asked community question: 'How to normalize tables?' (Yesterday, 04:30 PM)");
        activities.add("Uploaded note 'Physics Unit 3 Guide' for review (2 days ago)");
        activities.add("Completed study session - 2.5 hours (3 days ago)");
        activityListView.setItems(FXCollections.observableArrayList(activities));
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
        currentUser.setDepartment(departmentField.getText().trim());
        currentUser.setSemester(semesterCombo.getValue());

        if (selectedAvatarFile != null) {
            currentUser.setProfileImagePath(
                    selectedAvatarFile.getAbsolutePath()
            );
        }

        // DEBUG OUTPUT
        System.out.println("ID = " + currentUser.getId());
        System.out.println("Phone = " + currentUser.getPhoneNumber());
        System.out.println("Department = " + currentUser.getDepartment());
        System.out.println("Semester = " + currentUser.getSemester());

        boolean success =
                userService.updatePersonalInformation(currentUser);

        System.out.println("Update Result = " + success);

        userService.updateFullName(currentUser.getId(), name);
        userService.updateUsername(currentUser.getId(), uname);

        if (success) {

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

        currentUser.setPreferredSubjects(
                java.util.Objects.toString(preferredSubjectsField.getText(), "").trim());

        currentUser.setStudyGoals(
                java.util.Objects.toString(studyGoalsField.getText(), "").trim());

        currentUser.setLearningInterests(
                java.util.Objects.toString(learningInterestsField.getText(), "").trim());

        userService.updateStudyPreferences(currentUser);

        showAlert(Alert.AlertType.INFORMATION,
                "Success",
                "Study preferences updated successfully.");

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
        userService.updateEmail(currentUser.getId(), email);

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

        userService.updateEmailSettings(currentUser);

        showAlert(Alert.AlertType.INFORMATION, "Success", "Notification preferences saved.");
        loadProfileData();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}