package com.studybuddy.models;

import java.time.LocalDateTime;

/**
 * Model representing a single application-wide setting stored in the Settings table.
 */
public class AppSetting {

    private String settingKey;
    private String settingValue;
    private LocalDateTime updatedAt;

    public AppSetting() {}

    public AppSetting(String settingKey, String settingValue) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getSettingKey() { return settingKey; }
    public String getSettingValue() { return settingValue; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return settingKey + " = " + settingValue;
    }
}
