package client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;

/**
 * SmtpClient – kết nối TCP tới SMTP Server (cổng 2525),
 * thực hiện AUTH LOGIN, gửi thư theo chuẩn RFC 5321.
 *
 * Cách dùng:
 *   SmtpClient client = new SmtpClient("localhost", 2525);
 *   client.connect();
 *   client.login("huy", "123456");
 *   client.sendMail("huy@mail.local", "an@mail.local", "Tiêu đề", "Nội dung thư");
 *   client.quit();
 */
public class SmtpClient {

    private final String host;
    private final int port;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public SmtpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // ──────────────────────────────────────────────
    // Kết nối & ngắt kết nối
    // ──────────────────────────────────────────────

    /**
     * Mở kết nối TCP tới server, đọc banner "220 Ready".
     */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        // Đọc banner từ server: "220 ..."
        String banner = readLine();
        if (!banner.startsWith("220")) {
            throw new IOException("Kết nối thất bại, server trả: " + banner);
        }

        // Gửi EHLO
        sendCommand("EHLO client");
        // Đọc phản hồi EHLO (có thể nhiều dòng, kết thúc khi dòng không bắt đầu bằng "250-")
        String line;
        do {
            line = readLine();
        } while (line.startsWith("250-"));

        if (!line.startsWith("250")) {
            throw new IOException("EHLO thất bại: " + line);
        }
    }

    /**
     * Đăng nhập bằng AUTH LOGIN (username và password mã hoá Base64).
     */
    public void login(String username, String password) throws IOException {
        sendCommand("AUTH LOGIN");
        String resp = readLine();
        if (!resp.startsWith("334")) {
            throw new IOException("AUTH LOGIN thất bại: " + resp);
        }

        // Gửi username (base64)
        sendCommand(base64(username));
        resp = readLine();
        if (!resp.startsWith("334")) {
            throw new IOException("Gửi username thất bại: " + resp);
        }

        // Gửi password (base64)
        sendCommand(base64(password));
        resp = readLine();
        if (!resp.startsWith("235")) {
            throw new IOException("Đăng nhập thất bại (sai mật khẩu?): " + resp);
        }
    }

    /**
     * Gửi một thư email.
     *
     * @param from    địa chỉ người gửi, phải trùng tài khoản đăng nhập (vd: huy@mail.local)
     * @param to      địa chỉ người nhận (vd: an@mail.local)
     * @param subject tiêu đề thư
     * @param body    nội dung thư (plain text)
     */
    public void sendMail(String from, String to, String subject, String body) throws IOException {
        // MAIL FROM
        sendCommand("MAIL FROM:<" + from + ">");
        assertOk(readLine(), "MAIL FROM");

        // RCPT TO
        sendCommand("RCPT TO:<" + to + ">");
        assertOk(readLine(), "RCPT TO");

        // DATA
        sendCommand("DATA");
        String resp = readLine();
        if (!resp.startsWith("354")) {
            throw new IOException("DATA thất bại: " + resp);
        }

        // Ghi các header RFC 5321 – BẮT BUỘC phải có để thư hiển thị đúng
        String dateStr = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).format(new Date());
        writer.print("From: " + from + "\r\n");
        writer.print("To: " + to + "\r\n");
        writer.print("Subject: " + subject + "\r\n");
        writer.print("Date: " + dateStr + "\r\n");
        writer.print("MIME-Version: 1.0\r\n");
        writer.print("Content-Type: text/plain; charset=UTF-8\r\n");
        writer.print("\r\n"); // Dòng trống ngăn header và body
        writer.flush();

        // Ghi body – áp dụng dot-stuffing (dòng bắt đầu bằng "." → "..")
        for (String line : body.split("\n")) {
            line = line.replace("\r", ""); // Chuẩn hoá CR
            if (line.startsWith(".")) {
                writer.print("." + line + "\r\n"); // dot-stuffing
            } else {
                writer.print(line + "\r\n");
            }
        }
        writer.flush();

        // Kết thúc DATA bằng dòng chỉ có dấu chấm
        writer.print(".\r\n");
        writer.flush();

        assertOk(readLine(), "Gửi thư");
    }

    /**
     * Gửi lệnh QUIT và đóng kết nối.
     */
    public void quit() {
        try {
            if (writer != null) {
                sendCommand("QUIT");
                readLine(); // Đọc "221 Bye"
            }
        } catch (IOException ignored) {
        } finally {
            closeQuietly();
        }
    }

    // ──────────────────────────────────────────────
    // Tiện ích nội bộ
    // ──────────────────────────────────────────────

    private void sendCommand(String cmd) {
        writer.print(cmd + "\r\n");
        writer.flush();
    }

    private String readLine() throws IOException {
        String line = reader.readLine();
        if (line == null) throw new IOException("Server đóng kết nối bất ngờ.");
        return line;
    }

    private void assertOk(String response, String context) throws IOException {
        if (!response.startsWith("2")) { // 2xx là OK
            throw new IOException(context + " thất bại: " + response);
        }
    }

    private String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private void closeQuietly() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
