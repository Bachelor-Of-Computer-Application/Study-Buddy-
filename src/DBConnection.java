import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        try {

            String url = "jdbc:sqlserver://localhost:1433;databaseName=StudyBuddy;encrypt=true;trustServerCertificate=true";
            String user = "studybuddy";
            String password = "StudyBuddy@123";

            Connection conn = DriverManager.getConnection(url, user, password);

            System.out.println("Database Connected Successfully!");

            return conn;

        } catch (Exception e) {
            System.out.println("Connection Failed!");
            e.printStackTrace();
            return null;
        }
    }
}