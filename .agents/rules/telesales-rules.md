# 📋 BỘ QUY TẮC DỰ ÁN & HƯỚNG DẪN BẮT ĐẦU NHANH (TELESALES APP)

> **Mục đích:** Giúp bất kỳ Developer hoặc AI Assistant nào sau khi `git pull` code mới về có thể nắm bắt ngay lập tức cấu trúc, kiến trúc, quy định code và luồng xử lý chính của dự án mà không cần mất thời gian quét (scan) lại toàn bộ dự án.

---

## 1. 🎯 TỔNG QUAN DỰ ÁN (PROJECT OVERVIEW)
- **Tên dự án:** `TelesalesApp` (Nha Khoa Quảng Ninh)
- **Mục tiêu:** Ứng dụng Android chạy ngầm ghi âm cuộc gọi telesales GSM/Cellular của nhân viên, tự động lưu trữ và đồng bộ (upload) file ghi âm cuộc gọi + lịch sử cuộc gọi lên hệ thống Server Backend.
- **Tính năng cốt lõi:**
  1. **Xác thực:** Đăng nhập theo luồng `Nhập User ID -> Yêu cầu OTP (gửi qua email quản lý) -> Xác thực OTP -> Lưu Bearer Token + API Key`.
  2. **Ghi âm cuộc gọi ngầm:** Tự động bắt sự kiện cuộc gọi (`CallStateReceiver`), khởi chạy `TelesalesForegroundService`, dùng `AudioRecorderHelper` để ghi âm cuộc gọi.
  3. **Đồng bộ dữ liệu:** Sử dụng `WorkManager` (`UploadAudioWorker`) để upload file ghi âm + thông tin cuộc gọi lên Server theo chuẩn REST API (`multipart/form-data`).
  4. **Giám sát & Cảnh báo:** Hiển thị notification foreground service và màn hình `WarningActivity` nếu bị ngắt quyền hoặc bị tắt dịch vụ ngầm.

---

## 2. 🏗️ KIẾN TRÚC VÀ CẤU TRÚC MODULE (CLEAN ARCHITECTURE)

Dự án áp dụng mô hình **Multi-Module Clean Architecture**:

```
TelesalesApp/
├── app/        # UI, Navigation, Application, Services, Receivers, Workers, DI (ServiceLocator)
├── domain/     # Pure Kotlin: Models, Repository Interfaces, UseCases, Common Resources
├── data/       # Network (Retrofit/DTOs), Local Storage (Preferences/DB), Repository Impls
└── core/       # Base Activity/ViewModel, Core Extensions, Utility classes
```

### Chi tiết các Module:
1. **`:app` (Android App Layer)**
   - **UI:** Jetpack Compose + Material3 + Navigation 3 (`androidx.navigation3.ui`).
   - **Màn hình chính:** `LoginScreen`, `OtpVerifyScreen`, `MainScreen`.
   - **Cơ chế ngầm:** `CallStateReceiver` (lắng nghe trạng thái GSM), `TelesalesForegroundService` (Foreground Service), `AudioRecorderHelper` (xử lý MediaRecorder/AudioRecord), `UploadAudioWorker` (WorkManager upload).
   - **Dependency Injection:** Khởi tạo tập trung qua `ServiceLocator.kt`.

2. **`:domain` (Business Logic Layer)**
   - Không chứa bất kỳ thư viện Android UI nào (Pure Kotlin).
   - **Models:** `User`, `AuthToken`, `CallLogItem`, v.v.
   - **UseCases:** Thao tác nghiệp vụ đơn nhiệm (`LoginUseCase`, `VerifyOtpUseCase`, `UploadCallLogUseCase`).
   - **Common:** `Resource<T>` (`Success`, `Error`, `Loading`) để wrap kết quả trả về.

3. **`:data` (Data & Network Layer)**
   - **Remote:** `ApiService` (Retrofit), `AuthDto`, `BaseResponse`.
   - **Repository Implementation:** Thực thi các interface định nghĩa ở `:domain` (vd: `AuthRepositoryImpl`).
   - **Local:** Quản lý lưu trữ token, cấu hình bằng SharedPreferences/Encrypted Preferences.

4. **`:core` (Core Utilities Layer)**
   - `BaseActivity`, `BaseViewModel`, helper data classes (`Quadruple`, v.v.).

---

## 3. 🔐 CƠ CHẾ XÁC THỰC VÀ BẢO MẬT API

Tất cả các API gửi lên Server backend đều tuân thủ các quy tắc sau:
- **Base URL:** `https://<domain>/api/mobile`
- **Header bắt buộc:**
  - `X-Api-Key: <mobile_api_key>` (TẤT CẢ các request)
  - `Authorization: Bearer <token>` (Tất cả request sau khi đăng nhập thành công)
- **Luồng Auth:** `POST /auth/request-otp` -> `POST /auth/verify-otp` -> Lưu Token vĩnh viễn.

---

## 4. ⚙️ QUY TẮC PHÁT TRIỂN & CHUẨN CODE (DEVELOPMENT RULES)

### 🔴 Quy tắc Cốt lõi:
1. **Tuyệt đối không sử dụng Stringee VoIP SDK cho cuộc gọi GSM:**
   - Ứng dụng tập trung ghi âm cuộc gọi GSM thông thường qua SIM điện thoại (`CallStateReceiver` & `AudioRecorderHelper`). Các lớp Stringee SDK đã được refactor dọn dẹp khỏi codebase.
2. **Quản lý Quyền Nguy hiểm (Runtime Permissions):**
   - Khi chỉnh sửa tính năng ghi âm, luôn kiểm tra các quyền: `RECORD_AUDIO`, `READ_PHONE_STATE`, `READ_CALL_LOG`, `POST_NOTIFICATIONS`.
3. **Cấu hình Legacy JNI Packaging:**
   - Trong `app/build.gradle.kts`, giữ thuộc tính `useLegacyPackaging = true` để tương thích với cơ chế 16KB Page Alignment trên các thiết bị Android 15+.
4. **Quản lý DI:**
   - Không tự ý thêm Hilt/Koin nếu chưa có sự đồng ý của Team Leader. Tất cả dependency injection hiện tại được đăng ký gọn nhẹ qua `ServiceLocator.kt`.
5. **Xử lý Lỗi trên UI:**
   - Sử dụng `ErrorDialog` trong `ui/components/ErrorDialog.kt` để hiển thị thông báo lỗi đồng nhất cho người dùng.

---

## 5. 🚀 QUY TRÌNH KIỂM TRA NHANH SAU KHI PULL CODE (POST-PULL CHECKLIST)

Mỗi khi `git pull` code mới về, thực hiện các bước sau:
1. **Kiểm tra Gradle Sync:** Sync project để đảm bảo không lỗi dependency.
2. **Kiểm tra các tệp thay đổi:** Xem tệp `huong-dan-tich-hop-api.md` hoặc các thay đổi trong `app/`, `data/`, `domain/` để biết có API/UI nào mới.
3. **Build & Verify:** Đảm bảo project build thành công không có lỗi syntax hay import missing.
