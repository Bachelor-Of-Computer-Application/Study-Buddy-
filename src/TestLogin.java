public class TestLogin {

    public static void main(String[] args) {

        AuthDAO dao = new AuthDAO();

        String username = "sampada";
        String password = "1234";

        boolean ok = dao.loginUser(username, password);

        if (ok) {
            System.out.println("Login Successful!");
        } else {
            System.out.println("Invalid Username or Password");
        }
    }
}