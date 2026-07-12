package com.studybuddy.controllers;

import com.studybuddy.App;
import com.studybuddy.models.Achievement;
import com.studybuddy.models.User;
import com.studybuddy.services.AchievementService;
import com.studybuddy.utils.EventBus;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AchievementsController implements Initializable {

    @FXML private Button refreshBtn;
    @FXML private Label totalAchievementsLabel;
    @FXML private Label unlockedAchievementsLabel;
    @FXML private Label totalRewardPointsLabel;
    @FXML private FlowPane unlockedAchievementsContainer;
    @FXML private FlowPane lockedAchievementsContainer;

    private final AchievementService achievementService = AchievementService.getInstance();
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = App.getCurrentUser();
        refreshAchievements();

        // Subscribe to EventBus events
        EventBus.getInstance().subscribe(EventBus.StatisticsChangedEvent.class, 
            (_event) -> javafx.application.Platform.runLater(this::refreshAchievements));
        EventBus.getInstance().subscribe(EventBus.PointsChangedEvent.class, 
            (event) -> javafx.application.Platform.runLater(() -> {
                if (currentUser != null && event.getUserId() == currentUser.getId()) {
                    refreshAchievements();
                }
            }));
    }

    @FXML
    public void refreshAchievements() {
        currentUser = App.getCurrentUser();
        if (currentUser == null) {
            totalAchievementsLabel.setText("0");
            unlockedAchievementsLabel.setText("0");
            totalRewardPointsLabel.setText("0");
            unlockedAchievementsContainer.getChildren().clear();
            lockedAchievementsContainer.getChildren().clear();
            return;
        }

        int userId = currentUser.getId();

        try {
            // Get all achievements for user
            List<Achievement> achievements = achievementService.getAchievementsForUser(userId);
            
            // Update stats
            int unlockedCount = achievementService.countUnlockedAchievements(userId);
            int totalRewardPoints = achievementService.getTotalRewardPoints(userId);
            
            totalAchievementsLabel.setText(String.valueOf(achievements.size()));
            unlockedAchievementsLabel.setText(String.valueOf(unlockedCount));
            totalRewardPointsLabel.setText(String.valueOf(totalRewardPoints));

            // Clear containers
            unlockedAchievementsContainer.getChildren().clear();
            lockedAchievementsContainer.getChildren().clear();

            // Populate containers
            for (Achievement achievement : achievements) {
                VBox card = createAchievementCard(achievement);
                if (achievement.isUnlocked()) {
                    unlockedAchievementsContainer.getChildren().add(card);
                } else {
                    lockedAchievementsContainer.getChildren().add(card);
                }
            }

        } catch (Exception e) {
            System.err.println("[AchievementsController] Failed to load achievements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createAchievementCard(Achievement achievement) {
        VBox card = new VBox( 12);
        card.setPrefWidth(280);
        card.getStyleClass().add("achievement-card");
        
        // Apply styles based on status
        if (achievement.isUnlocked()) {
            card.setStyle("-fx-border-color: #22c55e; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-background-color: #f0fdf4;");
        } else {
            card.setStyle("-fx-border-color: #d1d5db; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-background-color: #f9fafb; -fx-opacity: 0.7;");
        }

        // Header with icon, name, and ribbon for completed
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(achievement.isUnlocked() ? "✅" : achievement.getIcon());
        iconLabel.setStyle("-fx-font-size: 32px;");
        
        Label nameLabel = new Label(achievement.getName());
        nameLabel.getStyleClass().add("achievement-name");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        if (achievement.isUnlocked()) {
            nameLabel.setStyle("-fx-text-fill: #166534; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            nameLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Completed ribbon
        if (achievement.isUnlocked()) {
            Label ribbonLabel = new Label("COMPLETED");
            ribbonLabel.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 4px;");
            header.getChildren().addAll(iconLabel, nameLabel, spacer, ribbonLabel);
        } else {
            header.getChildren().addAll(iconLabel, nameLabel, spacer);
        }

        // Description
        Label descLabel = new Label(achievement.getDescription());
        descLabel.getStyleClass().add("achievement-description");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(260);
        descLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12px;");

        // Progress
        VBox progressBox = new VBox(4);
        Label progressLabel = new Label(String.format("Progress: %d/%d (%d%%)",
                achievement.getCurrentProgress(),
                achievement.getTargetProgress(),
                achievement.getProgressPercentage()));
        progressLabel.getStyleClass().add("achievement-progress-text");
        progressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        
        javafx.scene.control.ProgressBar progressBar = new javafx.scene.control.ProgressBar();
        double progressValue = (double) achievement.getCurrentProgress() / achievement.getTargetProgress();
        progressBar.setProgress(progressValue);
        progressBar.getStyleClass().add("achievement-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        
        if (achievement.isUnlocked()) {
            progressBar.setStyle("-fx-accent: #22c55e;");
        } else {
            progressBar.setStyle("-fx-accent: #3b82f6;");
        }
        
        progressBox.getChildren().addAll(progressLabel, progressBar);

        // Reward points
        Label rewardLabel = new Label("🎁 Reward: " + achievement.getRewardPoints() + " pts");
        rewardLabel.getStyleClass().add("achievement-reward");
        rewardLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #059669;");
        
        // Status badge
        Label statusLabel;
        if (achievement.isUnlocked()) {
            statusLabel = new Label("✨ COMPLETED");
            statusLabel.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6px 12px; -fx-background-radius: 12px;");
        } else {
            statusLabel = new Label("🔒 IN PROGRESS");
            statusLabel.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6px 12px; -fx-background-radius: 12px;");
        }

        card.getChildren().addAll(header, descLabel, progressBox, rewardLabel, statusLabel);
        card.setPadding(new javafx.geometry.Insets(16));

        return card;
    }

    @FXML
    public void handleGoToProfile() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/studybuddy/fxml/ProfileView.fxml"));
            javafx.scene.Parent view = loader.load();
            
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) 
                refreshBtn.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            } else {
                javafx.scene.layout.BorderPane mainBorderPane = 
                    (javafx.scene.layout.BorderPane) refreshBtn.getScene().getRoot();
                mainBorderPane.setCenter(view);
            }
        } catch (Exception e) {
            System.err.println("[AchievementsController] Failed to navigate to profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
