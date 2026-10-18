# Email Server nội bộ (POP3 / SMTP) – Lập trình mạng máy tính

Đồ án cuối kỳ môn **Lập trình mạng máy tính** – Đề tài I.1: Hệ thống Email Server nội bộ hỗ trợ POP3 và SMTP Client.
Triển khai bằng **Java** (Socket, đa luồng).

## Thành phần

| Module | Mô tả | Người phụ trách |
|---|---|---|
| SMTP Server | Nhận thư từ client, lưu vào mailbox | Người 1 |
| POP3 Server | Cho client tải thư từ mailbox | Người 2 |
| Client GUI | Giao diện gửi / nhận thư | Người 3 |

## Cấu trúc thư mục

```
email-server-lptm/
├── src/
│   └── smtp/
│       ├── SmtpServer.java         # Mở cổng 2525, nhận kết nối (thread pool)
│       ├── SmtpClientHandler.java  # Xử lý lệnh SMTP của một client
│       ├── UserValidator.java      # Đọc users.txt, kiểm tra user và mật khẩu
│       ├── MailboxWriter.java      # Ghi thư ra file .eml
│       └── SmtpLogger.java         # Ghi log ra console và logs/smtp.log
├── users.example.txt               # File user mẫu
├── mailbox/                        # (tự sinh) thư của từng user
├── logs/                           # (tự sinh) file log
└── README.md
```

## Cổng dịch vụ

| Dịch vụ | Cổng |
|---|---|
| SMTP Server | 2525 |
| POP3 Server | 1100 |

## Yêu cầu

- JDK 11 trở lên (kiểm tra: `java -version`, `javac -version`)

## Cấu hình user

Tạo `users.txt` từ file mẫu:

```powershell
Copy-Item users.example.txt users.txt
```

Mỗi dòng một user, định dạng `tên_user:mật_khẩu`. Dòng bắt đầu bằng `#` là chú thích.

```
huy:123456
an:abc123
```

> `users.txt` đã nằm trong `.gitignore`, không commit mật khẩu thật lên repo.

## Biên dịch và chạy SMTP Server

```powershell
# Biên dịch
javac -d out src/smtp/*.java

# Chạy (mặc định: cổng 2525, thư mục mailbox, file users.txt)
java -cp out smtp.SmtpServer

# Hoặc chỉ định tham số: [port] [mailboxDir] [usersFile]
java -cp out smtp.SmtpServer 2525 mailbox users.txt
```

## Lệnh SMTP được hỗ trợ

`HELO`, `EHLO`, `AUTH PLAIN`, `AUTH LOGIN`, `MAIL FROM`, `RCPT TO`, `DATA`, `RSET`, `NOOP`, `VRFY`, `QUIT`

- Phải đăng nhập (`AUTH`) trước khi gửi thư.
- Địa chỉ `MAIL FROM` phải trùng với user đã đăng nhập.
- Người nhận không có trong `users.txt` bị từ chối với mã `550`.
- Đăng nhập sai 3 lần, server ngắt kết nối.

Hai tùy chọn trên có thể bật/tắt bằng hằng `REQUIRE_AUTH` và `ENFORCE_SENDER_MATCH` trong `SmtpClientHandler.java`.

## Cấu trúc mailbox

Mỗi người nhận có một thư mục, mỗi thư là một file `.eml`:

```
mailbox/
└── <tên_user>/
    ├── 20261018130000123-a1b2c3d4.eml
    └── ...
```

Tên thư mục là phần trước dấu `@` của địa chỉ, viết thường. POP3 Server đọc đúng cấu trúc này.

## Kiểm thử nhanh

Mở terminal, kết nối bằng `telnet localhost 2525` (hoặc `nc localhost 2525`), nhập lần lượt:

```
EHLO test
AUTH PLAIN AGh1eQAxMjM0NTY=
MAIL FROM:<huy@mail.local>
RCPT TO:<an@mail.local>
DATA
Subject: test

Xin chao
.
QUIT
```

`AGh1eQAxMjM0NTY=` là base64 của `\0huy\0123456` (user `huy`, mật khẩu `123456`).
Kết quả: file `.eml` mới trong `mailbox/an/` và các dòng log trong `logs/smtp.log`.

## Quy trình làm việc với Git

- Không push trực tiếp lên `main`. Mỗi người làm trên nhánh riêng:
  - `feature/smtp-server`
  - `feature/pop3-server`
  - `feature/client-gui`
- Commit theo quy ước: `feat:`, `fix:`, `docs:`, `refactor:`.
- Mở Pull Request, có ít nhất một bạn trong nhóm review rồi mới merge.
- Trước khi bắt đầu việc mới: `git checkout main` rồi `git pull origin main`.