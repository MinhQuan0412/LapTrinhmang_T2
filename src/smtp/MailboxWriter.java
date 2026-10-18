package smtp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Ghi thư vào hộp thư của người nhận dưới dạng file .eml.
 * Cấu trúc: mailbox/<user>/<yyyyMMddHHmmssSSS>-<uuid>.eml
 * (POP3 Server của bạn cùng nhóm sẽ đọc các file này.)
 */
public class MailboxWriter {

    private final Path mailboxRoot;

    public MailboxWriter(String mailboxDir) {
        this.mailboxRoot = Paths.get(mailboxDir);
    }

    /**
     * Lưu một thư cho một người nhận.
     *
     * @param recipient địa chỉ người nhận (user hoặc user@domain)
     * @param from      địa chỉ người gửi (MAIL FROM)
     * @param rawData   nội dung thư giữa DATA và dấu "." (đã bỏ dot-stuffing)
     * @return đường dẫn file .eml đã tạo
     */
    public Path save(String recipient, String from, String rawData) throws IOException {
        String user = UserValidator.localPart(recipient);
        Path userDir = mailboxRoot.resolve(user);
        Files.createDirectories(userDir);

        String stamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        Path file = userDir.resolve(stamp + "-" + UUID.randomUUID().toString().substring(0, 8) + ".eml");

        // Thêm header Return-Path và Received ở đầu thư
        String received = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("Return-Path: <").append(from).append(">\r\n");
        sb.append("Received: by localhost (SMTP Server LPTM); ").append(received).append("\r\n");
        sb.append(rawData);

        // Ghi file an toàn khi nhiều client gửi cùng lúc (mỗi thư một file riêng)
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
        return file;
    }
}