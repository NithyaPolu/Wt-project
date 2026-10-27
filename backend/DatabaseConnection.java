import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/skilllink";
        String username = "root";
        String password = "Nithya@5002My";

        try {

            Connection con = DriverManager.getConnection(url, username, password);

            System.out.println("Database connected successfully!");

            con.close();

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
}