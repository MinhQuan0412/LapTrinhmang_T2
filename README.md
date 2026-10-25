# Email Client GUI – Lập trình mạng máy tính

Đồ án cuối kỳ môn **Lập trình mạng máy tính** – Đề tài I.1: Hệ thống Email Server nội bộ hỗ trợ POP3 và SMTP Client.
Đây là phần **Client GUI (Người 3)** – giao diện đồ hoạ để gửi và nhận thư.

## Thành viên nhóm

| Người | Module | Mô tả |
|-------|--------|-------|
| Người 1 (Huy) | SMTP Server | Nhận thư từ client, lưu vào mailbox |
| Người 2 | POP3 Server | Cho client tải thư từ mailbox |
| Người 3 (Quân) | **Client GUI** | Giao diện gửi / nhận thư ← **Repo này** |

## Cấu trúc thư mục

```
src/
├── smtp/                      # SMTP Server (Người 1)
│   ├── SmtpServer.java
│   ├── SmtpClientHandler.java
│   ├── UserValidator.java
│   ├── MailboxWriter.java
│   └── SmtpLogger.java
└── client/                    # Client GUI (Người 3) ← Phần này
    ├── SmtpClient.java        # Kết nối SMTP cổng 2525, gửi thư
    ├── Pop3Client.java        # Kết nối POP3 cổng 1100, nhận thư
    ├── ClientMain.java        # Entry point chạy ứng dụng
    └── gui/
        ├── LoginWindow.java   # Màn hình đăng nhập
        ├── MainWindow.java    # Hộp thư đến (đọc, xoá thư)
        └── ComposeWindow.java # Soạn & gửi thư mới
```

## Yêu cầu

- JDK 11 trở lên

## Biên dịch

```powershell
# Biên dịch toàn bộ phần Client
javac -encoding UTF-8 -d out src/client/*.java src/client/gui/*.java
```

## Chạy ứng dụng

> Cần SMTP Server chạy ở cổng 2525 và POP3 Server chạy ở cổng 1100 trước.

```powershell
java -cp out client.ClientMain
```

## Tài khoản test

Lấy từ `users.example.txt`, copy thành `users.txt` (không commit file này):

```
huy:123456
an:abc123
binh:pass789
```

## Chức năng Client GUI

| Màn hình | Chức năng |
|----------|-----------|
| **LoginWindow** | Đăng nhập bằng user/pass, nhập IP server |
| **MainWindow** | Xem hộp thư đến, đọc nội dung thư, xoá thư, nhận thư mới |
| **ComposeWindow** | Soạn thư mới, nhập To/Subject/Body, gửi qua SMTP |

## Giao thức kết nối

### SMTP (gửi thư) – cổng 2525
- Hỗ trợ `AUTH LOGIN` và `AUTH PLAIN`
- Tự ghi các header `From`, `To`, `Subject`, `Date` theo RFC 5321
- Áp dụng **dot-stuffing** (dòng bắt đầu bằng `.` → `..`)
- `MAIL FROM` phải trùng với tài khoản đã đăng nhập

### POP3 (nhận thư) – cổng 1100
- Hỗ trợ `USER`, `PASS`, `STAT`, `LIST`, `RETR`, `DELE`, `QUIT`
- `DELE` chỉ đánh dấu, thư bị xoá thật khi `QUIT`

## Cổng dịch vụ

| Dịch vụ | Cổng |
|---------|------|
| SMTP Server | 2525 |
| POP3 Server | 1100 |

## Repo nhóm

[https://github.com/huyy0209/email-server-lptm](https://github.com/huyy0209/email-server-lptm)
