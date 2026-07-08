package com.studybuddy.dao;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Singleton database connection manager for StudyBuddy.
 *
 * <p><b>Key design decisions:</b></p>
 * <ul>
 *   <li>{@link #initialize()} eagerly opens the physical SQL Server connection once
 *       (called from {@code App.start()} at application startup). Repeated calls are
 *       safe — a new connection is only created if the current one is null, closed, or
 *       invalid. Debug output is suppressed on re-validation calls.</li>
 *   <li>{@link #getConnection()} returns a <em>non-closing proxy</em> {@link Connection}.
 *       Because all DAOs wrap the connection in a {@code try-with-resources} block, the
 *       JVM would otherwise call {@code conn.close()} at the end of each method — which
 *       would close the shared singleton and force a reconnect on every subsequent call.
 *       The proxy intercepts {@code close()} and makes it a no-op, while delegating every
 *       other method to the real physical connection.</li>
 *   <li>{@link #closeConnection()} closes the real physical connection and should only
 *       be called on application shutdown.</li>
 * </ul>
 */
public class DatabaseConnection {

    /** The single physical connection shared across the entire application. */
    private static Connection realConnection;

    private static final Logger logger =
            Logger.getLogger(DatabaseConnection.class.getName());

    // ── Connection configuration ──────────────────────────────────────────────

    private static final String DB_URL =
            "jdbc:sqlserver://localhost:1433;databaseName=StudyBuddy;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "studybuddy";
    private static final String DB_PASSWORD = "StudyBuddy123";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialises the physical database connection if it is not already open.
     *
     * <p>Safe to call multiple times — a new {@link Connection} is created only when
     * the existing one is {@code null}, closed, or fails the 2-second validity check.
     * Log output is emitted only on an actual (re-)connect, not on every validation.</p>
     */
    public static synchronized void initialize() {
        try {
            if (realConnection == null
                    || realConnection.isClosed()
                    || !realConnection.isValid(2)) {

                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

                realConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

                logger.info("✅ Database Connected Successfully");
            }
        } catch (Exception e) {
            logger.severe("❌ Database Connection Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Returns a <em>non-closing</em> proxy wrapping the shared physical connection.
     *
     * <p>The proxy delegates every {@link Connection} method to the real connection
     * <em>except</em> {@code close()} — which is silently swallowed. This allows all
     * DAO methods to safely use {@code try-with-resources} without inadvertently
     * closing (and thus invalidating) the singleton connection.</p>
     *
     * <p>If the connection is not yet open (or has become stale), {@link #initialize()}
     * is called automatically before the proxy is returned.</p>
     *
     * @return a non-closing {@link Connection} proxy backed by the shared physical connection
     */
    public static Connection getConnection() {
        try {
            if (realConnection == null
                    || realConnection.isClosed()
                    || !realConnection.isValid(2)) {
                initialize();
            }
        } catch (SQLException e) {
            logger.warning("Connection validation failed, attempting re-initialise: " + e.getMessage());
            initialize();
        }

        return buildNonClosingProxy(realConnection);
    }

    /**
     * Closes the real physical connection.
     *
     * <p>Should only be called once, on application shutdown.</p>
     */
    public static void closeConnection() {
        try {
            if (realConnection != null && !realConnection.isClosed()) {
                realConnection.close();
                logger.info("🔒 Database Connection Closed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Wraps the supplied physical {@link Connection} in a JDK dynamic proxy whose
     * {@code close()} method is a deliberate no-op.  All other {@link Connection}
     * methods are forwarded transparently to the delegate.
     *
     * @param delegate the real physical connection
     * @return a non-closing {@link Connection} proxy
     */
    private static Connection buildNonClosingProxy(Connection delegate) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // Intercept close() — silently ignore it so the shared singleton
                // is never closed by a try-with-resources block in a DAO.
                if ("close".equals(method.getName())) {
                    return null;
                }
                if (delegate == null) {
                    throw new SQLException(
                            "Database connection is unavailable. Verify SQL Server is running and credentials are correct.");
                }
                // isClosed() must reflect the real connection's state so callers
                // can still guard against a genuinely closed connection.
                return method.invoke(delegate, args);
            }
        };

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                handler
        );
    }
}