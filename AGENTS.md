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
  4. **Giám sát & Đồng bộ:** Hiển thị notification foreground service và tự động lưu cache dữ liệu cuộc gọi cục bộ khi gặp sự cố mạng/file để tự động đồng bộ lại lên Server.

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

Tất cả các API gửi lên Server backend đều tuân thủ các quy tắc sau (chi tiết trong `huong-dan-tich-hop-api.md`):
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
6. **Ngôn ngữ Giao diện UI (100% Tiếng Việt):**
   - Tất cả văn bản hiển thị trên giao diện người dùng (tiêu đề, nhãn input, placeholder, nút bấm, thông báo lỗi, dialog, helper text) BẮT BUỘC phải sử dụng **Tiếng Việt**, chuẩn hóa theo ngữ cảnh doanh nghiệp Việt Nam (Nha Khoa Quảng Ninh). Tuyệt đối không dùng Tiếng Anh cho các văn bản hiển thị tới người dùng cuối.
7. **Quản lý Màu sắc Tập trung (Tuyệt đối KHÔNG hardcode mã màu trên UI):**
   - Tất cả mã màu (Color hex) và Design Tokens BẮT BUỘC phải được khai báo tập trung tại tệp `com.nhakhoaquangninh.telesales.theme.Color.kt` (hoặc `MaterialTheme.colorScheme` / `res/values/colors.xml`).
   - Tuyệt đối KHÔNG hardcode trực tiếp mã màu (ví dụ: `Color(0xFF005C55)`) hoặc tạo các biến màu private riêng lẻ trong từng file Composable màn hình.
8. **Tuyệt đối KHÔNG Hardcode String và Dimension (dp, sp):**
   - **String:** Tất cả các chuỗi văn bản (text) hiển thị trên UI phải được khai báo trong `res/values/strings.xml` và gọi qua `stringResource(id = R.string.xxx)`.
   - **Dimension:** Tất cả các kích thước padding, margin, size (dp), cỡ chữ (sp) phải được khai báo tập trung. Đối với Compose, khuyến khích tạo file `theme/Dimens.kt` (chứa các biến `val PaddingSmall = 8.dp`) hoặc dùng `dimensionResource` thay vì gõ trực tiếp `16.dp` hay `14.sp` vào code.
9. **Mobile-First UI Adaptation (Chuyển đổi UI Web sang Mobile):**
   - Khi tham khảo các bản thiết kế HTML/Web (ví dụ `code.html`), BẮT BUỘC phải chủ động chuyển đổi sang bố cục chuẩn của Mobile App.
   - Các thành phần dàn hàng ngang (Row/Grid) trên Web phải được chuyển thành hàng dọc (Column) trên Mobile để tránh bị tràn màn hình hoặc khó nhìn.
   - Kích thước chữ, nút bấm phải tuân thủ hướng dẫn thiết kế cảm ứng của Material 3.
10. **Chống Memory Leak và API Deprecated:**
    - **Context Leaks:** BẮT BUỘC sử dụng `context.applicationContext` khi khởi tạo các Singleton (như Repositories, Managers, ServiceLocator) thay vì truyền Activity Context, để tránh giam giữ bộ nhớ của Activity.
    - **Coroutine Scope:** Hạn chế tối đa dùng `GlobalScope`. Ưu tiên dùng `lifecycleScope` (Activity/Fragment) hoặc `viewModelScope` (ViewModel).
    - **Modern APIs:** Luôn dùng các giải pháp thay thế hiện đại của Google (VD: dùng `registerForActivityResult` thay cho `startActivityForResult`, dùng `VibrationEffect` thay cho `vibrate(long)`, sử dụng `WorkManager` thay cho Background Services truyền thống nếu không cần Foreground).
11. **Bắt buộc sử dụng `launchSafe` trong ViewModel:**
    - Tuyệt đối không dùng `viewModelScope.launch` trần (raw). Luôn luôn sử dụng hàm `launchSafe(onError = { ... }) { ... }` được cung cấp sẵn trong `BaseViewModel` để đảm bảo mọi Exception (Network, IO, Crash) đều được bắt và chuyển thành `Resource.Error` chuẩn hóa.
    - Mọi tác vụ đọc ghi file, truy vấn Database, hoặc MediaStore trong ViewModel BẮT BUỘC phải đặt trong `launchSafe` kết hợp `withContext(Dispatchers.IO)` để tránh treo giao diện (ANR).
12. **Tự động cập nhật `UPDATE_SUMMARY.md` trước khi Commit/Push:**
    - Trước khi thực hiện bất kỳ lệnh `git commit` hoặc `git push` nào, AI Assistant BẮT BUỘC phải tổng hợp tóm tắt các thay đổi, tính năng mới hoặc bug fix vừa làm và ghi lại vào tệp `UPDATE_SUMMARY.md`.
    - Giúp tiết kiệm tối đa token và thời gian khi `git pull` dự án ở một thiết bị hoặc phiên làm việc khác.
13. **Quy tắc kiểm thử:**
    - Không tự động chạy unit test, integration test, lint, build hoặc assemble.
    - Chỉ chạy kiểm thử khi người dùng yêu cầu rõ ràng.
    - Sau khi sửa code, phải thông báo rõ các bước kiểm thử chưa được chạy.

## 5. 🚀 QUY TRÌNH KIỂM TRA NHANH SAU KHI PULL CODE (POST-PULL CHECKLIST)

Mỗi khi `git pull` code mới về, thực hiện các bước sau:
1. **Đọc `UPDATE_SUMMARY.md`:** Đọc ngay tệp này để nắm bắt toàn bộ cập nhật gần nhất.
2. **Kiểm tra Gradle Sync:** Chạy `./gradlew tasks` hoặc sync project để đảm bảo không lỗi dependency.
3. **Build & Verify:**
   - Kiểm tra build thành công: `./gradlew assembleDebug`
   - Đảm bảo app không bị crash ở bước khởi tạo `ServiceLocator`.

---

## 📚 TÀI LIỆU THAM KHẢO TRONG DỰ ÁN
- [`UPDATE_SUMMARY.md`](file:///d:/telesales/UPDATE_SUMMARY.md): **Tóm tắt tổng quan toàn bộ cập nhật tính năng mới & refactor kiến trúc gần nhất (Bắt buộc đọc khi pull code về máy mới).**
- [`huong-dan-tich-hop-api.md`](file:///d:/New%20folder/TelesalesApp/huong-dan-tich-hop-api.md): Chi tiết các API Endpoint, format DTO & Upload Multipart.
- [`huong_dan_khac_phuc_service.md`](file:///d:/New%20folder/TelesalesApp/huong_dan_khac_phuc_service.md): Xử lý sự cố Foreground Service bị OS kill (Battery Optimization, Xiaomi/Samsung OEM restrictions).
- [`huong_dan_su_dung.md`](file:///d:/New%20folder/TelesalesApp/huong_dan_su_dung.md): Hướng dẫn vận hành ứng dụng cho nhân viên telesales.
