package com.studybuddy.dao;

import java.sql.*;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static Connection connection;

    // =========================
    // LOGGER
    // =========================
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());

    // =========================
    // CONFIG (ENVIRONMENT BASED)
    // =========================
    private static final String DB_URL =
            "jdbc:sqlserver://localhost:1433;"
                    + "databaseName=StudyBuddy;"
                    + "encrypt=true;"
                    + "trustServerCertificate=true;";

    private static final String DB_USER = System.getenv("DB_USER") != null
            ? System.getenv("DB_USER")
            : "studybuddy";

    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD") != null
            ? System.getenv("DB_PASSWORD")
            : "StudyBuddy123";

    // =========================
    // MOCK MODE (FOR TESTING UI)
    // =========================
    private static final boolean USE_MOCK = false;

    // =========================
    // INITIALIZE CONNECTION
    // =========================
    public static synchronized void initialize() {
        try {
            if (USE_MOCK) {
                connection = createMockConnection();
                logger.info("⚠ Using MOCK database connection");
                return;
            }

            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                logger.info("✅ MSSQL Database Connected Successfully!");
            }

        } catch (SQLException e) {
            logger.severe("❌ Database Connection Failed!");
            e.printStackTrace();
        }
    }

    // =========================
    // GET CONNECTION
    // =========================
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                initialize();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return connection;
    }

    // =========================
    // CLOSE CONNECTION
    // =========================
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("🔒 Database Connection Closed!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // MOCK CONNECTION
    // =========================
    private static Connection createMockConnection() {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {

                        String name = method.getName();

                        try {
                            if (name.equals("prepareStatement") || name.equals("createStatement")) {
                                return createMockStatement();
                            }

                            if (name.equals("isClosed")) return false;
                            if (name.equals("isValid")) return true;

                            if (name.equals("close")
                                    || name.equals("commit")
                                    || name.equals("rollback")
                                    || name.equals("setAutoCommit")) {
                                return null;
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return null;
                    }
                }
        );
    }

    // =========================
    // MOCK STATEMENT
    // =========================
    private static PreparedStatement createMockStatement() {
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {

                        String name = method.getName();

                        if (name.equals("executeQuery")) {
                            return createMockResultSet();
                        }

                        if (name.equals("executeUpdate")) return 1;
                        if (name.equals("execute")) return true;

                        if (name.equals("setString")
                                || name.equals("setInt")
                                || name.startsWith("set")) {
                            return null;
                        }

                        if (name.equals("close")) return null;

                        return null;
                    }
                }
        );
    }

    // =========================
    // MOCK RESULT SET
    // =========================
    private static ResultSet createMockResultSet() {
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                new InvocationHandler() {

                    private int row = 0;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {

                        String name = method.getName();

                        if (name.equals("next")) {
                            return ++row <= 1;
                        }

                        if (name.equals("close")) return null;
                        if (name.equals("wasNull")) return false;

                        Class<?> type = method.getReturnType();

                        if (type == String.class) {
                            if (args != null && args.length > 0) {
                                String col = args[0].toString().toLowerCase();

                                if (col.contains("email")) return "student@studybuddy.com";
                                if (col.contains("name")) return "Guest Student";
                                if (col.contains("role")) return "user";
                                if (col.contains("phone")) return "+977-9800000000";
                                if (col.contains("department")) return "Computer Engineering";
                                if (col.contains("semester")) return "Semester 5";
                                if (col.contains("bio")) return "Mock profile data";
                            }
                            return "mock";
                        }

                        if (type == int.class || type == Integer.class) {
                            if (args != null && args.length > 0) {
                                String col = args[0].toString().toLowerCase();

                                if (col.contains("id")) return 1;
                                if (col.contains("points")) return 100;
                                if (col.contains("questions")) return 5;
                                if (col.contains("answers")) return 10;
                            }
                            return 0;
                        }

                        if (type == boolean.class || type == Boolean.class) return true;
                        if (type == double.class || type == Double.class) return 0.0;

                        if (type == Timestamp.class) return new Timestamp(System.currentTimeMillis());
                        if (type == Date.class) return new Date(System.currentTimeMillis());

                        return null;
                    }
                }
        );
    }
}