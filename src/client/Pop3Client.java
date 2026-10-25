package client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Pop3Client – kết nối TCP tới POP3 Server (cổng 1100),
 * tải danh sách thư và nội dung từng thư.
 *
 * Cách dùng:
 *   Pop3Client pop3 = new Pop3Client("localhost", 1100);
 *   pop3.connect();
 *   pop3.login("an", "abc123");
 *   List<String> list = pop3.list();   // ["1 1024", "2 2048"]
 *   String mail = pop3.retr(1);        // nội dung thư số 1
 *   pop3.dele(1);                      // đánh dấu xoá thư số 1
 *   pop3.quit();                       // xác nhận xoá và thoát
 */
public class Pop3Client {

    private final String host;
    private final int port;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public Pop3Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // ──────────────────────────────────────────────
    // Kết nối & xác thực
    // ──────────────────────────────────────────────

    /**
     * Mở kết nối và đọc banner "+OK".
     */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        String banner = readLine();
        if (!banner.startsWith("+OK")) {
            throw new IOException("Kết nối POP3 thất bại: " + banner);
        }
    }

    /**
     * Đăng nhập bằng lệnh USER và PASS.
     */
    public void login(String username, String password) throws IOException {
        sendCommand("USER " + username);
        assertOk(readLine(), "USER");

        sendCommand("PASS " + password);
        assertOk(readLine(), "PASS");
    }

    // ──────────────────────────────────────────────
    // Lệnh POP3
    // ──────────────────────────────────────────────

    /**
     * STAT – trả về tổng số thư và tổng kích thước (bytes).
     * Ví dụ: [3, 8472] nghĩa là 3 thư, tổng 8472 bytes.
     */
    public int[] stat() throws IOException {
        sendCommand("STAT");
        String resp = readLine();  // "+OK 3 8472"
        assertOk(resp, "STAT");
        String[] parts = resp.split(" ");
        return new int[]{Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
    }

    /**
     * LIST – trả về danh sách ["1 512", "2 1024", ...].
     * Mỗi phần tử: "số_thứ_tự kích_thước_bytes".
     */
    public List<String> list() throws IOException {
        sendCommand("LIST");
        assertOk(readLine(), "LIST");

        List<String> result = new ArrayList<>();
        String line;
        while (!(line = readLine()).equals(".")) {
            result.add(line.trim());
        }
        return result;
    }

    /**
     * RETR msgNumber – tải nội dung thư theo số thứ tự (bắt đầu từ 1).
     * Trả về toàn bộ nội dung thư dưới dạng String (headers + body).
     */
    public String retr(int msgNumber) throws IOException {
        sendCommand("RETR " + msgNumber);
        assertOk(readLine(), "RETR");

        StringBuilder sb = new StringBuilder();
        String line;
        while (!(line = readLine()).equals(".")) {
            // Bỏ dot-stuffing: dòng bắt đầu ".." → "."
            if (line.startsWith("..")) {
                line = line.substring(1);
            }
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * DELE msgNumber – đánh dấu xoá thư (thực sự xoá khi gọi quit()).
     */
    public void dele(int msgNumber) throws IOException {
        sendCommand("DELE " + msgNumber);
        assertOk(readLine(), "DELE");
    }

    /**
     * NOOP – kiểm tra kết nối còn sống không.
     */
    public void noop() throws IOException {
        sendCommand("NOOP");
        assertOk(readLine(), "NOOP");
    }

    /**
     * QUIT – xác nhận xoá các thư đã DELE, đóng kết nối.
     */
    public void quit() {
        try {
            if (writer != null) {
                sendCommand("QUIT");
                readLine(); // Đọc "+OK Bye"
            }
        } catch (IOException ignored) {
        } finally {
            closeQuietly();
        }
    }

    // ──────────────────────────────────────────────
    // Parse nội dung thư
    // ──────────────────────────────────────────────

    /**
     * Lấy giá trị một header từ nội dung thư đã RETR.
     * Ví dụ: getHeader(content, "Subject") → "Tiêu đề thư"
     */
    public static String getHeader(String content, String headerName) {
        for (String line : content.split("\n")) {
            if (line.toLowerCase().startsWith(headerName.toLowerCase() + ":")) {
                return line.substring(headerName.length() + 1).trim();
            }
        }
        return "";
    }

    /**
     * Tách phần body (sau dòng trống) khỏi nội dung thư.
     */
    public static String getBody(String content) {
        int blankLine = content.indexOf("\n\n");
        if (blankLine == -1) return content;
        return content.substring(blankLine + 2).trim();
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
        if (!response.startsWith("+OK")) {
            throw new IOException(context + " thất bại: " + response);
        }
    }

    private void closeQuietly() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
