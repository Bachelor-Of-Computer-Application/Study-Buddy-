public class TestRegister {

    public static void main(String[] args) {

        AuthDAO dao = new AuthDAO();

        boolean success = dao.registerUser(
                "Sampada Pahari",
                "sampada@gmail.com",
                "sampada",
                "123456"
        );

        if(success) {
            System.out.println("User Registered Successfully!");
        } else {
            System.out.println("Registration Failed!");
        }
    }
}
