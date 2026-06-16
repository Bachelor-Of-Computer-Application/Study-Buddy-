import javax.swing.*;

public class LoginForm extends JFrame {

    JTextField txtUsername;
    JPasswordField txtPassword;

    public LoginForm() {

        setTitle("StudyBuddy Login");

        JLabel lblUser = new JLabel("Username:");
        JLabel lblPass = new JLabel("Password:");

        txtUsername = new JTextField();
        txtPassword = new JPasswordField();

        JButton btnLogin = new JButton("Login");

        lblUser.setBounds(30, 30, 100, 25);
        txtUsername.setBounds(120, 30, 150, 25);

        lblPass.setBounds(30, 70, 100, 25);
        txtPassword.setBounds(120, 70, 150, 25);

        btnLogin.setBounds(120, 110, 100, 30);

        add(lblUser);
        add(lblPass);
        add(txtUsername);
        add(txtPassword);
        add(btnLogin);

        setLayout(null);
        setSize(320, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        // 🔥 LOGIN BUTTON ACTION
        btnLogin.addActionListener(e -> {

            String username = txtUsername.getText();
            String password = new String(txtPassword.getPassword());

            AuthDAO dao = new AuthDAO();

            boolean ok = dao.loginUser(username, password);

            if (ok) {
                JOptionPane.showMessageDialog(this, "Login Successful!");

                new Dashboard().setVisible(true);
                this.dispose();

            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password");
            }
        });
    }

    public static void main(String[] args) {
        new LoginForm();
    }
}