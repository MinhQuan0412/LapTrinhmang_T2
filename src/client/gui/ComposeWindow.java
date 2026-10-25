package client.gui;

import client.SmtpClient;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * ComposeWindow – cửa sổ soạn và gửi thư.
 * Sau khi gửi thành công, gọi callback onSent (thường là MainWindow.refreshMails).
 */
public class ComposeWindow extends JFrame {

    private final String host;
    private final int    smtpPort;
    private final String fromEmail; // vd: huy@mail.local
    private final String pass;
    private final Runnable onSent;  // callback sau khi gửi xong

    // ── Thành phần giao diện ──────────────────────
    private JTextField  tfTo;
    private JTextField  tfSubject;
    private JTextArea   areaBody;
    private JButton     btnSend;
    private JLabel      lblStatus;

    public ComposeWindow(String host, int smtpPort,
                         String fromEmail, String pass, Runnable onSent) {
        super("Soạn thư mới – " + fromEmail);
        this.host      = host;
        this.smtpPort  = smtpPort;
        this.fromEmail = fromEmail;
        this.pass      = pass;
        this.onSent    = onSent;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildUI();
        setSize(560, 460);
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setBorder(new EmptyBorder(16, 20, 16, 20));
        main.setBackground(new Color(248, 250, 253));

        // ── Các trường nhập ───────────────────────
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Từ (chỉ đọc)
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        fields.add(label("Từ:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JTextField tfFrom = new JTextField(fromEmail);
        tfFrom.setEditable(false);
        tfFrom.setBackground(new Color(235, 240, 248));
        styleComponent(tfFrom);
        fields.add(tfFrom, gbc);

        // Đến
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        fields.add(label("Đến:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfTo = styledField("");
        fields.add(tfTo, gbc);

        // Tiêu đề
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        fields.add(label("Tiêu đề:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfSubject = styledField("");
        fields.add(tfSubject, gbc);

        main.add(fields, BorderLayout.NORTH);

        // ── Vùng soạn nội dung ────────────────────
        areaBody = new JTextArea();
        areaBody.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        areaBody.setLineWrap(true);
        areaBody.setWrapStyleWord(true);
        areaBody.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane scroll = new JScrollPane(areaBody);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(180, 190, 210)));
        main.add(scroll, BorderLayout.CENTER);

        // ── Nút gửi + trạng thái ─────────────────
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);

        btnSend = new JButton("📤  Gửi thư");
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSend.setBackground(new Color(33, 150, 83));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFocusPainted(false);
        btnSend.setBorderPainted(false);
        btnSend.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSend.setPreferredSize(new Dimension(0, 40));
        btnSend.addActionListener(e -> doSend());

        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        bottom.add(btnSend,    BorderLayout.CENTER);
        bottom.add(lblStatus,  BorderLayout.SOUTH);
        main.add(bottom, BorderLayout.SOUTH);

        setContentPane(main);
    }

    // ── Gửi thư ─────────────────────────────────────
    private void doSend() {
        String to      = tfTo.getText().trim();
        String subject = tfSubject.getText().trim();
        String body    = areaBody.getText();

        // Kiểm tra đầu vào
        if (to.isEmpty()) {
            showStatus("Vui lòng nhập địa chỉ người nhận.", Color.RED);
            return;
        }
        if (subject.isEmpty()) {
            showStatus("Vui lòng nhập tiêu đề thư.", Color.RED);
            return;
        }
        if (body.trim().isEmpty()) {
            showStatus("Vui lòng nhập nội dung thư.", Color.RED);
            return;
        }

        // Thêm @mail.local nếu người dùng nhập thiếu
        if (!to.contains("@")) {
            to = to + "@mail.local";
        }

        btnSend.setEnabled(false);
        showStatus("Đang gửi...", new Color(33, 90, 168));

        final String recipient = to;

        new Thread(() -> {
            try {
                // Lấy tên user từ email (huy@mail.local → huy)
                String user = fromEmail.contains("@")
                        ? fromEmail.split("@")[0]
                        : fromEmail;

                SmtpClient smtp = new SmtpClient(host, smtpPort);
                smtp.connect();
                smtp.login(user, pass);
                smtp.sendMail(fromEmail, recipient, subject, body);
                smtp.quit();

                SwingUtilities.invokeLater(() -> {
                    showStatus("✅ Đã gửi thư thành công!", new Color(33, 150, 83));
                    btnSend.setEnabled(true);
                    // Gọi callback để MainWindow refresh hộp thư
                    if (onSent != null) onSent.run();

                    // Đóng cửa sổ sau 1.5 giây
                    new Timer(1500, ev -> dispose()) {{
                        setRepeats(false);
                        start();
                    }};
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    showStatus("Lỗi: " + ex.getMessage(), Color.RED);
                    btnSend.setEnabled(true);
                });
            }
        }).start();
    }

    // ── Tiện ích ────────────────────────────────────
    private void showStatus(String msg, Color color) {
        lblStatus.setText(msg);
        lblStatus.setForeground(color);
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }

    private JTextField styledField(String def) {
        JTextField tf = new JTextField(def, 20);
        styleComponent(tf);
        return tf;
    }

    private void styleComponent(JComponent comp) {
        comp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 190, 210), 1, true),
                new EmptyBorder(4, 8, 4, 8)));
    }
}
