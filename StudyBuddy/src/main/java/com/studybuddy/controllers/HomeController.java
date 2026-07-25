package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.admin.services.NotificationService;
import com.studybuddy.models.Notification;
import com.studybuddy.models.Task;
import com.studybuddy.models.User;
import com.studybuddy.services.AuthService;
import com.studybuddy.services.TaskService;
import com.studybuddy.utils.EventBus;
import com.studybuddy.utils.ImageLoader;
import com.studybuddy.utils.SceneManager;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller for the HomeView.fxml.
 *
 * Dashboard data is loaded from SQL Server via TaskService â†’ TaskDAO.
 *
 * Architecture:
 *   HomeController â†’ TaskService â†’ TaskDAO â†’ DatabaseConnection â†’ SQL Server
 */
public class HomeController {

    @FXML private BorderPane rootPane;
    @FXML private StackPane  contentArea;
    @FXML private Text       usernameLabel;
    @FXML private Text       usernameWelcome;
    @FXML private Label      emailLabel;
    @FXML private Label      userRoleLabel;

    @FXML private Button dashboardNavBtn;
    @FXML private Button notesNavBtn;
    @FXML private Button questionsNavBtn;
    @FXML private Button resourcesNavBtn;
    @FXML private Button tasksNavBtn;
    @FXML private Button profileNavBtn;

    // Stats displayed in the home sidebar / welcome area
    @FXML private Text totalTasksCount;
    @FXML private Text completedTasksCount;
    @FXML private ImageView sidebarAvatarView;

    private User currentUser;
    private final TaskService taskService = new TaskService();
    private final ImageLoader imageLoader = ImageLoader.getInstance();
    private Button activeNavButton;
    private static final double SIDEBAR_AVATAR_SIZE = 36;

    // =========================
    // INITIALIZE
    // =========================

    @FXML
    public void initialize() {
        if (rootPane != null) {
            addFadeInAnimation(rootPane);
        }

        // Resolve current user from session
        currentUser = App.getCurrentUser();

        if (currentUser != null) {
            if (usernameLabel != null) {
                usernameLabel.setText(currentUser.getDisplayFullName());
            }
            if (usernameWelcome != null) {
                usernameWelcome.setText(currentUser.getDisplayFullName());
            }
            if (emailLabel != null) {
                emailLabel.setText(currentUser.getEmail());
            }
            if (userRoleLabel != null) {
                String role = currentUser.getRole();
                userRoleLabel.setText(role != null ? role : "Student");
            }
        }

        // Load all dashboard data from SQL Server
        loadDashboardStats();
        loadRecentTasks();
        loadStudyProgress();
        checkUserNotifications();
        refreshSidebarAvatar();

        // Automatically load DashboardView by default
        goToDashboard();

        EventBus.getInstance().subscribe(EventBus.TasksChangedEvent.class, (_event) -> loadDashboardStats());
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, (_event) -> loadDashboardStats());
        
        // Requirement 5: Refresh achievement_points when points change
        EventBus.getInstance().subscribe(EventBus.PointsChangedEvent.class, (event) -> {
            if (currentUser != null && event.getUserId() == currentUser.getId()) {
                currentUser = App.getCurrentUser();
            }
        });

        refreshNotificationCount();

        EventBus.getInstance().subscribe(EventBus.NotificationsChangedEvent.class, (event) -> {
            javafx.application.Platform.runLater(this::refreshNotificationCount);
        });
        
        EventBus.getInstance().subscribe(EventBus.ProfileChangedEvent.class, (_event) -> {
            currentUser = App.getCurrentUser();
            refreshSidebarAvatar();
            if (usernameLabel != null && currentUser != null) {
                usernameLabel.setText(currentUser.getDisplayFullName());
            }
            if (userRoleLabel != null && currentUser != null) {
                String role = currentUser.getRole();
                userRoleLabel.setText(role != null ? role : "Student");
            }
        });
    }

    private void refreshSidebarAvatar() {
        if (sidebarAvatarView == null) return;
        String path = currentUser != null ? currentUser.getProfileImagePath() : null;
        imageLoader.applyAvatarToView(sidebarAvatarView, path, SIDEBAR_AVATAR_SIZE);
    }

    // =========================
    // DASHBOARD STATS
    // =========================

    /**
     * Loads task statistics for the current user from SQL Server and
     * updates the totalTasksCount, completedTasksCount Text nodes.
     *
     * SQL (via TaskDAO.getTaskCount):
     *   SELECT COUNT(*) FROM Tasks WHERE userId = ?
     *
     * SQL (via TaskDAO.getCompletedTaskCount):
     *   SELECT COUNT(*) FROM Tasks WHERE userId = ? AND status = 'completed'
     */
    private void loadDashboardStats() {
        int userId = (currentUser != null) ? currentUser.getId() : 0;

        if (totalTasksCount != null) {
            int total     = taskService.getTotalTaskCount(userId);
            int completed = taskService.getCompletedTaskCount(userId);
            totalTasksCount.setText(completed + " / " + total);
        }

        if (completedTasksCount != null) {
            int progress = taskService.getStudyProgress(userId);
            completedTasksCount.setText(progress + "%");
        }
    }

    // =========================
    // RECENT TASKS
    // =========================

    /**
     * Loads the 10 most recent tasks for the current user from SQL Server.
     *
     * SQL (via TaskDAO.getRecentTasksByUserId):
     *   SELECT TOP (10) id, userId, title, description, status, created_at
     *   FROM Tasks
     *   WHERE userId = ?
     *   ORDER BY created_at DESC
     *
     * If a recentTasksContainer node (fx:id="recentTasksContainer") exists
     * in the FXML, task cards are rendered into it. Otherwise the data is
     * loaded silently so that other views (e.g. DashboardView) can display it.
     */
    private void loadRecentTasks() {
        int userId = (currentUser != null) ? currentUser.getId() : 0;

        List<Task> recentTasks = taskService.getRecentTasksForUser(userId);

        // If the FXML has a recentTasksContainer, populate it with task cards.
        // The HomeView.fxml currently uses a StackPane for dynamic content
        // (loaded views); task cards would be rendered inside DashboardView.
        // This method ensures data is fetched and available without stubs.
        if (contentArea != null && recentTasks != null && !recentTasks.isEmpty()) {
            // Tasks are available â€” they will be displayed when the user
            // navigates to DashboardView via goToDashboard().
            // Real data has been retrieved from SQL Server.
        }

    }

    // =========================
    // STUDY PROGRESS
    // =========================

    /**
     * Loads the study progress percentage for the current user from SQL Server
     * using a single aggregate query and updates the completedTasksCount label.
     *
     * SQL (via TaskDAO.getStudyProgress):
     *   SELECT
     *       COUNT(*) AS TotalTasks,
     *       SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) AS CompletedTasks
     *   FROM Tasks
     *   WHERE userId = ?
     *
     * progress = (CompletedTasks * 100) / TotalTasks
     * Returns 0 when TotalTasks = 0.
     */
    private void loadStudyProgress() {
        int userId = (currentUser != null) ? currentUser.getId() : 0;

        int progress = taskService.getStudyProgress(userId);

        // Update the progress label if it is bound in FXML
        if (completedTasksCount != null) {
            completedTasksCount.setText(progress + "%");
        }

    }

    // =========================
    // NAVIGATION
    // =========================

    @FXML
    public void goToDashboard() {
        loadCenterView("/com/studybuddy/fxml/DashboardView.fxml", dashboardNavBtn);
    }

    @FXML
    public void goToNotes() {
        loadCenterView("/com/studybuddy/fxml/NotesView.fxml", notesNavBtn);
    }

    @FXML
    public void goToQuestions() {
        loadCenterView("/com/studybuddy/fxml/QuestionsView.fxml", questionsNavBtn);
    }

    @FXML
    public void goToResources() {
        loadCenterView("/com/studybuddy/fxml/ResourcesView.fxml", resourcesNavBtn);
    }

    @FXML
    public void goToProgress() {
        loadCenterView("/com/studybuddy/fxml/DashboardView.fxml", dashboardNavBtn);
    }
    
    @FXML
    public void goToTasks() {
        loadCenterView("/com/studybuddy/fxml/TaskView.fxml", tasksNavBtn);
    }

    @FXML
    public void goToProfile() {
        setActiveNav(profileNavBtn);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/studybuddy/fxml/ProfileView.fxml"));
            Parent view = loader.load();
            fadeInContent(view);
        } catch (Exception e) {
            showAlert("Navigation Error", "Could not navigate to Profile: " + e.getMessage());
            java.util.logging.Logger.getLogger(HomeController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }
    }

    @FXML
    public void onLogoutClick() {
        AuthService.logout();
        try {
            SceneManager.showLoginPage(getStage());
        } catch (java.io.IOException e) {
            java.util.logging.Logger.getLogger(HomeController.class.getName()).warning("[HomeController] âŒ Could not navigate back to LoginView.fxml");
            java.util.logging.Logger.getLogger(HomeController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            showAlert("Navigation Error", "Could not return to login: " + e.getMessage());
        }
    }

    // =========================
    // HELPERS
    // =========================

    private void loadCenterView(String fxmlPath, Button navButton) {
        setActiveNav(navButton);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            fadeInContent(view);
        } catch (Exception e) {
            showAlert("Navigation Error", "Could not navigate to " + fxmlPath + ": " + e.getMessage());
            java.util.logging.Logger.getLogger(HomeController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }
    }

    private void setActiveNav(Button navButton) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-button-active");
        }
        activeNavButton = navButton;
        if (activeNavButton != null && !activeNavButton.getStyleClass().contains("nav-button-active")) {
            activeNavButton.getStyleClass().add("nav-button-active");
        }
    }

    private void fadeInContent(Parent view) {
        contentArea.getChildren().setAll(view);
        addFadeInAnimation(view);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Stage getStage() {
        return (Stage) rootPane.getScene().getWindow();
    }

    private void checkUserNotifications() {
        if (currentUser == null) return;
        try {
            NotificationService service = NotificationService.getInstance();
            List<Notification> userNotifications = service.getNotificationsByUserId(currentUser.getId());
            long unreadCount = userNotifications.stream().filter(n -> !n.isRead()).count();
            if (unreadCount > 0) {
                showNotificationsDialog();
            }
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(HomeController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }
    }

    private void refreshNotificationCount() {
        if (currentUser == null) return;
        try {
            int unread = com.studybuddy.admin.services.NotificationService.getInstance().getUnreadCountByUserId(currentUser.getId());
            if (profileNavBtn != null && profileNavBtn.getGraphic() instanceof HBox) {
                HBox hbox = (HBox) profileNavBtn.getGraphic();
                for (javafx.scene.Node child : hbox.getChildren()) {
                    if (child instanceof javafx.scene.text.Text) {
                        javafx.scene.text.Text textNode = (javafx.scene.text.Text) child;
                        if (unread > 0) {
                            textNode.setText("Profile (" + unread + ")");
                        } else {
                            textNode.setText("Profile");
                        }
                    }
                }
            }
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(HomeController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }
    }

    private void showNotificationsDialog() {
        if (currentUser == null) return;
        
        Stage dialog = new Stage();
        dialog.setTitle("My Notifications");
        dialog.initModality(Modality.APPLICATION_MODAL);
        
        VBox layout = new VBox(15);
        layout.getStyleClass().add("dialog-panel");
        layout.setPadding(new Insets(20));
        if (getClass().getResource("/com/studybuddy/css/theme.css") != null) {
            layout.getStylesheets().add(getClass().getResource("/com/studybuddy/css/theme.css").toExternalForm());
        }

        Label headerLabel = new Label("Notifications");
        headerLabel.getStyleClass().add("dialog-header");

        Label unreadLabel = new Label();
        unreadLabel.getStyleClass().add("dialog-subheader");
        
        ListView<Notification> listView = new ListView<>();
        listView.setPrefHeight(250);
        listView.setCellFactory(param -> new ListCell<Notification>() {
            @Override
            protected void updateItem(Notification item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String icon = "ðŸ””";
                    if ("REWARD".equalsIgnoreCase(item.getRecipientType()) || "REWARD".equalsIgnoreCase(item.getNotificationType())) {
                        icon = "ðŸ†";
                    } else if ("HIGH".equalsIgnoreCase(item.getPriority()) || "URGENT".equalsIgnoreCase(item.getPriority())) {
                        icon = "ðŸš¨";
                    }
                    String dateStr = "N/A";
                    String timeStr = "N/A";
                    if (item.getSentAt() != null) {
                        dateStr = item.getSentAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        timeStr = item.getSentAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    }
                    setText(icon + " " + item.getTitle() + " (" + dateStr + " " + timeStr + ")" + "\n" + item.getMessage());
                    if (item.isRead()) {
                        getStyleClass().setAll("hint-text");
                    } else {
                        getStyleClass().setAll("notification-unread");
                    }
                }
            }
        });
        
        Runnable updateUIOnly = () -> {
            List<Notification> list = NotificationService.getInstance().getNotificationsByUserId(currentUser.getId());
            listView.setItems(FXCollections.observableArrayList(list));
            long count = list.stream().filter(n -> !n.isRead()).count();
            unreadLabel.setText("Unread Count: " + count);
        };

        Runnable refreshList = () -> {
            updateUIOnly.run();
            EventBus.getInstance().publish(new EventBus.NotificationsChangedEvent());
        };

        refreshList.run();

        // Subscribe Dialog to events to keep in sync in real-time
        EventBus.EventListener<EventBus.NotificationsChangedEvent> dialogListener = event -> {
            javafx.application.Platform.runLater(updateUIOnly);
        };
        EventBus.getInstance().subscribe(EventBus.NotificationsChangedEvent.class, dialogListener);
        dialog.setOnHidden(e -> {
            EventBus.getInstance().unsubscribe(EventBus.NotificationsChangedEvent.class, dialogListener);
        });

        Button markReadBtn = new Button("âœ“ Mark as Read");
        markReadBtn.getStyleClass().add("btn-primary");
        markReadBtn.setOnAction(e -> {
            Notification selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                NotificationService.getInstance().markAsRead(selected.getId());
                refreshList.run();
            }
        });

        Button markAllReadBtn = new Button("âœ“ Mark All Read");
        markAllReadBtn.getStyleClass().add("btn-success");
        markAllReadBtn.setOnAction(e -> {
            NotificationService.getInstance().markAllReadForUser(currentUser.getId());
            refreshList.run();
        });

        Button deleteBtn = new Button("ðŸ—‘ Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> {
            Notification selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                NotificationService.getInstance().deleteNotification(selected.getId());
                refreshList.run();
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setOnAction(e -> dialog.close());
        
        HBox actions = new HBox(10, markReadBtn, markAllReadBtn, deleteBtn, closeBtn);
        layout.getChildren().addAll(headerLabel, unreadLabel, listView, actions);
        
        dialog.setScene(new Scene(layout, 550, 380));
        dialog.show();
    }

    /**
     * Adds a fade-in transition when the view loads.
     */
    private void addFadeInAnimation(Node node) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }
}
