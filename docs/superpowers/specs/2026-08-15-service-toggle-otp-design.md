# Tài liệu Thiết kế: Xác thực OTP khi Tắt Dịch vụ Ghi âm trên HomeScreen

- **Ngày tạo:** 15/08/2026
- **Trạng thái:** Chờ phê duyệt (Draft)
- **Tác giả:** Antigravity AI Pair Programmer & User

---

## 1. 🎯 Mục tiêu & Bối cảnh (Problem & Objectives)

### Bối cảnh
Hiện tại, trên màn hình Trang chủ (`HomeScreenContent.kt`), người dùng có thể tùy ý gạt thanh Switch Toggle để bật hoặc tắt `TelesalesForegroundService`. Để đảm bảo tính tuân thủ quy trình làm việc và tránh việc nhân viên tự ý tắt ghi âm trong ca làm việc, hệ thống cần bảo vệ hành vi **Tắt dịch vụ** bằng cơ chế xác thực mã OTP gửi về Email của Quản lý (tương tự như quy trình Đăng xuất tại màn hình Cài đặt).

### Mục tiêu
1. **Mặc định:** Thanh Switch luôn ở trạng thái **BẬT (ON)** khi dịch vụ đang hoạt động.
2. **Gạt BẬT lại (OFF -> ON):** Khởi chạy `startForegroundService` ngay lập tức mà không cần xác thực OTP.
3. **Gạt TẮT (ON -> OFF):** Yêu cầu xác thực 2 bước:
   - **Bước 1:** Hiển thị Dialog Xác nhận muốn tắt dịch vụ.
   - **Bước 2:** Gửi mã OTP về email quản lý và hiển thị Dialog nhập OTP 6 chữ số.
   - **Bước 3:** Chỉ khi xác thực OTP thành công trên Server, `TelesalesForegroundService` mới bị dừng (`stopService`) và Switch chuyển về OFF. Nếu Hủy hoặc nhập sai OTP, Switch vẫn giữ nguyên trạng thái BẬT.

---

## 2. 🏗️ Kiến trúc & Thiết kế Thành phần (Architecture & Component Design)

### 2.1. Quản lý Chuỗi Giao diện (`res/values/strings.xml`)
Khai báo tập trung 100% Tiếng Việt:
- `home_service_stop_confirm_title`: *"Xác nhận Tạm dừng Dịch vụ"*
- `home_service_stop_confirm_msg`: *"Bạn có chắc chắn muốn tạm dừng dịch vụ ghi âm cuộc gọi? Thao tác này cần mã OTP xác nhận từ Quản lý."*
- `home_service_stop_confirm_btn`: *"Gửi OTP & Tiếp tục"*
- `home_service_stop_otp_desc`: *"Vui lòng nhập mã OTP 6 chữ số đã được gửi đến email quản lý để xác nhận tạm dừng dịch vụ."*
- `home_service_stop_success`: *"Đã tạm dừng dịch vụ ghi âm cuộc gọi."*
- `home_service_start_success`: *"Đã kích hoạt dịch vụ ghi âm cuộc gọi."*

---

### 2.2. Xử lý State & Logic trong `MainScreenViewModel.kt`
Mở rộng `MainScreenViewModel` để quản lý luồng OTP tắt dịch vụ (sử dụng các UseCase đã có `RequestOtpUseCase` & `VerifyOtpUseCase`):
- **StateFlows:**
  - `requestStopServiceOtpState: StateFlow<Resource<String>>`
  - `verifyStopServiceOtpState: StateFlow<Resource<UserSession>>`
  - `stopServiceOtpInput: StateFlow<String>`
  - `stopServiceOtpError: StateFlow<String?>`
- **Functions:**
  - `onStopServiceOtpChanged(input: String)`: Cập nhật chuỗi 6 ký tự số.
  - `requestStopServiceOtp(userId: Int)`: Gọi API gửi OTP về email quản lý.
  - `verifyStopServiceOtp(userId: Int, onSuccess: () -> Unit)`: Xác thực mã OTP, nếu thành công thì thực thi callback `onSuccess` để dừng Service.
  - `resetStopServiceOtpState()`: Xóa sạch input và trạng thái khi đóng Dialog.

---

### 2.3. Cập nhật Giao diện `HomeScreenContent.kt` & `MainScreen.kt`
1. **`HomeScreenContent.kt`:**
   - Nhận callback `onRequestStopService: () -> Unit` và `onStartService: () -> Unit`.
   - Khi Switch đang ON mà người dùng nhấn gạt sang OFF: Gọi `onRequestStopService()` thay vì tắt trực tiếp.
   - Khi Switch đang OFF mà người dùng nhấn gạt sang ON: Gọi `onStartService()`.
2. **Dialogs trên `MainScreen.kt` (hoặc trong `HomeScreenContent.kt`):**
   - **Dialog 1 (Xác nhận tắt):** Hiển thị `AlertDialog` thông báo lý do cần OTP. Bấm *"Gửi OTP"* ➔ Gọi ViewModel gửi OTP và chuyển sang Dialog 2.
   - **Dialog 2 (Nhập OTP):** Hiển thị `AlertDialog` chứa ô nhập OTP 6 số (`OtpSixDigitInput`). Bấm *"Xác nhận"* ➔ Xác thực OTP ➔ Nếu đúng: dừng Foreground Service, chuyển switch sang OFF, đóng popup.

---

## 3. 🔄 Sơ đồ Luồng Hoạt Động (Interaction Flow)

```
[User gạt Toggle sang OFF]
          │
          ▼
┌──────────────────────────────────────────────┐
│ Dialog 1: Xác nhận tạm dừng dịch vụ?        │
└──────────────────────────────────────────────┘
     │ (Hủy)                     │ (Bấm "Gửi OTP")
     ▼                           ▼
[Giữ Switch ON]         [Gọi RequestOtpUseCase]
                                 │
                                 ▼
                        ┌──────────────────────────────────────────────┐
                        │ Dialog 2: Nhập OTP 6 số gửi về email QL      │
                        └──────────────────────────────────────────────┘
                             │ (Hủy)                     │ (Bấm "Xác nhận")
                             ▼                           ▼
                        [Giữ Switch ON]         [Gọi VerifyOtpUseCase]
                                                         │
                                        ┌────────────────┴────────────────┐
                                        │ (Sai OTP)                       │ (Đúng OTP)
                                        ▼                                 ▼
                               [Báo lỗi đỏ trên Dialog]       [context.stopService()]
                               [Giữ Switch ON]                [Chuyển Switch sang OFF]
                                                              [Đóng Dialog & Toast thành công]
```

---

## 4. ✅ Tiêu chí Kiểm thử & Nghiệm thu (Acceptance Criteria)

1. **Khởi tạo:** Khi mở app, nếu Service đang chạy thì Toggle hiển thị màu xanh (ON).
2. **Bật lại:** Từ trạng thái OFF, gạt sang ON ➔ Service khởi chạy ngay lập tức, hiển thị Notification Service.
3. **Chặn tắt tùy tiện:** Từ trạng thái ON, gạt sang OFF ➔ Không bị tắt ngay mà mở popup Xác nhận.
4. **Hủy thao tác:** Bấm "Hủy" ở bất kỳ bước nào (Dialog xác nhận hoặc Dialog OTP) ➔ Switch vẫn giữ nguyên trạng thái ON.
5. **Sai OTP:** Nhập sai mã OTP ➔ Hiển thị dòng thông báo lỗi màu đỏ, không tắt Service.
6. **Đúng OTP:** Nhập đúng mã OTP và bấm Xác nhận ➔ Service dừng lại, thanh switch chuyển sang OFF màu xám, hiển thị Toast thông báo.
