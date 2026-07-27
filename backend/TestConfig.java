import java.sql.*;
public class TestConfig {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:5432/swipelab";
        String user = "postgres";
        String pass = "postgres";
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT config_key, config_value FROM system_configuration")) {
            while (rs.next()) {
                System.out.println(rs.getString(1) + " = " + rs.getString(2));
            }
        }
    }
}
