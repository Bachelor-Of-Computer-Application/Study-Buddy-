package com.studybuddy.admin.utils;

import com.studybuddy.admin.AdminApp;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class AdminSceneManager {
    private static final Logger logger = Logger.getLogger(AdminSceneManager.class.getName());
    private static Stage primaryStage;
    
    // Window size constants
    private static final double MIN_WIDTH = 1280.0;
    private static final double MIN_HEIGHT = 720.0;
    private static final double PREF_WIDTH = 1366.0;
    private static final double PREF_HEIGHT = 768.0;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    private static void configureWindow(Stage stage) {
        // Set minimum size
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        
        // Set preferred size
        stage.setWidth(PREF_WIDTH);
        stage.setHeight(PREF_HEIGHT);
        
        // Center window on screen
        Screen screen = Screen.getPrimary();
        javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
        double centerX = bounds.getMinX() + (bounds.getWidth() - PREF_WIDTH) / 2;
        double centerY = bounds.getMinY() + (bounds.getHeight() - PREF_HEIGHT) / 2;
        
        if (stage.getX() == 0 && stage.getY() == 0) {
            stage.setX(centerX);
            stage.setY(centerY);
        }
        
        // Allow resizing
        stage.setResizable(true);
    }

    public static void showLoginPage(Stage stage) {
        try {
            logger.fine("AdminLoginView.fxml loaded");
            FXMLLoader loader = new FXMLLoader(
                    AdminApp.class.getResource("/com/studybuddy/admin/fxml/AdminLoginView.fxml"));
            Parent root = loader.load();
            stage.setTitle("Study Buddy Admin - Login");
            stage.setScene(new Scene(root));
            configureWindow(stage);
            stage.show();
        } catch (Exception e) {
            logger.severe("Failed to load AdminLoginView.fxml: " + e.getMessage());
            java.util.logging.Logger.getLogger(AdminSceneManager.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }
    }

    public static void showDashboardPage(Stage stage) {
        try {
            logger.fine("AdminDashboardView.fxml loaded");
            FXMLLoader loader = new FXMLLoader(
                    AdminApp.class.getResource("/com/studybuddy/admin/fxml/AdminDashboardView.fxml"));
            Parent root = loader.load();
            stage.setTitle("Study Buddy Admin - Dashboard");
            stage.setScene(new Scene(root));
            configureWindow(stage);
            stage.show();
        } catch (Exception e) {
            logger.severe("Failed to load AdminDashboardView.fxml: " + e.getMessage());
            java.util.logging.Logger.getLogger(AdminSceneManager.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }
    }
}