
package com.studybuddy.models;

import java.time.LocalDateTime;

/**
 * Model class representing an achievable achievement in the Study Buddy app.
 */
public class Achievement {
    private String id;           // Unique identifier for the achievement
    private String name;         // Display name of the achievement
    private String description;  // Description of what to do to unlock
    private String icon;         // Emoji/icon for the achievement
    private int currentProgress; // User's current progress towards the goal
    private int targetProgress;  // Target progress to unlock
    private boolean unlocked;    // Whether the user has unlocked it
    private LocalDateTime unlockedAt; // When it was unlocked (if applicable)

    public Achievement() {}

    public Achievement(String id, String name, String description, String icon, int targetProgress) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.targetProgress = targetProgress;
        this.currentProgress = 0;
        this.unlocked = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public int getCurrentProgress() { return currentProgress; }
    public void setCurrentProgress(int currentProgress) { this.currentProgress = currentProgress; }
    public int getTargetProgress() { return targetProgress; }
    public void setTargetProgress(int targetProgress) { this.targetProgress = targetProgress; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
    
    /**
     * Returns the progress percentage (0-100)
     */
    public int getProgressPercentage() {
        if (targetProgress == 0) return 0;
        return Math.min(100, (currentProgress * 100) / targetProgress);
    }
}
