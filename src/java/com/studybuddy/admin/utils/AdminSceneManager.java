package com.studybuddy.admin.utils;

import com.studybuddy.admin.AdminApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminSceneManager {
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void showLoginPage(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AdminApp.class.getResource("/com/studybuddy/admin/fxml/AdminLoginView.fxml"));
            Parent root = loader.load();
            stage.setTitle("Study Buddy Admin - Login");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showDashboardPage(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AdminApp.class.getResource("/com/studybuddy/admin/fxml/AdminDashboardView.fxml"));
            Parent root = loader.load();
            stage.setTitle("Study Buddy Admin - Dashboard");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
