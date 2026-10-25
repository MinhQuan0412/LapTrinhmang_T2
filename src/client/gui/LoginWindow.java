package client.gui;

import client.Pop3Client;
import client.SmtpClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * LoginWindow – cửa sổ đăng nhập Email Client.
 * Người dùng nhập: tên user, mật khẩu, IP server.
 * Sau khi đăng nhập thành công → mở MainWindow.
 */
public class LoginWindow extends JFrame {

    // ── Cấu hình mặc định ────────────────────────
    private static final String DEFAULT_HOST = "localhost";
    private static final int    SMTP_PORT    = 2525;
    private static final int    POP3_PORT    = 1100;
    private static final String DOMAIN       = "mail.local";

    // ── Thành phần giao diện ─────────────────────
    private JTextField  tfHost;
    private JTextField  tfUser;
    private JPasswordField pfPass;
    private JButton     btnLogin;
    private JLabel      lblStatus;

    public LoginWindow() {
        super("Email Client – Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        buildUI();
        pack();
        setLocationRelativeTo(null); // Căn giữa màn hình
    }

    private void buildUI() {
        // Panel chính
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBorder(new EmptyBorder(30, 40, 30, 40));
        main.setBackground(new Color(245, 247, 250));

        // ── Tiêu đề ────────────────────────────────
        JLabel title = new JLabel("📧 Email Client", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(33, 90, 168));
        main.add(title, BorderLayout.NORTH);

        // ── Form ───────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Server IP
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(label("Server IP:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfHost = styledTextField(DEFAULT_HOST);
        form.add(tfHost, gbc);

        // Tên người dùng
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(label("Tên đăng nhập:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        tfUser = styledTextField("");
        form.add(tfUser, gbc);

        // Mật khẩu
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(label("Mật khẩu:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        pfPass = new JPasswordField(18);
        styleComponent(pfPass);
        form.add(pfPass, gbc);

        main.add(form, BorderLayout.CENTER);

        // ── Nút đăng nhập + trạng thái ─────────────
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);

        btnLogin = new JButton("Đăng nhập");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(33, 90, 168));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setPreferredSize(new Dimension(0, 40));
        btnLogin.addActionListener(e -> doLogin());

        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(Color.RED);

        bottom.add(btnLogin, BorderLayout.CENTER);
        bottom.add(lblStatus, BorderLayout.SOUTH);
        main.add(bottom, BorderLayout.SOUTH);

        // Enter → đăng nhập
        getRootPane().setDefaultButton(btnLogin);

        setContentPane(main);
        setPreferredSize(new Dimension(380, 310));
    }

    // ── Xử lý đăng nhập ────────────────────────────
    private void doLogin() {
        String host    = tfHost.getText().trim();
        String user    = tfUser.getText().trim();
        String pass    = new String(pfPass.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            lblStatus.setText("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        btnLogin.setEnabled(false);
        lblStatus.setForeground(new Color(33, 90, 168));
        lblStatus.setText("Đang kết nối...");

        // Chạy trên thread riêng để không đóng băng UI
        new Thread(() -> {
            try {
                // Kiểm tra đăng nhập bằng SMTP
                SmtpClient smtp = new SmtpClient(host, SMTP_PORT);
                smtp.connect();
                smtp.login(user, pass);
                smtp.quit();

                // Đăng nhập thành công → mở MainWindow
                String email = user + "@" + DOMAIN;
                SwingUtilities.invokeLater(() -> {
                    new MainWindow(host, SMTP_PORT, POP3_PORT, email, pass).setVisible(true);
                    dispose(); // Đóng cửa sổ login
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setForeground(Color.RED);
                    lblStatus.setText("Lỗi: " + ex.getMessage());
                    btnLogin.setEnabled(true);
                });
            }
        }).start();
    }

    // ── Tiện ích tạo component ───────────────────────
    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }

    private JTextField styledTextField(String defaultText) {
        JTextField tf = new JTextField(defaultText, 18);
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
