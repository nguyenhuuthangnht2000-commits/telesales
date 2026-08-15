# Kế hoạch Triển khai: Xác thực OTP khi Tắt Dịch vụ Ghi âm trên HomeScreen

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bổ sung cơ chế bảo vệ toggle tắt dịch vụ ghi âm trên HomeScreen bằng quy trình xác thực mã OTP gửi về Email của Quản lý (2 bước: Xác nhận -> Nhập OTP 6 số).

**Architecture:** Mở rộng `MainScreenViewModel` để quản lý các state OTP (`requestStopServiceOtpState`, `verifyStopServiceOtpState`, `stopServiceOtpInput`, `stopServiceOtpError`), cập nhật `HomeScreenContent` nhận các callback tương tác rõ ràng, và bổ sung các Dialog xác nhận + Dialog OTP trực tiếp trên giao diện `MainScreen`/`HomeScreenContent`.

**Tech Stack:** Jetpack Compose, Material3, Kotlin Coroutines & StateFlow, Clean Architecture UseCases (`RequestOtpUseCase`, `VerifyOtpUseCase`).

## Global Constraints
- Tất cả văn bản hiển thị trên UI phải dùng Tiếng Việt và khai báo trong `res/values/strings.xml`.
- Không hardcode màu sắc hoặc kích thước trên UI Composable.
- Tuân thủ quy tắc `launchSafe` kết hợp `withContext(Dispatchers.IO)` trong ViewModel.

---

### Task 1: Khai báo String Resources cho Luồng Tắt Dịch vụ

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: String IDs `home_service_stop_confirm_title`, `home_service_stop_confirm_msg`, `home_service_stop_confirm_btn`, `home_service_stop_otp_desc`, `home_service_stop_otp_confirm`, `home_service_stop_cancel`, `home_service_stop_success`, `home_service_start_success`.

- [x] **Step 1: Thêm string resources vào `app/src/main/res/values/strings.xml`**

```xml
    <!-- Stop Service OTP Strings -->
    <string name="home_service_stop_confirm_title">Xác nhận Tạm dừng Dịch vụ</string>
    <string name="home_service_stop_confirm_msg">Bạn có chắc chắn muốn tạm dừng dịch vụ ghi âm cuộc gọi? Thao tác này cần mã OTP xác nhận từ Quản lý.</string>
    <string name="home_service_stop_confirm_btn">Gửi OTP &amp; Tiếp tục</string>
    <string name="home_service_stop_otp_desc">Vui lòng nhập mã OTP 6 chữ số đã được gửi đến email quản lý để xác nhận tạm dừng dịch vụ.</string>
    <string name="home_service_stop_otp_confirm">Xác nhận</string>
    <string name="home_service_stop_cancel">Hủy</string>
    <string name="home_service_stop_success">Đã tạm dừng dịch vụ ghi âm cuộc gọi.</string>
    <string name="home_service_start_success">Đã kích hoạt dịch vụ ghi âm cuộc gọi.</string>
```

- [x] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat(strings): add string resources for stop service OTP flow"
```

---

### Task 2: Cập nhật `MainScreenViewModel` Quản lý Luồng OTP Tắt Dịch Vụ

**Files:**
- Modify: `app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/MainScreenViewModel.kt`

**Interfaces:**
- Consumes: `ServiceLocator.requestOtpUseCase`, `ServiceLocator.verifyOtpUseCase`
- Produces:
  - `requestStopServiceOtpState: StateFlow<Resource<String>>`
  - `verifyStopServiceOtpState: StateFlow<Resource<UserSession>>`
  - `stopServiceOtpInput: StateFlow<String>`
  - `stopServiceOtpError: StateFlow<String?>`
  - `onStopServiceOtpChanged(input: String)`
  - `requestStopServiceOtp(userId: Int)`
  - `verifyStopServiceOtp(userId: Int, onSuccess: () -> Unit)`
  - `resetStopServiceOtpState()`

- [x] **Step 1: Khai báo StateFlows và phương thức xử lý OTP trong `MainScreenViewModel.kt`**

```kotlin
    private val requestOtpUseCase = ServiceLocator.requestOtpUseCase
    private val verifyOtpUseCase = ServiceLocator.verifyOtpUseCase

    private val _requestStopServiceOtpState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val requestStopServiceOtpState: StateFlow<Resource<String>> = _requestStopServiceOtpState

    private val _verifyStopServiceOtpState = MutableStateFlow<Resource<UserSession>>(Resource.Idle)
    val verifyStopServiceOtpState: StateFlow<Resource<UserSession>> = _verifyStopServiceOtpState

    private val _stopServiceOtpInput = MutableStateFlow("")
    val stopServiceOtpInput: StateFlow<String> = _stopServiceOtpInput

    private val _stopServiceOtpError = MutableStateFlow<String?>(null)
    val stopServiceOtpError: StateFlow<String?> = _stopServiceOtpError

    fun onStopServiceOtpChanged(input: String) {
        if (input.length <= 6 && input.all { it.isDigit() }) {
            _stopServiceOtpInput.value = input
            if (_stopServiceOtpError.value != null) {
                _stopServiceOtpError.value = null
            }
        }
    }

    fun requestStopServiceOtp(userId: Int) {
        _requestStopServiceOtpState.value = Resource.Loading
        launchSafe(onError = { error -> _requestStopServiceOtpState.value = error }) {
            val result = withContext(Dispatchers.IO) { requestOtpUseCase(userId.toString()) }
            _requestStopServiceOtpState.value = result
        }
    }

    fun verifyStopServiceOtp(userId: Int, onSuccess: () -> Unit) {
        val otp = _stopServiceOtpInput.value
        _verifyStopServiceOtpState.value = Resource.Loading
        launchSafe(onError = { error -> _verifyStopServiceOtpState.value = error }) {
            val result = withContext(Dispatchers.IO) { verifyOtpUseCase(userId, otp) }
            if (result is Resource.Error && result.source == com.nhakhoaquangninh.telesales.domain.common.ErrorSource.APP_CLIENT) {
                _stopServiceOtpError.value = result.message
            } else if (result is Resource.Success) {
                onSuccess()
            }
            _verifyStopServiceOtpState.value = result
        }
    }

    fun resetStopServiceOtpState() {
        _requestStopServiceOtpState.value = Resource.Idle
        _verifyStopServiceOtpState.value = Resource.Idle
        _stopServiceOtpInput.value = ""
        _stopServiceOtpError.value = null
    }
```

- [x] **Step 2: Commit**

```bash
git add app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/MainScreenViewModel.kt
git commit -m "feat(viewmodel): add stop service OTP handling in MainScreenViewModel"
```

---

### Task 3: Cập nhật `HomeScreenContent` và `MainScreen` Tích hợp Dialog Xác nhận & Nhập OTP

**Files:**
- Modify: `app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/components/HomeScreenContent.kt`
- Modify: `app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/MainScreen.kt`

**Interfaces:**
- Consumes: Task 1 strings, Task 2 ViewModel methods & states.
- Produces: UI hỗ trợ gạt Switch bật ngay / gạt tắt hiện dialog OTP.

- [x] **Step 1: Cập nhật `HomeScreenContent.kt`**
  - Chỉnh sửa `onToggleService: (Boolean) -> Unit` để khi `checked = isServiceRunning`, khi người dùng bấm switch:
    - Nếu `isServiceRunning == true` (đang chạy) và người dùng bấm gạt: gọi `onToggleService(false)`.
    - Nếu `isServiceRunning == false` (đang tắt) và người dùng bấm gạt: gọi `onToggleService(true)`.

- [x] **Step 2: Cập nhật `MainScreen.kt` tích hợp Dialogs**
  - Khai báo states:
    - `var showConfirmStopServiceDialog by rememberSaveable { mutableStateOf(false) }`
    - `var showStopServiceOtpDialog by rememberSaveable { mutableStateOf(false) }`
  - Lắng nghe `requestStopServiceOtpState`: Khi chuyển sang `Resource.Success`, tự động đóng `showConfirmStopServiceDialog` và mở `showStopServiceOtpDialog`.
  - Triển khai Dialog 1: `AlertDialog` xác nhận tắt dịch vụ (`showConfirmStopServiceDialog`).
  - Triển khai Dialog 2: `AlertDialog` nhập OTP (`showStopServiceOtpDialog`) sử dụng component `OtpSixDigitInput`.
  - Khi xác thực thành công: Dừng service (`context.stopService(intent)`), set `isServiceRunning = false`, reset OTP state, hiển thị toast thành công.

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/components/HomeScreenContent.kt app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/MainScreen.kt
git commit -m "feat(ui): integrate OTP confirmation and verification dialogs on HomeScreen service toggle"
```

---

### Task 4: Ghi nhận Nhật ký Cập nhật (`UPDATE_SUMMARY.md`)

**Files:**
- Modify: `UPDATE_SUMMARY.md`

- [x] **Step 1: Thêm mục 24 vào `UPDATE_SUMMARY.md` tóm tắt toàn bộ thay đổi.**
- [x] **Step 2: Commit**

```bash
git add UPDATE_SUMMARY.md
git commit -m "docs: record section 24 in UPDATE_SUMMARY.md for stop service OTP verification"
```
