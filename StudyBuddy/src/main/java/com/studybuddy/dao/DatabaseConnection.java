package com.studybuddy.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static Connection connection;

    private static final Logger logger =
            Logger.getLogger(DatabaseConnection.class.getName());

    // SQL Server Connection URL
    private static final String DB_URL =
            "jdbc:sqlserver://localhost:1433;" +
                    "databaseName=StudyBuddy;" +
                    "encrypt=true;" +
                    "trustServerCertificate=true;";

    // Change these if your SQL login is different
    private static final String DB_USER = "studybuddy";
    private static final String DB_PASSWORD = "StudyBuddy123";
    /**
     * Initialize database connection
     */
    public static synchronized void initialize() {

        try {

            if (connection == null
                    || connection.isClosed()
                    || !connection.isValid(2)) {

                // Load SQL Server JDBC Driver
                Class.forName(
                        "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                );

                connection = DriverManager.getConnection(
                        DB_URL,
                        DB_USER,
                        DB_PASSWORD
                );

                logger.info("✅ Database Connected Successfully");
            }

        } catch (Exception e) {

            logger.severe("❌ Database Connection Failed");
            e.printStackTrace();
        }
    }

    /**
     * Get connection object
     */
    public static Connection getConnection() {

        try {

            if (connection == null
                    || connection.isClosed()
                    || !connection.isValid(2)) {

                initialize();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return connection;
    }

    /**
     * Close database connection
     */
    public static void closeConnection() {

        try {

            if (connection != null
                    && !connection.isClosed()) {

                connection.close();

                logger.info("🔒 Database Connection Closed");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}