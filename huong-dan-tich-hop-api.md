# Tài liệu Tích hợp API - App Ghi âm Cuộc gọi
> **Dành cho:** Team Mobile (Android/iOS)
> **Ngày cập nhật:** 2026-08-02

Tài liệu này tóm tắt toàn bộ quy trình tích hợp API ghi âm cuộc gọi của nhân viên vào ứng dụng mobile, bao gồm cơ chế xác thực, chi tiết các API, luồng xử lý trên ứng dụng và các lưu ý kỹ thuật quan trọng khi triển khai.

---

## 1. Thông tin chung

*   **Base URL:** `https://<domain>/api/mobile`
*   **Content-Type:** 
    *   Xác thực (Auth): `application/json` (cho các request `POST`)
    *   Tải lên (Upload): `multipart/form-data` (cho request tải file ghi âm)
*   **Authentication (Xác thực):**
    *   `X-Api-Key`: Bắt buộc đính kèm trong Header của **TẤT CẢ** request. API key được cấp riêng cho team mobile (liên hệ team backend để nhận).
    *   `Authorization`: Định dạng `Bearer <token>`, bắt buộc đính kèm trong Header sau khi đã xác thực OTP thành công.

---

## 2. Luồng Xác thực (Authentication Flow)

Ứng dụng thực hiện xác thực lần đầu theo mô hình **OTP → Bearer Token** như sau:

```
Nhân viên nhập ID trên App
       │
       ▼
POST /auth/request-otp (Kèm X-Api-Key)
       │
       ▼
Server gửi mã OTP về Email của Quản lý (hiệu lực 15 phút)
       │
       ▼
Quản lý nhận OTP từ email và đưa cho Nhân viên nhập vào App
       │
       ▼
POST /auth/verify-otp (Kèm X-Api-Key)
       │
       ▼
Server trả về Bearer Token + Thông tin Nhân viên
       │
       ▼
App lưu Token vĩnh viễn và gửi kèm Header Authorization trong các API sau
```

---

## 3. Danh sách API chi tiết

### 3.1. Yêu cầu gửi mã OTP (Bước 1)
*   **Endpoint:** `POST /auth/request-otp`
*   **Headers:**
    *   `X-Api-Key: <your_mobile_api_key>`
    *   `Content-Type: application/json`
*   **Request Body (JSON):**
    ```json
    {
      "user_id": 123 // ID của nhân viên (Kiểu: int, Bắt buộc)
    }
    ```
*   **Mã phản hồi HTTP (HTTP Status Code):**
    *   `200`: Thành công. Mã OTP đã được gửi về email của quản lý (Mã OTP có hiệu lực trong **15 phút**. Mỗi lần yêu cầu OTP mới, các OTP cũ sẽ bị vô hiệu).
    *   `404`: Không tìm thấy nhân viên.
    *   `500`: Lỗi server (ví dụ: chưa cấu hình email trên hệ thống).

### 3.2. Xác thực OTP (Bước 2)
*   **Endpoint:** `POST /auth/verify-otp`
*   **Headers:**
    *   `X-Api-Key: <your_mobile_api_key>`
    *   `Content-Type: application/json`
*   **Request Body (JSON):**
    ```json
    {
      "user_id": 123, // ID của nhân viên (Kiểu: int, Bắt buộc)
      "otp": "123456" // Mã OTP 6 số do quản lý cung cấp (Kiểu: string, Bắt buộc)
    }
    ```
*   **Mã phản hồi HTTP (HTTP Status Code):**
    *   `200`: Xác thực thành công. Trả về token và thông tin nhân viên.
        *   *Yêu cầu:* App phải lưu token này vào persistent storage bảo mật (`SharedPreferences` trên Android hoặc `UserDefaults` / `Keychain` trên iOS) để sử dụng lâu dài.
    *   `401`: OTP sai hoặc đã hết hạn.

### 3.3. Tải lên file ghi âm cuộc gọi (Upload Recording)
*   **Yêu cầu:** Đã xác thực thành công (có Bearer Token). API này được gọi ngay sau khi mỗi cuộc gọi kết thúc (hoặc gửi theo lô khi thiết bị có mạng).
*   **Endpoint:** `POST /call-records`
*   **Headers:**
    *   `X-Api-Key: <your_mobile_api_key>`
    *   `Authorization: Bearer <token>`
    *   `Content-Type: multipart/form-data`
*   **Request Body (Multipart Form):**
    *   `recording` (File - Bắt buộc): File ghi âm định dạng `mp3`, `wav`, `m4a`, `amr`... Dung lượng tối đa **50MB**.
    *   `phone_number_from` (String - Tùy chọn): Số điện thoại người gọi.
    *   `phone_number_to` (String - Tùy chọn): Số điện thoại người nhận.
    *   `call_type` (String - Tùy chọn): Nhận diện loại cuộc gọi (`incoming` = cuộc gọi đến, `outgoing` = cuộc gọi đi).
    *   `duration` (Int - Tùy chọn): Thời lượng cuộc gọi tính bằng giây. Mặc định là `0`.
    *   `call_at` (String - Tùy chọn): Thời điểm diễn ra cuộc gọi. Định dạng bắt buộc: `YYYY-MM-DD HH:mm:ss` (theo múi giờ `Asia/Ho_Chi_Minh`).

*   **Quy tắc xác định số điện thoại gửi/nhận:**
    | Loại cuộc gọi | Giá trị `call_type` | `phone_number_from` (Từ) | `phone_number_to` (Đến) |
    | :--- | :--- | :--- | :--- |
    | Nhân viên gọi đi cho khách hàng | `outgoing` | Số điện thoại Nhân viên | Số điện thoại Khách hàng |
    | Khách hàng gọi đến nhân viên | `incoming` | Số điện thoại Khách hàng | Số điện thoại Nhân viên |

*   **Mã phản hồi HTTP (HTTP Status Code):**
    *   `201`: Tạo và tải lên thành công.
    *   `401`: Chưa xác thực / Token sai hoặc không hợp lệ.
    *   `422`: Dữ liệu không hợp lệ (xem thông tin chi tiết lỗi trong trường `errors` của response).
    *   `500`: Lỗi server.

---

## 4. Bảng mã lỗi HTTP chung

| Mã lỗi HTTP | Ý nghĩa | Hướng xử lý trên App |
| :--- | :--- | :--- |
| **200** | Thành công | Tiếp tục xử lý logic app (Lưu token khi xác thực) |
| **201** | Tạo mới thành công | Đánh dấu cuộc gọi đã tải lên thành công, xóa khỏi queue local |
| **401** | Chưa xác thực / Sai Token / Sai hoặc hết hạn OTP | Chuyển ngay về màn hình nhập ID để xác thực lại từ đầu |
| **404** | Không tìm thấy nhân viên | Hiển thị thông báo lỗi cho người dùng kiểm tra lại ID |
| **422** | Dữ liệu không hợp lệ | Đọc chi tiết lỗi từ response để debug/điều chỉnh dữ liệu gửi đi |
| **500** | Lỗi server | Ghi log lỗi và thực hiện Retry logic |

---

## 5. Nguyên tắc triển khai và xử lý lỗi trên Mobile

Để đảm bảo ứng dụng hoạt động ổn định và không làm mất mát dữ liệu ghi âm của nhân viên, team Mobile cần tuân thủ nghiêm ngặt các nguyên tắc sau:

1.  **Lưu trữ Token an toàn (Persistent Storage):**
    *   Lưu token xác thực vào bộ nhớ an toàn (`SharedPreferences` / `UserDefaults`).
    *   Token này **không có thời hạn hết hạn** trên server. Nó chỉ mất hiệu lực khi nhân viên thực hiện xác thực (nhập OTP) thành công trên một thiết bị khác.
2.  **Xử lý lỗi 401 (Hết hạn/Sai token):**
    *   Bất kỳ khi nào ứng dụng nhận được mã phản hồi HTTP `401` từ bất kỳ API nào, app phải lập tức xóa token cũ và chuyển hướng người dùng về màn hình xác thực ban đầu (nhập ID nhân viên và yêu cầu OTP mới).
3.  **Xử lý khi mất kết nối mạng (Offline Mode):**
    *   Nếu thiết bị không có kết nối mạng tại thời điểm cuộc gọi kết thúc, app tuyệt đối không được bỏ qua cuộc gọi.
    *   Hãy lưu file ghi âm cùng các thông tin metadata (`phone_number_from`, `phone_number_to`, `call_type`, `duration`, `call_at`) vào một hàng đợi cục bộ (Local Queue/Database) trên thiết bị.
    *   Khi có mạng trở lại, ứng dụng phải tự động quét hàng đợi này và gửi tuần tự lên server.
4.  **Cơ chế thử lại (Retry Logic):**
    *   Trường hợp tải file ghi âm thất bại do lỗi mạng (Timeout) hoặc lỗi server (`HTTP 500`), ứng dụng cần thực hiện cơ chế thử lại tối đa **3 lần** với khoảng cách thời gian tăng dần để tránh làm quá tải server:
        *   Lần thử lại thứ 1: sau **5 giây**
        *   Lần thử lại thứ 2: sau **15 giây**
        *   Lần thử lại thứ 3: sau **30 giây**
5.  **Giới hạn dung lượng file ghi âm:**
    *   Dung lượng file tải lên tối đa được server cho phép là **50MB**.
    *   Đối với các cuộc gọi có thời lượng quá dài, team mobile nên cân nhắc nén file ghi âm (giảm bitrate hoặc chuyển sang định dạng nén tối ưu như `.m4a`, `.mp3`) trước khi thực hiện tải lên để đảm bảo dung lượng luôn dưới ngưỡng 50MB.
6.  **Định dạng thời gian cuộc gọi (`call_at`):**
    *   Khi gửi thông tin thời gian cuộc gọi, bắt buộc phải dùng định dạng `YYYY-MM-DD HH:mm:ss`.
    *   Múi giờ mặc định áp dụng là `Asia/Ho_Chi_Minh` (UTC+7). Hãy đảm bảo thời gian trên thiết bị đã được chuẩn hóa theo múi giờ này trước khi gửi.
