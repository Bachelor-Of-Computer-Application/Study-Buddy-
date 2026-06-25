package com.studybuddy.utils;

import com.studybuddy.App;
import com.studybuddy.models.User;

public class SessionManager {
    private static SessionManager instance;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        App.setCurrentUser(user);
    }

    public void logout() {
        App.setCurrentUser(null);
    }

    public User getCurrentUser() {
        return App.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return App.getCurrentUser() != null;
    }
}
