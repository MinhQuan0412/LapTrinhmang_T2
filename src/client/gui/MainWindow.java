package client.gui;

import client.Pop3Client;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MainWindow – cửa sổ chính hiển thị hộp thư đến.
 * Chức năng:
 *   - Nhận thư (POP3 LIST + RETR)
 *   - Xem nội dung thư
 *   - Xoá thư
 *   - Soạn thư mới (mở ComposeWindow)
 */
public class MainWindow extends JFrame {

    // ── Thông tin kết nối ──────────────────────────
    private final String host;
    private final int    smtpPort;
    private final int    pop3Port;
    private final String email;   // vd: huy@mail.local
    private final String pass;
    private final String username; // phần trước @ (vd: huy)

    // ── Dữ liệu thư ────────────────────────────────
    private final List<String> mailContents = new ArrayList<>(); // Nội dung từng thư (đã RETR)
    private final DefaultTableModel tableModel;

    // ── Thành phần giao diện ───────────────────────
    private JTable  mailTable;
    private JTextArea areaContent;
    private JLabel  lblStatus;
    private JButton btnDelete;

    public MainWindow(String host, int smtpPort, int pop3Port, String email, String pass) {
        super("Email Client – Hộp thư đến (" + email + ")");
        this.host     = host;
        this.smtpPort = smtpPort;
        this.pop3Port = pop3Port;
        this.email    = email;
        this.pass     = pass;
        this.username = email.contains("@") ? email.split("@")[0] : email;

        tableModel = new DefaultTableModel(
                new String[]{"#", "Từ", "Tiêu đề", "Ngày"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUI();
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Tự động nhận thư khi mở
        refreshMails();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // ── Thanh công cụ ───────────────────────────
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(33, 90, 168));
        toolbar.setBorder(new EmptyBorder(6, 10, 6, 10));

        JButton btnCompose = toolbarButton("✏ Soạn thư");
        btnCompose.addActionListener(e ->
                new ComposeWindow(host, smtpPort, email, pass, this::refreshMails).setVisible(true));

        JButton btnRefresh = toolbarButton("🔄 Nhận thư");
        btnRefresh.addActionListener(e -> refreshMails());

        btnDelete = toolbarButton("🗑 Xoá thư");
        btnDelete.addActionListener(e -> deleteSelected());
        btnDelete.setEnabled(false);

        JLabel lblUser = new JLabel("  " + email);
        lblUser.setForeground(Color.WHITE);
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));

        toolbar.add(btnCompose);
        toolbar.addSeparator();
        toolbar.add(btnRefresh);
        toolbar.addSeparator();
        toolbar.add(btnDelete);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(lblUser);
        add(toolbar, BorderLayout.NORTH);

        // ── Bảng danh sách thư ─────────────────────
        mailTable = new JTable(tableModel);
        mailTable.setRowHeight(28);
        mailTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        mailTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        mailTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mailTable.setGridColor(new Color(220, 225, 235));
        mailTable.setShowVerticalLines(false);

        // Chiều rộng các cột
        mailTable.getColumnModel().getColumn(0).setMaxWidth(40);
        mailTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        mailTable.getColumnModel().getColumn(2).setPreferredWidth(300);
        mailTable.getColumnModel().getColumn(3).setPreferredWidth(160);

        mailTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelected();
        });

        JScrollPane tableScroll = new JScrollPane(mailTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        // ── Khu vực xem nội dung thư ───────────────
        areaContent = new JTextArea();
        areaContent.setEditable(false);
        areaContent.setFont(new Font("Consolas", Font.PLAIN, 13));
        areaContent.setBackground(new Color(250, 252, 255));
        areaContent.setBorder(new EmptyBorder(10, 14, 10, 14));
        areaContent.setLineWrap(true);
        areaContent.setWrapStyleWord(true);

        JScrollPane contentScroll = new JScrollPane(areaContent);
        contentScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 230)));

        // ── Chia đôi: danh sách bên trên, nội dung bên dưới ─
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, contentScroll);
        split.setDividerLocation(220);
        split.setOneTouchExpandable(true);
        add(split, BorderLayout.CENTER);

        // ── Thanh trạng thái ────────────────────────
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setBorder(new EmptyBorder(4, 10, 4, 10));
        lblStatus.setForeground(new Color(80, 90, 110));
        add(lblStatus, BorderLayout.SOUTH);
    }

    // ── Nhận thư từ server ──────────────────────────
    public void refreshMails() {
        lblStatus.setText("Đang nhận thư...");
        btnDelete.setEnabled(false);

        new Thread(() -> {
            try {
                Pop3Client pop3 = new Pop3Client(host, pop3Port);
                pop3.connect();
                pop3.login(username, pass);

                List<String> msgList = pop3.list();

                List<String[]>  rows    = new ArrayList<>();
                List<String>    content = new ArrayList<>();

                for (String entry : msgList) {
                    int num = Integer.parseInt(entry.split(" ")[0]);
                    String raw = pop3.retr(num);

                    String from    = Pop3Client.getHeader(raw, "From");
                    String subject = Pop3Client.getHeader(raw, "Subject");
                    String date    = Pop3Client.getHeader(raw, "Date");

                    if (from.isEmpty())    from    = "(không có)";
                    if (subject.isEmpty()) subject = "(không có tiêu đề)";
                    if (date.isEmpty())    date    = "";

                    rows.add(new String[]{String.valueOf(num), from, subject, date});
                    content.add(raw);
                }

                pop3.quit();

                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    mailContents.clear();
                    for (String[] row : rows)   tableModel.addRow(row);
                    mailContents.addAll(content);
                    areaContent.setText("");
                    lblStatus.setText("Tổng: " + msgList.size() + " thư");
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        lblStatus.setText("Lỗi nhận thư: " + ex.getMessage()));
            }
        }).start();
    }

    // ── Hiển thị thư được chọn ─────────────────────
    private void showSelected() {
        int row = mailTable.getSelectedRow();
        if (row < 0 || row >= mailContents.size()) {
            btnDelete.setEnabled(false);
            return;
        }

        String raw  = mailContents.get(row);
        String from    = Pop3Client.getHeader(raw, "From");
        String to      = Pop3Client.getHeader(raw, "To");
        String subject = Pop3Client.getHeader(raw, "Subject");
        String date    = Pop3Client.getHeader(raw, "Date");
        String body    = Pop3Client.getBody(raw);

        areaContent.setText(
                "Từ     : " + from    + "\n" +
                "Đến    : " + to      + "\n" +
                "Tiêu đề: " + subject + "\n" +
                "Ngày   : " + date    + "\n" +
                "─".repeat(50) + "\n" +
                body);
        areaContent.setCaretPosition(0);
        btnDelete.setEnabled(true);
    }

    // ── Xoá thư được chọn ─────────────────────────
    private void deleteSelected() {
        int row = mailTable.getSelectedRow();
        if (row < 0) return;

        int msgNum = Integer.parseInt((String) tableModel.getValueAt(row, 0));
        String subject = (String) tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Xoá thư: \"" + subject + "\"?",
                "Xác nhận xoá", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                Pop3Client pop3 = new Pop3Client(host, pop3Port);
                pop3.connect();
                pop3.login(username, pass);
                pop3.dele(msgNum);
                pop3.quit(); // Xoá thật khi QUIT

                SwingUtilities.invokeLater(() -> {
                    tableModel.removeRow(row);
                    mailContents.remove(row);
                    areaContent.setText("");
                    btnDelete.setEnabled(false);
                    lblStatus.setText("Đã xoá thư.");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        lblStatus.setText("Lỗi xoá thư: " + ex.getMessage()));
            }
        }).start();
    }

    // ── Tiện ích ────────────────────────────────────
    private JButton toolbarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(33, 90, 168));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(55, 120, 200));
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(33, 90, 168));
            }
        });
        return btn;
    }
}
