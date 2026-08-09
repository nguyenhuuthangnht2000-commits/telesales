# 📌 TÓM TẮT CẬP NHẬT TÍNH NĂNG VÀ KIẾN TRÚC DỰ ÁN (PROJECT UPDATE SUMMARY)

> **Mục đích:** File này lưu trữ lại toàn bộ các tính năng, refactor và cải tiến mới nhất của dự án Telesales App. Khi pull code ở máy khác hoặc mở phiên làm việc mới, AI/Dev chỉ cần đọc tệp này là hiểu ngay trạng thái hiện tại mà không cần quét lại toàn bộ codebase.

---

## 1. 📞 Trích Xuất Dữ Liệu Cuộc Gọi GSM (Call Log Metadata)
- **Cơ chế:** Khi cuộc gọi kết thúc, `CallStateReceiver.kt` tự động đọc `CallLog.Calls` từ Android để lấy thông tin thực tế:
  - `phoneNumberFrom` / `phoneNumberTo`: Số điện thoại gọi đến hoặc gọi đi.
  - `callType`: Loại cuộc gọi (`"incoming"` / `"outgoing"`).
  - `durationSeconds`: Thời lượng cuộc gọi tính theo giây.
- **Lưu trữ Cục bộ (Local Persistence):** `SyncStatusManager.kt` lưu thông tin metadata dưới dạng chuỗi JSON gắn liền với tên file ghi âm, bảo toàn dữ liệu ngay cả khi ứng dụng bị khởi động lại.
- **Giao diện (UI Lịch sử):** `HistoryScreenContent.kt` hiển thị số điện thoại thực tế làm tiêu đề chính, thời lượng cuộc gọi (`mm:ss`), và biểu tượng gọi đến/gọi đi chuẩn xác.

---

## 2. ⚡ Quản Lý Luồng & Bắt Lỗi An Toàn (LaunchSafe & IO Dispatcher)
- **Quy tắc mới (Rule #11):** Bắt buộc sử dụng `launchSafe` kết hợp `withContext(Dispatchers.IO)` cho mọi tác vụ trong ViewModel.
- **Refactor ViewModel:**
  - `MainScreenViewModel.kt`: Chuyển tác vụ đọc `MediaStore` và quét File hệ thống về `Dispatchers.IO` ngầm, tránh đứt quãng UI Thread (ANR). Đã dùng metadata chuẩn khi upload thủ công.
  - `LoginViewModel.kt` & `OtpVerifyViewModel.kt`: Bao bọc các UseCase API trong `withContext(Dispatchers.IO)` để chạy ngầm an toàn.

---

## 3. 🌐 Cơ Chế Upload Ngầm & Offline Caching
- **WorkManager Auto-Sync:** `UploadAudioWorker` được thiết lập `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)`.
- **Offline Mode:** Nếu thiết bị tắt Wifi/4G khi cuộc gọi kết thúc, file ghi âm được lưu tạm thời ở trạng thái `PENDING`. Ngay khi thiết bị kết nối lại Internet, Android sẽ tự động kích hoạt `WorkManager` để đẩy file lên Server ngầm.

---

## 4. 🧹 Clean Code & Bảo Mật Dữ Liệu
- **Memory Leak Fix:** `ServiceLocator.kt` sử dụng `applicationContext` để khởi tạo các Singleton.
- **API Version Sync:** Đồng bộ `compileSdk = 36` thống nhất giữa các module (`:app`, `:data`, `:core`).
- **Modern Android APIs:** `WarningActivity.kt` chuyển sang dùng `ComponentActivity` hiện đại với `OnBackPressedDispatcher`.
- **Giao diện 100% Tiếng Việt:** Tất cả chuỗi hiển thị được đưa vào `strings.xml`.

---

## 5. 🎨 Nâng cấp Giao diện Jetpack Compose
- **WarningActivity:** Chuyển đổi toàn bộ UI từ cơ chế View System/LinearLayout cũ sang Jetpack Compose theo bản thiết kế chuẩn. Bổ sung các token màu `TertiaryContainer` và `TertiaryFixedDim` vào tệp `Color.kt` tập trung để tuân thủ quy tắc quản lý màu sắc, đồng thời bảo tồn toàn bộ tính năng cốt lõi (chặn nút Back, chuông báo động, theo dõi vi phạm).

## 6. 🐛 Sửa Lỗi Metadata Cuộc Gọi (CallLog Fallback & Retroactive)
- **Vấn đề 1 (Upload tự động):** Ứng dụng thi thoảng báo sai loại cuộc gọi, thời lượng = 0, hoặc thiếu số điện thoại gọi đến/đi khi bắt sự kiện live. Nguyên nhân do hệ điều hành ghi dữ liệu vào `CallLog` bị trễ (hơn 3 giây sau khi cúp máy).
- **Vấn đề 2 (Upload thủ công):** Khi bấm nút tải lên thủ công ở màn hình danh sách các file cũ, những file chưa từng được bắt đúng metadata cũng bị thiếu toàn bộ thông tin (do file ghi âm cũ không có metadata lưu lại).
- **Giải pháp:**
  - Ở `CallStateReceiver`: Tự động ghi nhận `isIncomingCall`, `callStartTime`, và `callEndTime` thủ công thông qua Broadcast Receiver. Nếu `CallLog` chưa kịp cập nhật, app sẽ tự động tính toán thời lượng thực tế và phân bổ chính xác `phoneNumberFrom` / `phoneNumberTo`.
  - Ở `MainScreenViewModel`: Xây dựng cơ chế *Retroactive CallLog Matching* (quét hồi tố CallLog). Đối với các file đang bị thiếu metadata, app sẽ tự động lục lại lịch sử danh bạ hệ thống và khớp với thời gian sửa đổi (Modified Time) của file ghi âm (với sai số 60s) để tự động điền lại toàn bộ thông tin gốc chính xác 100%.

---
## 7. 🔐 Checkpoint 1 — Bảo mật dữ liệu và cấu hình release
- API key không còn nằm trong source; cấp bằng Gradle property hoặc biến môi trường `TELESALES_API_KEY`. Debug/test cho phép rỗng, release build bắt buộc phải cấu hình.
- Phiên đăng nhập được mã hóa AES-256-GCM bằng Android Keystore, tự migration SharedPreferences cũ và xóa phiên khi dữ liệu/key không thể giải mã.
- Debug chỉ log HTTP mức BASIC và che `Authorization`/`X-Api-Key`; release không gắn HTTP logger.
- Tắt backup, chặn cleartext HTTP, thu hẹp receiver nội bộ, đặt notification ở chế độ PRIVATE và xóa PII khỏi log/notification.
- Release bật R8, không dùng debug signing; cấu hình ký nhận từ các Gradle property `TELESALES_RELEASE_*` ngoài repository.
- Metadata cuộc gọi local tự dọn sau 30 ngày; JSON legacy được đóng dấu retention ở lần đọc đầu tiên.
- Verification: security tests 6/6 pass, TTL tests 3/3 pass, `lintDebug` và `assembleDebug` thành công.
## 8. Checkpoint 2 — Chuẩn hóa type, resource và dependency
- Thay magic string bằng CallType, FailureReason và HistoryFilter; dữ liệu gửi backend/lưu JSON dùng wireValue ổn định.
- Toàn bộ màu UI nằm trong Color.kt/MaterialTheme; toàn bộ dp/sp trong UI dùng token tập trung tại Dimens.kt.
- Chuỗi hiển thị, content description, Toast và notification được chuyển sang strings.xml bằng tiếng Việt.
- Xóa Stringee/JJWT không còn consumer; tập trung Compose, WorkManager, Coroutines, Retrofit và OkHttp vào version catalog.
- BaseViewModel ném lại CancellationException và phát unauthorized bằng tryEmit.
- Metadata đồng bộ JSON bị hỏng được xóa khi đọc, tránh lưu vô thời hạn.
- Metadata legacy chỉ có trạng thái không còn bị suy diễn thành cuộc gọi đi; CallLog duration = 0 chỉ được kết luận không kết nối sau lần retry cuối.
- Verification: 12 focused tests pass; lintDebug và assembleDebug thành công. Lint còn 36 warning đã phân loại cho checkpoint hardening.
## 9. Checkpoint 3 — Tái cấu trúc luồng cuộc gọi (Call Flow Architecture)
- `CallStateReceiver` chỉ parse broadcast và forward — không còn file I/O hay network trực tiếp trong receiver.
- Tách thành 6 component riêng biệt: `CallSessionTracker` (state machine), `CallLogDataSource` (truy vấn CallLog), `RecordingLocator` (tìm file qua MediaStore URI), `CallEventCoordinator` (phân loại + schedule), `ComplianceNotifier` (notification), `UploadScheduler` (WorkManager).
- Loại bỏ hoàn toàn `MediaStore.DATA`, `requestLegacyExternalStorage` và quét `/storage/emulated/0`. Dùng scoped storage `content://` URI.
- `RecordingUriValidator` kiểm tra scheme, MIME type, kích thước (≤ 50MB), và readable trước khi upload.
- `UploadAudioWorker` bọc `try/finally` đảm bảo không kẹt trạng thái `UPLOADING`.
- ViewModel không truy cập MediaStore/CallLog/filesystem trực tiếp — ủy quyền cho Repository/DataSource.
## 10. Checkpoint 4 — Dọn dẹp deprecated API và release hardening
- Thay `ActivityManager.getRunningServices()` (deprecated từ API 26) bằng `StateFlow<Boolean>` trong `TelesalesForegroundService`; `MainScreen` collect StateFlow tự động cập nhật.
- Fix `vibrator?.vibrate(1000)` deprecated: dùng `VibrationEffect.createOneShot()` cho mọi nhánh SDK ≥ 28 (minSdk).
- Chuyển `window.decorView.systemUiVisibility` deprecated và `windowInsetsController` (API 30+) sang `WindowCompat` + `WindowInsetsControllerCompat` từ AndroidX Core — một đường code duy nhất cho mọi SDK.
- Xóa dead code nhánh `< API 27` trong `WarningActivity` (unreachable vì minSdk = 28).
- Receiver registration dùng `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` chuẩn Android 14+.
- Back handling dùng `onBackPressedDispatcher.addCallback` — không còn `onBackPressed()` deprecated.
*Cập nhật lần cuối: 07/08/2026 23:28 bởi Antigravity AI Pair Programmer.*

## 11. Các lỗi phát sinh và cách khắc phục
- **Giao diện không hiển thị tên file:** Ứng dụng đã được sửa lại để luôn hiển thị tên gốc của file ghi âm thay vì tự động chuyển thành số điện thoại trong danh sách lịch sử.
- **Log API:** Cải thiện việc ghi log cho tính năng upload. Thay vì log toàn bộ nội dung file (gây quá tải Logcat), hệ thống hiện chỉ log siêu dữ liệu (metadata) trước khi upload và kết quả trả về (thành công/lỗi) từ Server.
- **Tự động làm mới UI:** Sửa lỗi giao diện không tự động tải lại sau khi upload file thành công. `UploadAudioWorker` và `ProcessCallWorker` hiện đã gửi broadcast `com.nhakhoaquangninh.telesales.REFRESH_RECORDINGS` kèm theo `setPackage(applicationContext.packageName)` để tương thích với quy định bảo mật `RECEIVER_NOT_EXPORTED` của Android 14+.

*Cập nhật lần cuối: 09/08/2026 23:45 bởi Antigravity AI Pair Programmer.*
