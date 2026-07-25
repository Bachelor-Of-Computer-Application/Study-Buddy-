package com.studybuddy.admin.services;

import com.studybuddy.admin.dao.SettingsDAO;
import java.util.Map;

/**
 * Service for reading and persisting application settings.
 */
public class SettingsService {

    private static SettingsService instance;
    private final SettingsDAO settingsDAO = SettingsDAO.getInstance();

    private SettingsService() {}

    public static synchronized SettingsService getInstance() {
        if (instance == null) instance = new SettingsService();
        return instance;
    }

    /**
     * Retrieve a single setting value by key. Returns defaultValue if not found.
     */
    public String getSetting(String key, String defaultValue) {
        String value = settingsDAO.getSetting(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Load all settings as a key → value map.
     */
    public Map<String, String> getAllSettings() {
        return settingsDAO.getAllSettings();
    }

    /**
     * Save a batch of settings in one transaction.
     *
     * @return true on success
     */
    public boolean saveAllSettings(Map<String, String> settings) {
        return settingsDAO.saveAllSettings(settings);
    }

    /**
     * Update a single setting.
     *
     * @return true on success
     */
    public boolean updateSetting(String key, String value) {
        return settingsDAO.updateSetting(key, value);
    }
}
