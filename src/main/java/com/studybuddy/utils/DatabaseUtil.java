package com.studybuddy.utils;

import com.studybuddy.dao.DatabaseConnection;
import java.sql.Connection;

public class DatabaseUtil {
    public static Connection getConnection() {
        return DatabaseConnection.getConnection();
    }
}
