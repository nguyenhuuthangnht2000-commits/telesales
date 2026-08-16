# Hướng dẫn Tích hợp API — App Ghi âm Cuộc gọi
> **Dành cho:** Team Mobile (Android/iOS)
> **Cập nhật:** 2026-08-16

Tài liệu này tóm tắt toàn bộ quy trình tích hợp API ghi âm cuộc gọi của nhân viên vào ứng dụng mobile, bao gồm cơ chế xác thực, chi tiết các API (Đăng nhập, Đăng xuất, Upload), luồng xử lý trên ứng dụng và các lưu ý kỹ thuật quan trọng khi triển khai.

---

## 1. Thông tin chung

| Mục | Giá trị |
|---|---|
| **Base URL** | `https://<domain>/api/mobile` |
| **Authentication** | `X-Api-Key` (tất cả request) + `Bearer Token` (sau xác thực OTP) |
| **Content-Type** | `application/json` (POST auth) / `multipart/form-data` (POST upload) |

---

## 2. Authentication (Xác thực & Bảo mật)

### 2.1. X-Api-Key (Bắt buộc cho TẤT CẢ request)
Mọi request đều phải gửi kèm Header:
```http
X-Api-Key: <api_key_duoc_cung_cap>
```

Nếu thiếu hoặc sai key:
```json
{ "message": "Unauthorized" }
```
*(HTTP Status: 401)*

> [!NOTE]
> API key sẽ được cung cấp riêng cho team mobile. Liên hệ team backend để nhận.

### 2.2. Luồng OTP → Bearer Token

```
App nhập ID nhân viên
        │
        ▼
POST /auth/request-otp  ──>  Server gửi mã OTP về email quản lý
        │
        ▼
Quản lý nhận OTP nhập vào app
        │
        ▼
POST /auth/verify-otp   ──>  Server trả về token + thông tin NV
        │
        ▼
App lưu token, gửi kèm header cho các request sau:
    Authorization: Bearer <token>
```

### 2.3. Các API sau khi đã xác thực
Gửi **cả 2 header**:
```http
X-Api-Key: <api_key>
Authorization: Bearer <token_nhan_duoc_tu_verify_otp>
```

Nếu thiếu hoặc sai token:
```json
{
  "success": false,
  "message": "Token required"
}
```
*(HTTP Status: 401)*

> [!IMPORTANT]
> Token **không có thời hạn hết hạn**. Chỉ mất hiệu lực khi nhân viên xác thực lại trên thiết bị khác hoặc khi logout thành công.

---

## 3. Danh sách API chi tiết

### 3.1. Request OTP (Bước 1 — Đăng nhập)
Nhân viên nhập ID của mình trên app, app gọi API này. Server gửi mã OTP về **email quản lý** (cấu hình trên hệ thống).

* **Endpoint:** `POST /api/mobile/auth/request-otp`
* **Content-Type:** `application/json`

**Bảng tham số Request:**
| Field | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `user_id` | int | **Có** | ID nhân viên |

**Ví dụ Request:**
```bash
curl -X POST https://<domain>/api/mobile/auth/request-otp \
  -H "X-Api-Key: <api_key>" \
  -H "Content-Type: application/json" \
  -d '{"user_id": 15}'
```

**Response thành công (200 OK):**
```json
{
  "success": true,
  "message": "Ma OTP da duoc gui. Vui long lien he quan ly de nhan ma.",
  "data": {
    "user_id": 15,
    "user_name": "Nguyen Van A"
  }
}
```

**Response lỗi — không tìm thấy nhân viên (404 Not Found):**
```json
{
  "success": false,
  "message": "Khong tim thay nhan vien"
}
```

**Response lỗi — chưa cấu hình email (500 Internal Server Error):**
```json
{
  "success": false,
  "message": "Chua cau hinh email nhan OTP. Lien he quan tri vien."
}
```

> [!NOTE]
> Mã OTP có hiệu lực trong **15 phút**. Mỗi lần request OTP mới, các OTP cũ sẽ bị vô hiệu.

---

### 3.2. Verify OTP (Bước 2 — Đăng nhập)
Quản lý nhận OTP từ email, đưa cho nhân viên nhập vào app.

* **Endpoint:** `POST /api/mobile/auth/verify-otp`
* **Content-Type:** `application/json`

**Bảng tham số Request:**
| Field | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `user_id` | int | **Có** | ID nhân viên (giống bước 1) |
| `otp` | string | **Có** | Mã OTP 6 số |

**Ví dụ Request:**
```bash
curl -X POST https://<domain>/api/mobile/auth/verify-otp \
  -H "X-Api-Key: <api_key>" \
  -H "Content-Type: application/json" \
  -d '{"user_id": 15, "otp": "482937"}'
```

**Response thành công (200 OK):**
```json
{
  "success": true,
  "message": "Xac thuc thanh cong",
  "data": {
    "token": "a1b2c3d4e5f6...64_ky_tu_random",
    "user": {
      "id": 15,
      "name": "Nguyen Van A",
      "phone": "0901234567",
      "department": "CSKH",
      "branch": "Ha Noi"
    }
  }
}
```

> [!IMPORTANT]
> Lưu `token` vào persistent storage (`SharedPreferences` / `UserDefaults`). Gửi kèm header `Authorization: Bearer <token>` cho tất cả API sau.

**Response lỗi — OTP sai hoặc hết hạn (401 Unauthorized):**
```json
{
  "success": false,
  "message": "Ma OTP khong hop le hoac da het han"
}
```

---

### 3.3. Request Logout OTP (Bước 1 — Đăng xuất)
* **Yêu cầu:** Đã xác thực (có Bearer token)
* Khi nhân viên muốn đăng xuất, app gọi API này. Server gửi mã OTP về **email quản lý** (giống như flow đăng nhập).

* **Endpoint:** `POST /api/mobile/auth/logout/request-otp`
* **Headers:**
  * `X-Api-Key: <api_key>`
  * `Authorization: Bearer <token>`

* Không cần body — server tự động xác định nhân viên từ Bearer token.

**Ví dụ Request:**
```bash
curl -X POST https://<domain>/api/mobile/auth/logout/request-otp \
  -H "X-Api-Key: <api_key>" \
  -H "Authorization: Bearer <token>"
```

**Response thành công (200 OK):**
```json
{
  "success": true,
  "message": "Ma OTP da duoc gui. Vui long lien he quan ly de nhan ma."
}
```

**Response lỗi — chưa cấu hình email (500 Internal Server Error):**
```json
{
  "success": false,
  "message": "Chua cau hinh email nhan OTP. Lien he quan tri vien."
}
```

> [!NOTE]
> Mã OTP có hiệu lực trong **15 phút**. Mỗi lần request OTP mới, các OTP cũ sẽ bị vô hiệu.

---

### 3.4. Logout — Verify OTP (Bước 2 — Đăng xuất)
* **Yêu cầu:** Đã xác thực (có Bearer token)
* Quản lý nhận OTP từ email, đưa cho nhân viên nhập vào app. Sau khi verify thành công, token sẽ bị xóa trên server và nhân viên cần đăng nhập lại.

* **Endpoint:** `POST /api/mobile/auth/logout`
* **Headers:**
  * `X-Api-Key: <api_key>`
  * `Authorization: Bearer <token>`
  * `Content-Type: application/json`

**Bảng tham số Request:**
| Field | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `otp` | string | **Có** | Mã OTP 6 số nhận từ email quản lý |

**Ví dụ Request:**
```bash
curl -X POST https://<domain>/api/mobile/auth/logout \
  -H "X-Api-Key: <api_key>" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"otp": "482937"}'
```

**Response thành công (200 OK):**
```json
{
  "success": true,
  "message": "Dang xuat thanh cong"
}
```

**Response lỗi — OTP sai hoặc hết hạn (401 Unauthorized):**
```json
{
  "success": false,
  "message": "Ma OTP khong hop le hoac da het han"
}
```

**Response lỗi — validation (422 Unprocessable Entity):**
```json
{
  "success": false,
  "message": "Validation error",
  "errors": {
    "otp": ["The otp field is required."]
  }
}
```

> [!IMPORTANT]
> Sau khi logout thành công, app **phải xóa token** đã lưu trong persistent storage (`SharedPreferences` / `UserDefaults`) và chuyển về màn hình đăng nhập.

---

### 3.5. Upload Ghi âm Cuộc gọi
* **Yêu cầu:** Đã xác thực (có Bearer token)
* Gửi sau khi mỗi cuộc gọi kết thúc (hoặc batch gửi khi có mạng).

* **Endpoint:** `POST /api/mobile/call-records`
* **Headers:**
  * `X-Api-Key: <api_key>`
  * `Authorization: Bearer <token>`
  * `Content-Type: multipart/form-data`

**Bảng tham số Request Body:**
| Field | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `recording` | file | **Có** | File ghi âm (`mp3`, `wav`, `m4a`, `amr`...). Tối đa **50MB** |
| `phone_number_from` | string | Không | SĐT người gọi |
| `phone_number_to` | string | Không | SĐT người nhận cuộc gọi |
| `call_type` | string | Không | `incoming` = cuộc gọi đến, `outgoing` = cuộc gọi đi |
| `duration` | int | Không | Thời lượng cuộc gọi (giây). Mặc định: 0 |
| `call_at` | string | Không | Thời điểm cuộc gọi. Format: `YYYY-MM-DD HH:mm:ss` |

> [!NOTE]
> `user_id` **không cần truyền** — server tự động lấy từ Bearer token.

**Quy tắc xác định phone_number_from / phone_number_to:**
| Loại cuộc gọi | `call_type` | `phone_number_from` | `phone_number_to` |
|---|---|---|---|
| NV gọi đi cho KH | `outgoing` | SĐT nhân viên | SĐT khách hàng |
| KH gọi đến NV | `incoming` | SĐT khách hàng | SĐT nhân viên |

**Ví dụ Request (cURL):**
```bash
curl -X POST https://<domain>/api/mobile/call-records \
  -H "X-Api-Key: <api_key>" \
  -H "Authorization: Bearer <token>" \
  -F "phone_number_from=0901234567" \
  -F "phone_number_to=0987654321" \
  -F "call_type=outgoing" \
  -F "duration=185" \
  -F "call_at=2026-08-01 14:30:00" \
  -F "recording=@/path/to/recording.mp3"
```

**Response thành công (201 Created):**
```json
{
  "success": true,
  "message": "Upload thanh cong",
  "data": {
    "id": 1,
    "user_id": 15,
    "phone_number_from": "0901234567",
    "phone_number_to": "0987654321",
    "call_type": "outgoing",
    "duration": 185,
    "recording_file": "call-recordings/2026/08/call_15_1722506400.mp3",
    "call_at": "2026-08-01 14:30:00",
    "created_at": "2026-08-01T14:35:00.000000Z",
    "updated_at": "2026-08-01T14:35:00.000000Z"
  }
}
```

**Response lỗi validation (422 Unprocessable Entity):**
```json
{
  "success": false,
  "message": "Validation error",
  "errors": {
    "call_type": ["The selected call type is invalid."],
    "recording": ["The recording must be a file."]
  }
}
```

---

## 4. Bảng mã lỗi HTTP

| Status | Ý nghĩa |
|---|---|
| **200** | Thành công (POST auth) |
| **201** | Tạo thành công (POST upload) |
| **401** | Chưa xác thực / Token sai / OTP sai hoặc hết hạn |
| **404** | Không tìm thấy nhân viên |
| **422** | Dữ liệu không hợp lệ (xem `errors` trong response) |
| **500** | Lỗi server |

---

## 5. Flow Tích hợp trên App

```
┌─────────────────────────────────────────────────────┐
│ 1. CÀI ĐẶT LẦN ĐẦU (ĐĂNG NHẬP)                      │
│                                                     │
│    Nhập ID nhân viên                                │
│         │                                           │
│         ▼                                           │
│    POST /auth/request-otp                           │
│         │                                           │
│         ▼                                           │
│    Server gửi OTP về email quản lý                  │
│         │                                           │
│         ▼                                           │
│    Quản lý nhập OTP vào app                         │
│         │                                           │
│         ▼                                           │
│    POST /auth/verify-otp                            │
│         │                                           │
│         ▼                                           │
│    Nhận token + thông tin NV ──> Lưu vào app        │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│ 2. UPLOAD LÊN SERVER                                │
│    POST /call-records (kèm Bearer token)            │
│    ──> Gửi file ghi âm + metadata                   │
│    ──> Nếu thất bại (mất mạng) ──> retry sau        │
│    ──> Nếu 401 ──> xác thực lại (OTP mới)           │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│ 3. ĐĂNG XUẤT (CẦN XÁC THỰC OTP)                     │
│                                                     │
│    Nhân viên bấm "Đăng xuất"                        │
│         │                                           │
│         ▼                                           │
│    POST /auth/logout/request-otp (kèm Bearer token) │
│         │                                           │
│         ▼                                           │
│    Server gửi OTP về email quản lý                  │
│         │                                           │
│         ▼                                           │
│    Quản lý nhập OTP vào app                         │
│         │                                           │
│         ▼                                           │
│    POST /auth/logout (kèm Bearer token + OTP)       │
│         │                                           │
│         ▼                                           │
│    Xóa token ──> Chuyển về màn hình đăng nhập       │
└─────────────────────────────────────────────────────┘
```

---

## 6. Lưu ý quan trọng cho Team Mobile

1. **Lưu `token` persistent** (`SharedPreferences` / `UserDefaults`) — token có hiệu lực lâu dài, chỉ mất khi nhân viên xác thực lại trên thiết bị khác hoặc khi logout thành công.
2. **Xử lý 401:** Khi nhận HTTP 401 từ bất kỳ API nào ➔ chuyển về màn hình nhập ID + request OTP lại.
3. **Logout cần OTP:** Đăng xuất yêu cầu xác thực OTP (giống đăng nhập). Sau khi logout thành công, xóa token trong persistent storage và chuyển về màn hình đăng nhập.
4. **Xử lý offline:** Nếu không có mạng khi cuộc gọi kết thúc, lưu vào hàng đợi local, tự động gửi khi có mạng.
5. **Retry logic:** Nếu upload thất bại (timeout, 500), retry tối đa 3 lần với khoảng cách tăng dần (5s, 15s, 30s).
6. **File ghi âm tối đa 50MB:** Nếu cuộc gọi quá dài, cân nhắc nén file trước khi gửi.
7. **Format thời gian `call_at`:** Dùng `YYYY-MM-DD HH:mm:ss` theo timezone `Asia/Ho_Chi_Minh`.
