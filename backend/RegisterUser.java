import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class RegisterUser {

    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/skilllink";
        String username = "root";
        String password = "Nithya@5002My";

        String name = "Nithya";
        String email = "nithya@gmail.com";
        String userPassword = "12345";
        String teachSkill = "Java";
        String learnSkill = "Python";

        try {

            Connection con = DriverManager.getConnection(url, username, password);

            String sql = "INSERT INTO users (name, email, password, teach_skill, learn_skill) VALUES (?, ?, ?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, userPassword);
            ps.setString(4, teachSkill);
            ps.setString(5, learnSkill);

            ps.executeUpdate();

            System.out.println("User registered successfully!");

            con.close();

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
}