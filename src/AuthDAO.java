import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthDAO {

   
    // 🔹 GET CONNECTION
   
    private Connection getConn() {
        return DBConnection.getConnection();
    }

    
    // 🔹 REGISTER USER
   
    public boolean registerUser(String fullName,
                                String email,
                                String username,
                                String password) {

        String sql = "INSERT INTO Users (FullName, Email, Username, Password) VALUES (?, ?, ?, ?)";

        try {
            Connection conn = getConn();

            if (conn == null) {
                System.out.println("Database connection failed!");
                return false;
            }

            PreparedStatement pst = conn.prepareStatement(sql);

            pst.setString(1, fullName);
            pst.setString(2, email);
            pst.setString(3, username);
            pst.setString(4, password);

            return pst.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Register Error: " + e.getMessage());
            return false;
        }
    }

    
    // 🔹 LOGIN USER
   
    public boolean loginUser(String username, String password) {

        String sql = "SELECT * FROM Users WHERE Username=? AND Password=?";

        try {
            Connection conn = getConn();

            if (conn == null) {
                System.out.println("Database connection failed!");
                return false;
            }

            PreparedStatement pst = conn.prepareStatement(sql);

            pst.setString(1, username);
            pst.setString(2, password);

            ResultSet rs = pst.executeQuery();

            return rs.next();

        } catch (Exception e) {
            System.out.println("Login Error: " + e.getMessage());
            return false;
        }
    }
}