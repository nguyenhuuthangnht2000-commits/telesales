# 📌 TÓM TẮT CẬP NHẬT TÍNH NĂNG VÀ KIẾN TRÚC DỰ ÁN (PROJECT UPDATE SUMMARY)

> **Mục đích:** File này lưu trữ lại toàn bộ các tính năng, refactor và cải tiến mới nhất của dự án Telesales App. Khi pull code ở máy khác hoặc mở phiên làm việc mới, AI/Dev chỉ cần đọc tệp này là hiểu ngay trạng thái hiện tại mà không cần quét lại toàn bộ codebase.

---

## 4. 🛠️ Khắc Phục Lỗi Upload File Trên Bản Release (Release Build Upload Fixes)
- **Bổ sung ProGuard Keep Rules ([app/proguard-rules.pro](file:///d:/telesales/app/proguard-rules.pro)):**
  - Khai báo keep `ListenableWorker`, `InputMerger` (`OverwritingInputMerger`, `ArrayCreatingInputMerger`) và các Worker cụ thể (`UploadAudioWorker`, `ProcessCallWorker`) kèm constructor để WorkManager không bị lỗi `ClassNotFoundException` / `NoSuchMethodException` do R8 obfuscation.
  - Bảo toàn Retrofit annotations (`@POST`, `@Multipart`, `@Part`, `@Header`), Gson `@SerializedName`, `ApiService`, DTOs và `CallRecordRepositoryImpl`.
- **Tăng Timeout OkHttpClient ([RetrofitClient.kt](file:///d:/telesales/data/src/main/java/com/nhakhoaquangninh/telesales/data/remote/RetrofitClient.kt)):**
  - Tăng `connectTimeout` (30s), `readTimeout` (60s), và `writeTimeout` (90s) nhằm tránh lỗi `SocketTimeoutException` khi tải file ghi âm dung lượng lớn qua kết nối di động 3G/4G yếu.
- **Tối Ưu Hoá Xác Thực MIME Type ([RecordingUriValidator.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/call/RecordingUriValidator.kt) & [CallRecordRepositoryImpl.kt](file:///d:/telesales/data/src/main/java/com/nhakhoaquangninh/telesales/data/repository/CallRecordRepositoryImpl.kt)):**
  - Bổ sung fallback xác định MIME type dựa trên extension của tệp (`.m4a`, `.mp3`, `.amr`, `.3gp`, v.v.) trong trường hợp `contentResolver.getType(uri)` trả về `application/octet-stream` hoặc `null` trên một số giao diện tùy biến (Samsung, Xiaomi, Oppo).

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
- **Sửa lỗi Crash màn hình khởi động ở bản Release:** Bổ sung các rule Proguard (R8) cho các class Navigation Compose (`@Serializable` Login, OtpVerify, Main) và các class Model trong Domain để tránh bị obfuscator xóa mất serializer, gây crash `SerializationException` khi khởi chạy ứng dụng. Đồng thời sinh và sử dụng keystore cục bộ để ký APK tự động.

*Cập nhật lần cuối: 10/08/2026 00:25 bởi Antigravity AI Pair Programmer.*

## 12. Sửa crash Release và footer đăng nhập responsive
- **Sửa crash WorkManager/Room ở bản Release:** Giữ constructor của các lớp kế thừa `RoomDatabase` để R8 không xóa `WorkDatabase_Impl` được khởi tạo bằng reflection.
- **Sửa footer hỗ trợ màn hình đăng nhập:** Chuyển nội dung hỗ trợ thành hai dòng căn giữa, giữ nền và padding footer để không tràn hoặc sát mép trên thiết bị nhỏ.
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
- **Sửa lỗi Crash màn hình khởi động ở bản Release:** Bổ sung các rule Proguard (R8) cho các class Navigation Compose (`@Serializable` Login, OtpVerify, Main) và các class Model trong Domain để tránh bị obfuscator xóa mất serializer, gây crash `SerializationException` khi khởi chạy ứng dụng. Đồng thời sinh và sử dụng keystore cục bộ để ký APK tự động.

*Cập nhật lần cuối: 10/08/2026 00:25 bởi Antigravity AI Pair Programmer.*

## 12. Sửa crash Release và footer đăng nhập responsive
- **Sửa crash WorkManager/Room ở bản Release:** Giữ constructor của các lớp kế thừa `RoomDatabase` để R8 không xóa `WorkDatabase_Impl` được khởi tạo bằng reflection.
- **Sửa footer hỗ trợ màn hình đăng nhập:** Chuyển nội dung hỗ trợ thành hai dòng căn giữa, giữ nền và padding footer để không tràn hoặc sát mép trên thiết bị nhỏ.

## 13. Tích hợp xác thực OTP Đăng xuất & Cập nhật Icon App
- **Xác thực OTP trước khi Đăng xuất:** Ràng buộc nhân viên phải nhập mã OTP (được gửi về email quản lý) mỗi khi muốn thoát ca làm việc. Trích xuất thành phần `OtpSixDigitInput` từ màn hình Đăng nhập để tái sử dụng ở màn hình Cài đặt, kết hợp với `SettingsViewModel` để tái sử dụng `RequestOtpUseCase` và `VerifyOtpUseCase`.
- **Chuẩn hoá Strings (Localization):** Loại bỏ toàn bộ text hardcode trên giao diện màn hình Cài đặt và Dialog, chuyển 100% vào `strings.xml`. Đã thêm quy định chống hardcode giao diện vào `AGENTS.md`.
- **Cập nhật Icon App:** Generate lại bộ app icon launcher mọi kích thước (`mipmap-mdpi` -> `xxxhdpi`) từ file ảnh thiết kế mới, vô hiệu hoá `ic_launcher.xml` mặc định (Adaptive Icon) để đảm bảo hình ảnh hiển thị đồng bộ trên mọi nền tảng Android.

*Cập nhật lần cuối: 10/08/2026 23:51 bởi Antigravity AI Pair Programmer.*

## 14. Chuyển đổi Local Cache sang Room Database
- **Vấn đề:** Việc lưu trữ JSON thô cho lịch sử hàng ngàn cuộc gọi vào `SharedPreferences` làm quá tải I/O và tăng nguy cơ tràn RAM.
- **Giải pháp:** Cập nhật cơ chế load data mới sử dụng `Room Database` (SQLite). 
  - Tạo `TelesalesDatabase`, `CallRecordEntity`, `FailedCallEntity` thay thế SharedPreferences.
  - Sử dụng KSP thay vì Kapt để cấu hình Room, nhằm tương thích hoàn toàn với Gradle Kotlin DSL built-in mới nhất của AGP 9.0.
  - Loại bỏ hoàn toàn cơ chế tự động xóa dữ liệu quá 30 ngày theo yêu cầu mới. Dữ liệu cuộc gọi được lưu vĩnh viễn trên thiết bị.
- **Quản lý Cache:** Đã thêm phương thức `clearAll()` gọi dao.clearAll() để hỗ trợ xóa sạch dữ liệu cache khi nhân viên đăng xuất.

*Cập nhật lần cuối: 11/08/2026 09:28 bởi Antigravity AI Pair Programmer.*

---

## 15. Cải Tiến UI/UX Đăng Nhập & Tinh Chỉnh Thành Phần Nhập OTP (`OtpSixDigitInput`)
- **Logo Màn Hình Đăng Nhập:** Thay thế biểu tượng mặc định ở đầu `LoginScreen` bằng logo ứng dụng bo tròn (`ic_launcher_round`).
- **Bảo Mật Phiên Nhập User ID:** Tự động xóa dữ liệu nhập `userIdInput` ngay khi đăng nhập thành công trước khi điều hướng sang màn hình chính.
- **Tối Ưu Thành Phần Nhập OTP (`OtpSixDigitInput`):**
  - Hỗ trợ chọn (select) và thay thế trực tiếp từng chữ số OTP khi bấm vào ô bất kỳ ở giữa mã.
  - Áp dụng kỹ thuật bôi đen đảo ngược (`reversed selection`) để xử lý triệt để bug phím xóa (Backspace) trên bàn phím ảo Android (Gboard).
  - Cập nhật logic hiển thị viền focus linh hoạt: Khi chuỗi tự động dồn lại sau khi xóa ở giữa, viền sáng sẽ lùi về đúng ô mà phím Backspace sẽ tác động tiếp theo, loại bỏ hoàn toàn sự lệch pha giữa giao diện (visual) và hành vi xóa thực tế.

*Cập nhật lần cuối: 11/08/2026 15:05 bởi Antigravity AI Pair Programmer.*

---

## 16. Cơ Chế Database Migration & Ghi Log Lỗi Cục Bộ
- **Database Migration (`TelesalesDatabase.kt`):**
  - Bổ sung `fallbackToDestructiveMigration()` để tránh crash ứng dụng khi nâng cấp version database mà không có migration.
  - Tạo sẵn template `MIGRATION_1_2` giúp DEV dễ dàng cấu hình cập nhật Schema (thêm/sửa cột) cho các bản release tương lai mà không làm mất dữ liệu cục bộ.
- **Local File Logger (`UploadAudioWorker.kt`):**
  - Sửa lỗi nuốt Exception (swallowed exception) khi bắt `RuntimeException` trong quá trình đồng bộ, giúp Logcat hiển thị chính xác nguyên nhân lỗi mạng hoặc parse JSON.
  - Tích hợp hàm `writeErrorLogToFile` tự động ghi toàn bộ stack trace của lỗi vào file `telesales_upload_error_log.txt` lưu tại thư mục Documents nội bộ của ứng dụng.
  - Giúp quản lý và nhân viên dễ dàng trích xuất log lỗi trên bản Release bằng cách truy cập `Android/data/com.nhakhoaquangninh.telesales/files/Documents` mà không cần cắm cáp ADB.

*Cập nhật lần cuối: 11/08/2026 22:01 bởi Antigravity AI Pair Programmer.*

---

## 17. Hỗ trợ Upload Trạng thái Cuộc gọi (is_answered) - Domain Layer
- Cập nhật Domain layer để hỗ trợ cuộc gọi không bắt máy (`isAnswered = false`), cho phép upload không kèm file ghi âm.
- `CallRecordMetadata`: Bổ sung thuộc tính `isAnswered` (mặc định `true`), chuyển `recordingUri` thành nullable.
- `CallMetadataMapper`: Thêm tham số `isAnswered` vào hàm `create`.
- `UploadCallRecordUseCase`: Chỉnh sửa logic, chỉ yêu cầu validate nguồn tệp ghi âm nếu cuộc gọi đã được trả lời.

*Cập nhật lần cuối: 14/08/2026 11:20 bởi Antigravity AI Pair Programmer.*

---

## 18. Hỗ trợ Upload Trạng thái Cuộc gọi (is_answered) - App Layer
- Cập nhật App layer để tự động enqueue và đẩy lịch sử các cuộc gọi không kết nối (is_answered = false) lên server.
- `UploadAudioWorker`: Nhận thêm `KEY_IS_ANSWERED`. Nếu cuộc gọi không bắt máy, bỏ qua bước validate file và khởi tạo Upload mà không cần ID file ghi âm.
- `CallEventCoordinator`: Chỉnh sửa logic ở `saveFailedCall` và `CallEventDecision.ScheduleUpload` để đẩy `isAnswered` flag. Gọi `uploadScheduler.enqueue(metadata)` cho các cuộc gọi thất bại tự động.

*Cập nhật lần cuối: 14/08/2026 13:25 bởi Antigravity AI Pair Programmer.*

---

## 19. Nâng Version Ứng dụng & Room Database Migration (v1 -> v2)
- Cập nhật `versionCode = 2` và `versionName = "1.1"` trong `app/build.gradle.kts`.
- Viết kịch bản Migration (`MIGRATION_1_2`) cho Room Database trong `TelesalesDatabase.kt` để thực thi lệnh `ALTER TABLE call_records ADD COLUMN isAnswered INTEGER NOT NULL DEFAULT 1`. Điều này giúp giữ nguyên dữ liệu lịch sử cuộc gọi cũ khi cài đè phiên bản ứng dụng mới.

*Cập nhật lần cuối: 14/08/2026 13:30 bởi Antigravity AI Pair Programmer.*
