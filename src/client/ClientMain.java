package client;

import client.gui.LoginWindow;
import javax.swing.SwingUtilities;

/**
 * ClientMain – điểm khởi động ứng dụng Email Client.
 *
 * Chạy:
 *   javac -d out src/client/*.java src/client/gui/*.java
 *   java -cp out client.ClientMain
 */
public class ClientMain {

    public static void main(String[] args) {
        // Chạy GUI trên Event Dispatch Thread (EDT) – bắt buộc với Swing
        SwingUtilities.invokeLater(() -> {
            new LoginWindow().setVisible(true);
        });
    }
}
