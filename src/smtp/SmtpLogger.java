package smtp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Ghi log ra console VÀ file logs/smtp.log (thread-safe).
 * Lưu ý: KHÔNG ghi mật khẩu vào log.
 */
public class SmtpLogger {

    private static final Path LOG_FILE = Paths.get("logs", "smtp.log");
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        try {
            Files.createDirectories(LOG_FILE.getParent());
        } catch (IOException e) {
            System.err.println("[LOG] Không tạo được thư mục logs: " + e.getMessage());
        }
    }

    public static void info(String msg) { write("INFO ", msg); }

    public static void error(String msg) { write("ERROR", msg); }

    private static synchronized void write(String level, String msg) {
        String line = FMT.format(new Date()) + " [" + level + "] " + msg;
        System.out.println(line);
        try {
            Files.write(LOG_FILE, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[LOG] Không ghi được file log: " + e.getMessage());
        }
    }
}