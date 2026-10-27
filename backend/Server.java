
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class Server {

    static String url = "jdbc:mysql://localhost:3306/skilllink";
    static String username = "root";
    static String dbPassword = "Nithya@5002My";

    static Connection getConnection() throws Exception {
        return DriverManager.getConnection(url, username, dbPassword);
    }

    static Map<String, String> readData(String data) {
        Map<String, String> values = new HashMap<>();

        for (String pair : data.split("&")) {
            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {
                String key = URLDecoder.decode(
                        parts[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(
                        parts[1], StandardCharsets.UTF_8);

                values.put(key, value);
            }
        }

        return values;
    }

    static Map<String, String> readBody(HttpExchange exchange)
            throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            String data = new String(
                    input.readAllBytes(), StandardCharsets.UTF_8);
            return readData(data);
        }
    }

    static Map<String, String> readQuery(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();

        if (query == null) {
            return new HashMap<>();
        }

        return readData(query);
    }

    static String value(Map<String, String> data, String key) {
        return data.getOrDefault(key, "").trim();
    }

    static void send(HttpExchange exchange, int status, String message)
            throws IOException {
        byte[] response = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(status, response.length);

        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    static void handle(HttpExchange exchange, Handler handler)
            throws IOException {
        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.getResponseHeaders().set(
                        "Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set(
                        "Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set(
                        "Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            handler.run(exchange);

        } catch (Exception e) {
            e.printStackTrace();

            try {
                send(exchange, 500, "Server error. Check the terminal.");
            } catch (Exception ignored) {
                exchange.close();
            }
        }
    }

    interface Handler {
        void run(HttpExchange exchange) throws Exception;
    }

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(8080), 0);

            server.createContext("/register", exchange -> handle(exchange, ex -> {
                if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
                    send(ex, 405, "POST method required.");
                    return;
                }

                Map<String, String> data = readBody(ex);

                String name = value(data, "name");
                String email = value(data, "email");
                String password = value(data, "password");
                String teachSkill = value(data, "teachSkill");
                String learnSkill = value(data, "learnSkill");

                if (name.isEmpty() || email.isEmpty()
                        || password.isEmpty()
                        || teachSkill.isEmpty()
                        || learnSkill.isEmpty()) {
                    send(ex, 400, "Please fill all fields.");
                    return;
                }

                try (Connection con = getConnection();
                        PreparedStatement ps = con.prepareStatement(
                                "INSERT INTO users " +
                                        "(name, email, password, teach_skill, learn_skill) " +
                                        "VALUES (?, ?, ?, ?, ?)")) {

                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setString(3, password);
                    ps.setString(4, teachSkill);
                    ps.setString(5, learnSkill);

                    ps.executeUpdate();
                    send(ex, 200, "Registration successful!");
                } catch (java.sql.SQLException e) {
                    if (e.getErrorCode() == 1062) {
                        send(ex, 409, "Email already registered.");
                    } else {
                        throw e;
                    }
                }
            }));

            server.createContext("/login", exchange -> handle(exchange, ex -> {
                if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
                    send(ex, 405, "POST method required.");
                    return;
                }

                Map<String, String> data = readBody(ex);
                String email = value(data, "email");
                String password = value(data, "password");

                try (Connection con = getConnection();
                        PreparedStatement ps = con.prepareStatement(
                                "SELECT email FROM users " +
                                        "WHERE email = ? AND password = ?")) {

                    ps.setString(1, email);
                    ps.setString(2, password);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            send(ex, 200, "Login successful!");
                        } else {
                            send(ex, 200,
                                    "Invalid email or password.");
                        }
                    }
                }
            }));

            server.createContext("/profile", exchange -> handle(exchange, ex -> {
                String email = value(readQuery(ex), "email");

                try (Connection con = getConnection();
                        PreparedStatement ps = con.prepareStatement(
                                "SELECT name, email, teach_skill, learn_skill " +
                                        "FROM users WHERE email = ?")) {

                    ps.setString(1, email);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String response = rs.getString("name") + "|" +
                                    rs.getString("email") + "|" +
                                    rs.getString("teach_skill") + "|" +
                                    rs.getString("learn_skill");

                            send(ex, 200, response);
                        } else {
                            send(ex, 200, "User not found");
                        }
                    }
                }
            }));

            server.createContext("/students", exchange -> handle(exchange, ex -> {
                String email = value(readQuery(ex), "email");

                try (Connection con = getConnection();
                        PreparedStatement ps = con.prepareStatement(
                                "SELECT u2.name, u2.email, u2.teach_skill " +
                                        "FROM users u1 JOIN users u2 " +
                                        "ON LOWER(TRIM(u1.learn_skill)) = " +
                                        "LOWER(TRIM(u2.teach_skill)) " +
                                        "WHERE u1.email = ? AND u1.email <> u2.email")) {

                    ps.setString(1, email);

                    StringBuilder response = new StringBuilder();

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            response.append(rs.getString("name"))
                                    .append("|")
                                    .append(rs.getString("email"))
                                    .append("|")
                                    .append(rs.getString("teach_skill"))
                                    .append("||");
                        }
                    }

                    send(ex, 200, response.toString());
                }
            }));

            server.createContext("/request", exchange -> handle(exchange, ex -> {
                if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
                    send(ex, 405, "POST method required.");
                    return;
                }

                Map<String, String> data = readBody(ex);
                String senderEmail = value(data, "senderEmail");
                String receiverEmail = value(data, "receiverEmail");

                if (senderEmail.isEmpty()
                        || receiverEmail.isEmpty()) {
                    send(ex, 400, "Missing email.");
                    return;
                }

                if (senderEmail.equalsIgnoreCase(receiverEmail)) {
                    send(ex, 400, "You cannot request yourself.");
                    return;
                }

                try (Connection con = getConnection()) {
                    String checkSql = "SELECT status FROM requests " +
                            "WHERE (sender_email = ? AND receiver_email = ?) " +
                            "OR (sender_email = ? AND receiver_email = ?)";

                    try (PreparedStatement check = con.prepareStatement(
                            checkSql)) {
                        check.setString(1, senderEmail);
                        check.setString(2, receiverEmail);
                        check.setString(3, receiverEmail);
                        check.setString(4, senderEmail);

                        try (ResultSet rs = check.executeQuery()) {
                            if (rs.next()) {
                                String status = rs.getString("status");

                                if ("Accepted".equals(status)) {
                                    send(ex, 200, "Already connected!");
                                } else {
                                    send(ex, 200, "Request already exists!");
                                }
                                return;
                            }
                        }
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO requests " +
                                    "(sender_email, receiver_email, status) " +
                                    "VALUES (?, ?, 'Pending')")) {

                        ps.setString(1, senderEmail);
                        ps.setString(2, receiverEmail);
                        ps.executeUpdate();
                    }

                    send(ex, 200, "Request sent successfully!");
                }
            }));

            server.createContext("/requests", exchange -> handle(exchange, ex -> {
                String email = value(readQuery(ex), "email");

                try (Connection con = getConnection();
                        PreparedStatement ps = con.prepareStatement(
                                "SELECT sender_email, status FROM requests " +
                                        "WHERE receiver_email = ? ORDER BY sender_email")) {

                    ps.setString(1, email);

                    StringBuilder response = new StringBuilder();

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            response.append(rs.getString("sender_email"))
                                    .append("|")
                                    .append(rs.getString("status"))
                                    .append("||");
                        }
                    }

                    send(ex, 200, response.toString());
                }
            }));

            server.createContext("/updateRequest", exchange -> handle(exchange, ex -> {
                if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
                    send(ex, 405, "POST method required.");
                    return;
                }

                Map<String, String> data = readBody(ex);
                String senderEmail = value(data, "senderEmail");
                String receiverEmail = value(data, "receiverEmail");
                String status = value(data, "status");

                if (!status.equals("Accepted")
                        && !status.equals("Rejected")) {
                    send(ex, 400, "Invalid request status.");
                    return;
                }

                try (Connection con = getConnection();
                        PreparedStatement ps = con.prepareStatement(
                                "UPDATE requests SET status = ? " +
                                        "WHERE sender_email = ? AND receiver_email = ? " +
                                        "AND status = 'Pending'")) {

                    ps.setString(1, status);
                    ps.setString(2, senderEmail);
                    ps.setString(3, receiverEmail);

                    int updated = ps.executeUpdate();

                    if (updated > 0) {
                        send(ex, 200, "Request updated successfully!");
                    } else {
                        send(ex, 200,
                                "Pending request not found. Refresh the page.");
                    }
                }
            }));

            server.createContext("/requestStatus", exchange -> handle(exchange, ex -> {
                Map<String, String> data = readQuery(ex);
                String senderEmail = value(data, "senderEmail");
                String receiverEmail = value(data, "receiverEmail");

                try (Connection con = getConnection();
                        PreparedStatement ps = con.prepareStatement(
                                "SELECT status FROM requests " +
                                        "WHERE sender_email = ? AND receiver_email = ? " +
                                        "UNION ALL " +
                                        "SELECT status FROM requests " +
                                        "WHERE sender_email = ? AND receiver_email = ? " +
                                        "ORDER BY FIELD(status, 'Accepted', 'Pending', 'Rejected') " +
                                        "LIMIT 1")) {

                    ps.setString(1, senderEmail);
                    ps.setString(2, receiverEmail);
                    ps.setString(3, receiverEmail);
                    ps.setString(4, senderEmail);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            send(ex, 200, rs.getString("status"));
                        } else {
                            send(ex, 200, "None");
                        }
                    }
                }
            }));

            server.createContext("/connections", exchange -> handle(exchange, ex -> {
                String email = value(readQuery(ex), "email");

                try (Connection con = getConnection();
                        PreparedStatement ps = con.prepareStatement(
                                "SELECT DISTINCT u.name, u.email, u.teach_skill " +
                                        "FROM requests r JOIN users u ON u.email = " +
                                        "CASE WHEN r.sender_email = ? " +
                                        "THEN r.receiver_email ELSE r.sender_email END " +
                                        "WHERE (r.sender_email = ? OR r.receiver_email = ?) " +
                                        "AND r.status = 'Accepted'")) {

                    ps.setString(1, email);
                    ps.setString(2, email);
                    ps.setString(3, email);

                    StringBuilder response = new StringBuilder();

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            response.append(rs.getString("name"))
                                    .append("|")
                                    .append(rs.getString("email"))
                                    .append("|")
                                    .append(rs.getString("teach_skill"))
                                    .append("||");
                        }
                    }

                    send(ex, 200, response.toString());
                }
            }));

            server.createContext("/doubtConnections", exchange -> handle(exchange, ex -> {
                String email = value(readQuery(ex), "email");

                try (Connection con = getConnection();
                        PreparedStatement ps = con.prepareStatement(
                                "SELECT DISTINCT u.name, u.email " +
                                        "FROM requests r JOIN users u ON u.email = " +
                                        "CASE WHEN r.sender_email = ? " +
                                        "THEN r.receiver_email ELSE r.sender_email END " +
                                        "WHERE (r.sender_email = ? OR r.receiver_email = ?) " +
                                        "AND r.status = 'Accepted'")) {

                    ps.setString(1, email);
                    ps.setString(2, email);
                    ps.setString(3, email);

                    StringBuilder response = new StringBuilder();

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            response.append(rs.getString("name"))
                                    .append("|")
                                    .append(rs.getString("email"))
                                    .append("||");
                        }
                    }

                    send(ex, 200, response.toString());
                }
            }));

            server.createContext("/messages", exchange -> handle(exchange, ex -> {
                String email = value(readQuery(ex), "email");
                String otherEmail = value(readQuery(ex), "otherEmail");

                try (Connection con = getConnection()) {
                    if (!areConnected(con, email, otherEmail)) {
                        send(ex, 403, "You must be connected to view messages.");
                        return;
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT sender_email, receiver_email, message, created_at " +
                                    "FROM messages WHERE " +
                                    "(sender_email = ? AND receiver_email = ?) OR " +
                                    "(sender_email = ? AND receiver_email = ?) " +
                                    "ORDER BY id")) {

                        ps.setString(1, email);
                        ps.setString(2, otherEmail);
                        ps.setString(3, otherEmail);
                        ps.setString(4, email);

                        StringBuilder response = new StringBuilder();

                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                response.append(rs.getString("sender_email"))
                                        .append("|")
                                        .append(rs.getString("receiver_email"))
                                        .append("|")
                                        .append(rs.getString("message").replace("|", " "))
                                        .append("|")
                                        .append(rs.getTimestamp("created_at"))
                                        .append("||");
                            }
                        }

                        send(ex, 200, response.toString());
                    }
                }
            }));

            server.createContext("/sendMessage", exchange -> handle(exchange, ex -> {
                if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
                    send(ex, 405, "POST method required.");
                    return;
                }

                Map<String, String> data = readBody(ex);
                String senderEmail = value(data, "senderEmail");
                String receiverEmail = value(data, "receiverEmail");
                String message = value(data, "message");

                if (senderEmail.isEmpty() || receiverEmail.isEmpty()
                        || message.trim().isEmpty()) {
                    send(ex, 400, "Please enter a message.");
                    return;
                }

                try (Connection con = getConnection()) {
                    if (!areConnected(con, senderEmail, receiverEmail)) {
                        send(ex, 403, "You must be connected to send messages.");
                        return;
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO messages " +
                                    "(sender_email, receiver_email, message) VALUES (?, ?, ?)")) {

                        ps.setString(1, senderEmail);
                        ps.setString(2, receiverEmail);
                        ps.setString(3, message.trim());
                        ps.executeUpdate();
                    }

                    send(ex, 200, "Message sent successfully!");
                }
            }));

            server.createContext("/addVideo", exchange -> handle(exchange, ex -> {
                if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
                    send(ex, 405, "POST method required.");
                    return;
                }

                Map<String, String> data = readBody(ex);

                String teacherEmail = value(data, "teacherEmail");
                String title = value(data, "title");
                String skill = value(data, "skill");
                String videoUrl = value(data, "videoUrl");

                if (teacherEmail.isEmpty() || title.isEmpty()
                        || skill.isEmpty() || videoUrl.isEmpty()) {
                    send(ex, 400, "Please fill all fields.");
                    return;
                }

                try (Connection con = getConnection()) {
                    try (PreparedStatement check = con.prepareStatement(
                            "SELECT email FROM users WHERE email = ?")) {
                        check.setString(1, teacherEmail);

                        try (ResultSet rs = check.executeQuery()) {
                            if (!rs.next()) {
                                send(ex, 403, "Please register or log in first.");
                                return;
                            }
                        }
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO videos " +
                                    "(teacher_email, skill, title, video_url) " +
                                    "VALUES (?, ?, ?, ?)")) {

                        ps.setString(1, teacherEmail);
                        ps.setString(2, skill);
                        ps.setString(3, title);
                        ps.setString(4, videoUrl);

                        ps.executeUpdate();
                    }

                    send(ex, 200, "Video added successfully!");
                }
            }));
server.createContext("/videos", exchange -> handle(exchange, ex -> {
    if (!ex.getRequestMethod().equalsIgnoreCase("GET")) {
        send(ex, 405, "GET method required.");
        return;
    }

    String email = value(readQuery(ex), "email");

    if (email.isEmpty()) {
        send(ex, 400, "Login email is required.");
        return;
    }

    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(
             "SELECT title, skill, teacher_email, video_url " +
             "FROM videos WHERE teacher_email = ? OR teacher_email IN (" +
             "SELECT CASE WHEN sender_email = ? THEN receiver_email " +
             "ELSE sender_email END FROM requests " +
             "WHERE (sender_email = ? OR receiver_email = ?) " +
             "AND status = 'Accepted') ORDER BY id DESC")) {

        ps.setString(1, email);
        ps.setString(2, email);
        ps.setString(3, email);
        ps.setString(4, email);

        StringBuilder response = new StringBuilder();

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                response.append(rs.getString("title"))
                        .append("|")
                        .append(rs.getString("skill"))
                        .append("|")
                        .append(rs.getString("teacher_email"))
                        .append("|")
                        .append(rs.getString("video_url"))
                        .append("||");
            }
        }

        send(ex, 200, response.toString());
    }
}));
            server.start();

            System.out.println(
                    "SkillLink server started at http://localhost:8080");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean areConnected(
            Connection con, String email1, String email2)
            throws java.sql.SQLException {

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id FROM requests " +
                        "WHERE ((sender_email = ? AND receiver_email = ?) " +
                        "OR (sender_email = ? AND receiver_email = ?)) " +
                        "AND status = 'Accepted'")) {

            ps.setString(1, email1);
            ps.setString(2, email2);
            ps.setString(3, email2);
            ps.setString(4, email1);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
