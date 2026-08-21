# 📌 TÓM TẮT CẬP NHẬT TÍNH NĂNG VÀ KIẾN TRÚC DỰ ÁN (PROJECT UPDATE SUMMARY)

> **Mục đích:** File này lưu trữ lại toàn bộ các tính năng, refactor và cải tiến của dự án Telesales App theo thứ tự tăng dần từ ban đầu đến phiên bản mới nhất. Khi pull code ở máy khác hoặc mở phiên làm việc mới, AI/Dev chỉ cần đọc tệp này là hiểu ngay toàn bộ lộ trình và trạng thái hiện tại mà không cần quét lại toàn bộ codebase.

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

---

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

---

## 8. Checkpoint 2 — Chuẩn hóa type, resource và dependency
- Thay magic string bằng CallType, FailureReason và HistoryFilter; dữ liệu gửi backend/lưu JSON dùng wireValue ổn định.
- Toàn bộ màu UI nằm trong Color.kt/MaterialTheme; toàn bộ dp/sp trong UI dùng token tập trung tại Dimens.kt.
- Chuỗi hiển thị, content description, Toast và notification được chuyển sang strings.xml bằng tiếng Việt.
- Xóa Stringee/JJWT không còn consumer; tập trung Compose, WorkManager, Coroutines, Retrofit và OkHttp vào version catalog.
- BaseViewModel ném lại CancellationException và phát unauthorized bằng tryEmit.
- Metadata đồng bộ JSON bị hỏng được xóa khi đọc, tránh lưu vô thời hạn.
- Metadata legacy chỉ có trạng thái không còn bị suy diễn thành cuộc gọi đi; CallLog duration = 0 chỉ được kết luận không kết nối sau lần retry cuối.
- Verification: 12 focused tests pass; lintDebug và assembleDebug thành công. Lint còn 36 warning đã phân loại cho checkpoint hardening.

---

## 9. Checkpoint 3 — Tái cấu trúc luồng cuộc gọi (Call Flow Architecture)
- `CallStateReceiver` chỉ parse broadcast và forward — không còn file I/O hay network trực tiếp trong receiver.
- Tách thành 6 component riêng biệt: `CallSessionTracker` (state machine), `CallLogDataSource` (truy vấn CallLog), `RecordingLocator` (tìm file qua MediaStore URI), `CallEventCoordinator` (phân loại + schedule), `ComplianceNotifier` (notification), `UploadScheduler` (WorkManager).
- Loại bỏ hoàn toàn `MediaStore.DATA`, `requestLegacyExternalStorage` và quét `/storage/emulated/0`. Dùng scoped storage `content://` URI.
- `RecordingUriValidator` kiểm tra scheme, MIME type, kích thước (≤ 50MB), và readable trước khi upload.
- `UploadAudioWorker` bọc `try/finally` đảm bảo không kẹt trạng thái `UPLOADING`.
- ViewModel không truy cập MediaStore/CallLog/filesystem trực tiếp — ủy quyền cho Repository/DataSource.

---

## 10. Checkpoint 4 — Dọn dẹp deprecated API và release hardening
- Thay `ActivityManager.getRunningServices()` (deprecated từ API 26) bằng `StateFlow<Boolean>` trong `TelesalesForegroundService`; `MainScreen` collect StateFlow tự động cập nhật.
- Fix `vibrator?.vibrate(1000)` deprecated: dùng `VibrationEffect.createOneShot()` cho mọi nhánh SDK ≥ 28 (minSdk).
- Chuyển `window.decorView.systemUiVisibility` deprecated và `windowInsetsController` (API 30+) sang `WindowCompat` + `WindowInsetsControllerCompat` từ AndroidX Core — một đường code duy nhất cho mọi SDK.
- Xóa dead code nhánh `< API 27` trong `WarningActivity` (unreachable vì minSdk = 28).
- Receiver registration dùng `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` chuẩn Android 14+.
- Back handling dùng `onBackPressedDispatcher.addCallback` — không còn `onBackPressed()` deprecated.
*Cập nhật lần cuối: 07/08/2026 23:28 bởi Antigravity AI Pair Programmer.*

---

## 11. Các lỗi phát sinh và cách khắc phục
- **Giao diện không hiển thị tên file:** Ứng dụng đã được sửa lại để luôn hiển thị tên gốc của file ghi âm thay vì tự động chuyển thành số điện thoại trong danh sách lịch sử.
- **Log API:** Cải thiện việc ghi log cho tính năng upload. Thay vì log toàn bộ nội dung file (gây quá tải Logcat), hệ thống hiện chỉ log siêu dữ liệu (metadata) trước khi upload và kết quả trả về (thành công/lỗi) từ Server.
- **Tự động làm mới UI:** Sửa lỗi giao diện không tự động tải lại sau khi upload file thành công. `UploadAudioWorker` và `ProcessCallWorker` hiện đã gửi broadcast `com.nhakhoaquangninh.telesales.REFRESH_RECORDINGS` kèm theo `setPackage(applicationContext.packageName)` để tương thích với quy định bảo mật `RECEIVER_NOT_EXPORTED` của Android 14+.
- **Sửa lỗi Crash màn hình khởi động ở bản Release:** Bổ sung các rule Proguard (R8) cho các class Navigation Compose (`@Serializable` Login, OtpVerify, Main) và các class Model trong Domain để tránh bị obfuscator xóa mất serializer, gây crash `SerializationException` khi khởi chạy ứng dụng. Đồng thời sinh và sử dụng keystore cục bộ để ký APK tự động.

*Cập nhật lần cuối: 10/08/2026 00:25 bởi Antigravity AI Pair Programmer.*

---

## 12. Sửa crash Release và footer đăng nhập responsive
- **Sửa crash WorkManager/Room ở bản Release:** Giữ constructor của các lớp kế thừa `RoomDatabase` để R8 không xóa `WorkDatabase_Impl` được khởi tạo bằng reflection.
- **Sửa footer hỗ trợ màn hình đăng nhập:** Chuyển nội dung hỗ trợ thành hai dòng căn giữa, giữ nền và padding footer để không tràn hoặc sát mép trên thiết bị nhỏ.

---

## 13. Tích hợp xác thực OTP Đăng xuất & Cập nhật Icon App
- **Xác thực OTP trước khi Đăng xuất:** Ràng buộc nhân viên phải nhập mã OTP (được gửi về email quản lý) mỗi khi muốn thoát ca làm việc. Trích xuất thành phần `OtpSixDigitInput` từ màn hình Đăng nhập để tái sử dụng ở màn hình Cài đặt, kết hợp với `SettingsViewModel` để tái sử dụng `RequestOtpUseCase` và `VerifyOtpUseCase`.
- **Chuẩn hoá Strings (Localization):** Loại bỏ toàn bộ text hardcode trên giao diện màn hình Cài đặt và Dialog, chuyển 100% vào `strings.xml`. Đã thêm quy định chống hardcode giao diện vào `AGENTS.md`.
- **Cập nhật Icon App:** Generate lại bộ app icon launcher mọi kích thước (`mipmap-mdpi` -> `xxxhdpi`) từ file ảnh thiết kế mới, vô hiệu hoá `ic_launcher.xml` mặc định (Adaptive Icon) để đảm bảo hình ảnh hiển thị đồng bộ trên mọi nền tảng Android.

*Cập nhật lần cuối: 10/08/2026 23:51 bởi Antigravity AI Pair Programmer.*

---

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

---

## 20. Nâng cấp Bộ Ghi Log Lỗi Chi Tiết (FileLogger Diagnostic Engine)
- **Tập trung hóa FileLogger (`FileLogger.kt`):** Tạo singleton `FileLogger` trong `:core` quản lý việc ghi log chẩn đoán ra tệp `Android/data/com.nhakhoaquangninh.telesales/files/Documents/telesales_upload_error_log.txt`.
- **Ghi nhận đầy đủ vòng đời Upload & Chi tiết lỗi Server (`CallRecordRepositoryImpl.kt` & `UploadAudioWorker.kt`):**
  - Ghi lại toàn bộ metadata trước khi gửi request (`is_answered`, số gọi, số nhận, thời lượng, loại cuộc gọi, file đính kèm).
  - Tự động ghi lại mã phản hồi HTTP (`401`, `422`, `500`) kèm toàn bộ nội dung `errorBody` (JSON chi tiết lỗi từ Server) khi upload bị từ chối.
  - Tự động ghi lại ngoại lệ mạng `IOException` khi mất kết nối mạng.
  - Giúp quản lý và DEV mở trực tiếp file log trên điện thoại để xem chính xác lý do Server từ chối request mà không cần máy tính hay cắm cáp ADB.

*Cập nhật lần cuối: 14/08/2026 15:45 bởi Antigravity AI Pair Programmer.*

---

## 21. Bổ sung Thông báo (Notification) Kết quả Upload Thành Công & Thất Bại
- **Mục tiêu:** Bắn notification trên thanh trạng thái hệ thống ngay khi quá trình đồng bộ/upload file ghi âm hoặc metadata cuộc gọi lên Server thành công hoặc gặp lỗi để người dùng/nhân viên telesales dễ dàng nhận biết.
- **Thành phần cập nhật:**
  - `res/values/strings.xml`: Khai báo tập trung toàn bộ chuỗi văn bản thông báo Tiếng Việt (`notification_upload_success_title`, `notification_upload_success_content`, `notification_upload_unanswered_success_content`, `notification_upload_failed_title`, `notification_upload_failed_content`, `notification_upload_failed_unauthorized`).
  - `ComplianceNotifier.kt`: Bổ sung 2 hàm `notifyUploadSuccess(metadata)` và `notifyUploadFailed(metadata, failureReason)`. Thiết lập `PendingIntent` mở `MainActivity`, tự động sinh Notification ID duy nhất theo từng cuộc gọi để cập nhật thông báo mượt mà.
  - `UploadAudioWorker.kt`: Gọi `complianceNotifier.notifyUploadSuccess(metadata)` khi upload hoàn tất và `complianceNotifier.notifyUploadFailed(metadata, failureReason)` khi validation không hợp lệ, lỗi xác thực 401 hoặc server từ chối request.

*Cập nhật lần cuối: 15/08/2026 10:05 bởi Antigravity AI Pair Programmer.*

---

## 22. Sửa lỗi Type Mismatch CallType & Đổi tên Ứng dụng thành NK_QuocTe
- **Fix Type Mismatch:** Trong [`UploadAudioWorker.kt`](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/UploadAudioWorker.kt), truyền `callType ?: CallType.OUTGOING` khi khởi tạo `CallRecordMetadata` để tương thích hoàn toàn giữa kiểu `CallType?` từ `fromWire()` và `CallType` không null của domain model.
- **Đổi tên ứng dụng:** Cập nhật `app_name` trong [`strings.xml`](file:///d:/telesales/app/src/main/res/values/strings.xml) thành `"NK_QuocTe"`.

*Cập nhật lần cuối: 15/08/2026 10:10 bởi Antigravity AI Pair Programmer.*

---

## 23. Kích hoạt Tự động Mở WarningActivity khi Phát hiện Vi phạm Ghi âm
- **Mục tiêu:** Đảm bảo khi cuộc gọi kết thúc có thời lượng (`duration > 0`) mà thiếu file ghi âm (do nhân viên tắt ghi âm cuộc gọi), ứng dụng sẽ tự động bật thẳng màn hình `WarningActivity` đè lên màn hình hiện tại.
- **Thành phần cập nhật:**
  - `ComplianceNotifier.kt`: Bổ sung lệnh gọi trực tiếp `appContext.startActivity(warningIntent)` với các cờ `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP`, kết hợp cùng cơ chế dự phòng FullScreen Intent Notification để đảm bảo tính răn đe tối đa trên mọi phiên bản Android.

*Cập nhật lần cuối: 15/08/2026 10:40 bởi Antigravity AI Pair Programmer.*

---

## 24. Xác thực OTP khi Tắt Dịch vụ Ghi âm trên HomeScreen
- **Mục tiêu:** Bảo vệ công tắc Toggle trên màn hình Trang chủ (`HomeScreenContent`), yêu cầu quy trình xác thực OTP gửi về Email của Quản lý trước khi cho phép tạm dừng `TelesalesForegroundService`.
- **Thành phần cập nhật:**
  - `res/values/strings.xml`: Khai báo tập trung toàn bộ chuỗi văn bản Tiếng Việt cho luồng xác nhận và OTP tắt dịch vụ (`home_service_stop_confirm_title`, `home_service_stop_confirm_msg`, `home_service_stop_confirm_btn`, `home_service_stop_otp_desc`, `home_service_stop_otp_confirm`, `home_service_stop_cancel`, `home_service_stop_success`, `home_service_start_success`).
  - `MainScreenViewModel.kt`: Bổ sung các StateFlow và hàm `requestStopServiceOtp(userId)`, `verifyStopServiceOtp(userId, onSuccess)`, `onStopServiceOtpChanged(input)`, `resetStopServiceOtpState()`.
  - `MainScreen.kt`: Tích hợp Dialog 1 (Xác nhận tạm dừng dịch vụ) và Dialog 2 (Nhập OTP 6 số bằng `OtpSixDigitInput`). Khi người dùng gạt BẬT lại (OFF -> ON), kích hoạt Service ngay lập tức; khi người dùng gạt TẮT (ON -> OFF), mở popup xác thực OTP 2 bước và chỉ dừng Service khi xác thực thành công.

*Cập nhật lần cuối: 15/08/2026 10:55 bởi Antigravity AI Pair Programmer.*

---

## 25. Dọn dẹp Lint Warnings & Errors trong MainScreen
- **Mục tiêu:** Xử lý triệt để toàn bộ 17 lỗi và cảnh báo lint/IDE trong [`MainScreen.kt`](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/MainScreen.kt).
- **Thành phần cập nhật:**
  - `Querying resource values using LocalContext.current`: Chuyển sang sử dụng `resources.getString(...)` từ `LocalResources.current`.
  - `@SuppressLint("BatteryLife")`: Bổ sung chú thích cho `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` phù hợp với ứng dụng telesales ghi âm ngầm.
  - `Unnecessary SDK_INT >= 28`: Sử dụng trực tiếp `ContextCompat.startForegroundService(context, intent)`.
  - `Unused parameters`: Đổi các tham số không dùng `success -> _`, `e: Exception -> _: Exception`.
  - `Redundant qualifier`: Rút gọn `androidx.compose.material3.CircularProgressIndicator` thành `CircularProgressIndicator`.

*Cập nhật lần cuối: 15/08/2026 11:05 bởi Antigravity AI Pair Programmer.*

---

## 26. Tích hợp Điều hướng Cài đặt Hệ thống & Khắc phục Quyền trên HomeScreen
- **Mục tiêu:** Cung cấp các nút "Khắc phục" và "Cài đặt" trực tiếp trên từng mục quyền/cài đặt hệ thống ở màn hình Trang chủ (`HomeScreenContent`), giúp nhân viên bấm vào là chuyển thẳng tới đúng màn hình Cài đặt tương ứng của Android.
- **Thành phần cập nhật:**
  - `ui/util/SettingsNavUtils.kt`: Bộ tiện ích điều hướng cài đặt Android an toàn (kèm fallback đa tầng):
    - `openAppSettings(context)`: Mở trang Chi tiết ứng dụng để cấp quyền (Cuộc gọi, Nhật ký cuộc gọi, File âm thanh/Bộ nhớ).
    - `openNotificationSettings(context)`: Mở trang Cài đặt thông báo ứng dụng.
    - `openBatteryOptimizationSettings(context)`: Mở trang Tắt tối ưu hóa pin (Unrestricted).
    - `openCallRecordingSettings(context)`: Mở trang Cài đặt ghi âm cuộc gọi trong ứng dụng Điện thoại hệ thống.
    - `openAutostartSettings(context)`: Mở trang Cài đặt Tự khởi chạy/Chạy ngầm chuyên biệt theo từng hãng (Xiaomi/MIUI/HyperOS, Samsung, OPPO, Vivo, Huawei).
  - `HomeScreenContent.kt`: Nâng cấp thẻ "Trạng thái Quyền & Cài đặt":
    - Trạng thái cuộc gọi (`READ_PHONE_STATE`): Hiện nút "Khắc phục" khi chưa cấp.
    - Nhật ký cuộc gọi (`READ_CALL_LOG`): Hiện nút "Khắc phục" khi chưa cấp.
    - File âm thanh / Ghi âm (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`): Hiện nút "Khắc phục" khi chưa cấp.
    - Thông báo ứng dụng (`POST_NOTIFICATIONS`): Hiện nút "Khắc phục" khi chưa cấp.
    - Tối ưu hóa pin: Nút "Khắc phục" chuyển sang trang cấu hình pin không hạn chế.
    - Ghi âm cuộc gọi hệ thống: Nút "Cài đặt" mở cài đặt cuộc gọi để bật tự động ghi âm.
    - Tự khởi chạy & Chạy ngầm: Nút "Cấu hình" mở trang Autostart của hãng.
  - `MainScreen.kt`: Theo dõi trạng thái quyền động trong `Lifecycle.Event.ON_RESUME`, tự động làm mới giao diện ngay lập tức khi nhân viên vừa từ Cài đặt quay lại ứng dụng.

*Cập nhật lần cuối: 15/08/2026 11:10 bởi Antigravity AI Pair Programmer.*

---

## 27. Gỡ bỏ Card Trạng thái Cấp quyền khỏi Màn hình HomeScreen
- **Mục tiêu:** Tối giản giao diện trang chủ (`HomeScreenContent`), loại bỏ hoàn toàn card danh sách quyền hệ thống và các nút khắc phục để tránh nhân viên thao tác nhầm hoặc sử dụng sai mục đích.
- **Thành phần cập nhật:**
  - `HomeScreenContent.kt`:
    - Xóa bỏ card "Trạng thái Quyền & Cài đặt" (System Permissions & Settings Checklist) và hàm `PermissionRow`.
    - Dọn dẹp các tham số trạng thái quyền (`hasPhoneStatePerm`, `hasCallLogPerm`, `hasAudioPerm`, `hasNotificationPerm`, `isBatteryOptimized`) và các callback `onFix...`.
    - Dọn dẹp các import icon/component không còn sử dụng.
    - Giữ lại cấu trúc tinh gọn gồm 4 phần: Thẻ điều khiển Bật/Tắt Dịch vụ, Các chỉ số cuộc gọi nhanh (Metrics Stack), Cuộc gọi gần đây (Recent Calls), và Lưu ý tuân thủ bảo mật (Compliance Note).
  - `MainScreen.kt`:
    - Loại bỏ các biến state theo dõi quyền và hàm `refreshPermissionStates()`.
    - Đơn giản hóa lời gọi Composable `HomeScreenContent`.

*Cập nhật lần cuối: 15/08/2026 14:10 bởi Antigravity AI Pair Programmer.*

---

## 28. Nâng cấp Phiên bản Ứng dụng lên v1.2 (versionCode 3)
- **Mục tiêu:** Nâng phiên bản ứng dụng để chuẩn bị build release và phát hành bản cập nhật mới nhất (gồm tính năng OTP bảo vệ khi tắt dịch vụ, tối ưu giao diện HomeScreen, fix các vấn đề lint/warning).
- **Thành phần cập nhật:**
  - `app/build.gradle.kts`: Cập nhật `versionCode = 3`, `versionName = "1.2"`.

*Cập nhật lần cuối: 15/08/2026 14:15 bởi Antigravity AI Pair Programmer.*

---

## 29. Tích hợp Firebase Crashlytics & Analytics (Giám sát Lỗi Upload & Hệ thống Từ Xa)
- **Mục tiêu:** Cho phép quản trị viên và đội ngũ kỹ thuật theo dõi thời gian thực (real-time) chính xác nguyên nhân các cuộc gọi upload thất bại, sự cố mạng, HTTP status code từ Server, và metadata cuộc gọi từ xa mà không cần can thiệp trực tiếp vào thiết bị của nhân viên.
- **Thành phần cập nhật:**
  - `gradle/libs.versions.toml`: Khai báo plugins `google-services:4.4.2`, `firebase-crashlytics:3.0.3` và thư viện `firebase-bom:33.10.0`, `firebase-analytics`, `firebase-crashlytics`.
  - `build.gradle.kts` (Root) & `app/build.gradle.kts`: Áp dụng Google Services và Crashlytics Gradle plugins, import dependencies qua Firebase BOM.
  - `core/build.gradle.kts`: Bổ sung Firebase BOM và `firebase-crashlytics`.
  - `FileLogger.kt` (`:core`): Nâng cấp thành cơ chế **Ghi Log Kép (Dual Logging)**:
    - Vẫn ghi offline vào tệp văn bản cục bộ `Android/data/com.nhakhoaquangninh.telesales/files/Documents/telesales_upload_error_log.txt`.
    - Tự động đẩy log lên Firebase Crashlytics timeline (`FirebaseCrashlytics.getInstance().log(...)`).
    - Bổ sung `logNonFatalError(...)` để ghi nhận các lỗi quan trọng (HTTP 401, Server error, file missing, metadata invalid) thành Non-Fatal Exceptions kèm Custom Keys ngữ cảnh.
    - Cung cấp `setUserId(userId)` và `setCustomKey(key, value)` để lọc lỗi theo từng nhân viên trên Firebase Console.
  - `TelesalesApplication.kt` & `AuthRepositoryImpl.kt`: Tự động đồng bộ `userId` của nhân viên lên Crashlytics khi app khởi động và sau khi xác thực OTP thành công, đồng thời reset khi logout (`clearSession`).
  - `UploadAudioWorker.kt` & `CallRecordRepositoryImpl.kt`: Gán các Custom Keys chi tiết (`call_phone_from`, `call_phone_to`, `call_type`, `call_duration`, `call_is_answered`, `http_code`) và bắn lỗi Non-Fatal lên Crashlytics khi upload thất bại.

---

## 30. Tự Động Điều Hướng Sang Cài Đặt Ghi Âm Cuộc Gọi Sau Khi Cấp Quyền Thông Báo & Khởi Chạy
- **Mục tiêu:** Tự động hóa trải nghiệm thiết lập ban đầu cho nhân viên: ngay sau khi người dùng cấp quyền thông báo và các quyền runtime hệ thống, ứng dụng sẽ tự động chuyển tiếp người dùng sang màn hình Cài đặt ghi âm cuộc gọi của ứng dụng Điện thoại hệ thống để bật "Tự động ghi âm cuộc gọi".
- **Thành phần cập nhật:**
  - `SettingsNavUtils.kt`: Nâng cấp hàm `openCallRecordingSettings(context)` hỗ trợ danh sách Intent tùy biến sâu theo từng hãng sản xuất (Xiaomi/HyperOS, Samsung, Oppo/Realme, Vivo, Huawei) cùng fallback chuẩn AOSP và Dialer.
  - `MainActivity.kt`: Tích hợp luồng phân quyền và điều hướng 3 bước tuần tự:
    1. Yêu cầu cấp quyền runtime (`READ_PHONE_STATE`, `READ_CALL_LOG`, `POST_NOTIFICATIONS`, `READ_MEDIA_AUDIO`).
    2. Kiểm tra và yêu cầu quyền Overlay (Vẽ trên ứng dụng khác).
    3. Tự động hiển thị Toast hướng dẫn và mở màn hình **Cài đặt ghi âm cuộc gọi** (lưu cờ `KEY_CALL_RECORDING_PROMPTED` vào `SharedPreferences` để không lặp lại phiền toái ở các lần mở app sau).
    4. Khởi chạy `TelesalesForegroundService`.
  - `strings.xml`: Khai báo tập trung chuỗi thông báo hướng dẫn `prompt_enable_call_recording` và `perm_all_granted_starting_service`.

---

## 31. Cập Nhật Luồng Đăng Xuất (Logout) 2 Bước Theo Chuẩn API Mới
- **Mục tiêu:** Đồng bộ quy trình đăng xuất theo đặc tả API mới (`POST /auth/logout/request-otp` và `POST /auth/logout`), xác thực mã OTP gửi về email quản lý trước khi hủy phiên làm việc trên máy chủ và thiết bị.
- **Thành phần cập nhật:**
  - `data/remote/dto/AuthDto.kt`: Bổ sung `LogoutRequest(val otp: String)`.
  - `data/remote/ApiService.kt`: Bổ sung 2 endpoint `requestLogoutOtp` và `logout`.
  - `domain/repository/AuthRepository.kt` & `AuthRepositoryImpl.kt`: Triển khai `requestLogoutOtp(): Resource<String>` và `logout(otp: String): Resource<Boolean>`. Khi logout thành công, tự động xóa phiên cục bộ (`clearSession`) và reset `userId` trong Crashlytics.
  - `domain/usecase/`: Tạo `RequestLogoutOtpUseCase.kt` và `LogoutUseCase.kt`.
  - `ServiceLocator.kt`: Đăng ký `requestLogoutOtpUseCase` và `logoutUseCase`.
  - `SettingsViewModel.kt` & `SettingsScreenContent.kt`: Kết nối UI xác thực OTP đăng xuất với 2 use case mới.
  - `strings.xml` & `AppMessageProvider.kt`: Khai báo tập trung chuỗi phản hồi thông báo đăng xuất.
  - `huong-dan-tich-hop-api.md`: Đồng bộ tài liệu tích hợp API mới nhất (ngày 16/08/2026).

---

## 32. Tích Hợp Nút Xem Trực Tiếp và Chia Sẻ Tệp Nhật Ký Lỗi (Error Log)
- **Mục tiêu:** Cho phép nhân viên và kỹ thuật viên xem trực tiếp toàn bộ log ghi nhận lỗi/hoạt động hoặc chia sẻ tệp log chẩn đoán (`telesales_upload_error_log.txt`) qua các ứng dụng (Zalo, Gmail, Telegram, Google Drive...) ngay trong màn hình Cài đặt của ứng dụng (tối ưu hóa chỉ chia sẻ tệp file `.txt` trực tiếp, không dùng sao chép rườm rà).
- **Thành phần cập nhật:**
  - `core/FileLogger.kt`: Bổ sung 3 hàm tiện ích: `getLogFile(context)`, `readLogContent(context)` và `clearLog(context)`.
  - `app/src/main/res/xml/file_paths.xml` & `AndroidManifest.xml`: Cấu hình `androidx.core.content.FileProvider` cấp quyền chia sẻ tệp log và tệp dữ liệu ra ngoài ứng dụng một cách an toàn.
  - `ui/util/LogShareUtils.kt`: Tạo tiện ích chia sẻ tệp log qua Android Intent (`ACTION_SEND` + `FileProvider URI`).
  - `ui/main/components/SettingsScreenContent.kt`:
    - Thêm mục **"Xem nhật ký lỗi (Log)"**: Mở cửa sổ Dialog xem nội dung log với phông Monospace, hỗ trợ nút "Xóa log" và nút "Chia sẻ file".
    - Thêm mục **"Chia sẻ tệp nhật ký lỗi"**: Mở menu chia sẻ hệ thống để gửi tệp `telesales_upload_error_log.txt` cho bộ phận kỹ thuật.
  - `strings.xml`: Khai báo tập trung toàn bộ chuỗi giao diện cho tính năng Xem và Chia sẻ Log.

---

## 33. Khắc Phục Lỗi Điều Hướng & Kích Hoạt Màn Hình Cảnh Báo Vi Phạm (WarningActivity)
- **Mục tiêu:** Sửa lỗi khiến màn hình `WarningActivity` không tự động mở khi cuộc gọi kết thúc có thời lượng đàm thoại (`duration > 0`) nhưng thiếu file ghi âm do nhân viên tắt tính năng ghi âm cuộc gọi.
- **Nguyên nhân cốt lõi được phát hiện & xử lý:**
  1. Trong `ComplianceNotifier.notifyMissingRecording()`, lệnh `Toast.makeText()` được gọi trực tiếp từ background thread của `ProcessCallWorker` (thiếu UI Main Looper), gây ra crash ngầm `Can't toast on a thread that has not called Looper.prepare()`, làm dừng đột ngột luồng xử lý trước khi lệnh `startActivity(WarningActivity)` được gọi! Đã bọc `Toast.makeText` bằng `Handler(Looper.getMainLooper()).post`.
  2. Bổ sung cơ chế ghi log chẩn đoán (`FileLogger`) chi tiết trong `CallEventCoordinator.kt` và `ComplianceNotifier.kt` để theo dõi rõ: trạng thái cuộc gọi, số điện thoại, thời lượng đàm thoại thực tế, kết quả quét file và trạng thái mở `WarningActivity`.
  3. Củng cố cấu hình `NotificationChannel` với mức ưu tiên `IMPORTANCE_HIGH` và `lockscreenVisibility = VISIBILITY_PUBLIC` kèm `setFullScreenIntent` dự phòng khi `startActivity` bị giới hạn từ background.

---

## 34. Tự Động Xóa Sạch Dữ Liệu Nhập OTP Sau Khi Xác Thực & Đăng Xuất
- **Mục tiêu:** Đảm bảo bảo mật và trải nghiệm người dùng, tự động dọn sạch mã OTP đã nhập ngay sau khi xác thực thành công hoặc khi người dùng đăng xuất / quay lại màn hình Đăng nhập.
- **Thành phần cập nhật:**
  - `ui/auth/OtpVerifyViewModel.kt`:
    - Bổ sung hàm `clearInput()` để reset `_otpInput.value = ""` và `_otpError.value = null`.
    - Tự động gọi `clearInput()` ngay khi UseCase `verifyOtp` trả về `Resource.Success`.
    - Cập nhật hàm `resetState()` để dọn sạch cả trạng thái UI lẫn chuỗi OTP nhập dở.
  - `ui/auth/OtpVerifyScreen.kt`:
    - Thêm `LaunchedEffect(Unit) { viewModel.clearInput() }` đảm bảo mỗi lần màn hình xác thực OTP hiển thị, ô nhập mã luôn ở trạng thái trống mới hoàn toàn.
    - Gọi `viewModel.resetState()` khi nhân viên bấm liên kết "Quay lại Đăng nhập".

---

## 35. Nâng Cấp Phiên Bản v1.3 (versionCode 4) & Đóng Gói Bản Release APK
- **Mục tiêu:** Cập nhật phiên bản chính thức v1.3 tích hợp toàn bộ các tính năng Firebase Crashlytics, API Logout 2 bước, chia sẻ file log, sửa lỗi WarningActivity, dọn sạch mã OTP và xuất bản tệp `app-release.apk`.
- **Thành phần cập nhật:**
  - `app/build.gradle.kts`:
    - Nâng `versionCode = 4`, `versionName = "1.3"`.
    - Bổ sung cấu hình `lint { disable += "InvalidFragmentVersionForActivityResult" }` cho bản build release với ComponentActivity.
  - Đóng gói thành công tệp release: `app/build/outputs/apk/release/app-release.apk` (Dung lượng: ~4.4 MB).

---

## 36. Căn Chỉnh & Tối Ưu Hóa Icon Ứng Dụng Fit Khung Biểu Tượng Chuẩn Android (Adaptive Icons)
- **Mục tiêu:** Khắc phục tình trạng icon app bị phóng to quá mức, mất chữ "NHA KHOA QUỐC TẾ" và bị cắt viền tròn/dải màu khi hiển thị trên màn hình chính của các dòng điện thoại Android (Pixel, Samsung, Xiaomi, Oppo...).
- **Nguyên nhân cốt lõi:**
  - Dự án trước đó thiếu thư mục `mipmap-anydpi-v26/` định nghĩa Adaptive Icons theo chuẩn Android 8.0+.
  - Các tệp ảnh `ic_launcher.webp` và `ic_launcher_round.webp` bị crop quá sát viền (chiếm 100% kích thước canvas), khiến hệ điều hành tự động cắt theo mask (tròn, squircle, bo góc) làm mất chữ và dải biểu tượng.
- **Thành phần cập nhật & xử lý:**
  1. **Bổ sung Adaptive Icon XML (`res/mipmap-anydpi-v26/`):**
     - Khai báo `ic_launcher.xml` và `ic_launcher_round.xml` tách bạch 2 lớp `background` (@drawable/ic_launcher_background nền trắng chuẩn) và `foreground` (@mipmap/ic_launcher_foreground).
  2. **Tái tạo toàn bộ bộ ảnh Foreground & Legacy Icons:**
     - Căn chỉnh tỷ lệ logo nằm chuẩn xác trong vùng Safe Zone (~65% canvas 108dp cho adaptive foreground và ~83% canvas 48dp cho legacy square/round icon).
     - Tạo lại đầy đủ các độ phân giải: `mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi` cho `ic_launcher.webp`, `ic_launcher_round.webp`, và `ic_launcher_foreground.webp`.
     - Cập nhật lại ảnh `ic_launcher-playstore.png` kích thước 512x512 px độ nét cao.
  3. **Làm sạch viền ảnh:** Xóa bỏ hoàn toàn các vệt viền/đường kẻ dư thừa từ ảnh nguồn cũ.

*Cập nhật lần cuối: 17/08/2026 23:25 bởi Antigravity AI Pair Programmer.*

---

## 37. 🛠️ Khắc Phục Lỗi Upload File Trên Bản Release (Release Build Upload Fixes)
- **Bổ sung ProGuard Keep Rules ([app/proguard-rules.pro](file:///d:/telesales/app/proguard-rules.pro)):**
  - Khai báo keep `ListenableWorker`, `InputMerger` (`OverwritingInputMerger`, `ArrayCreatingInputMerger`) và các Worker cụ thể (`UploadAudioWorker`, `ProcessCallWorker`) kèm constructor để WorkManager không bị lỗi `ClassNotFoundException` / `NoSuchMethodException` do R8 obfuscation.
  - Bảo toàn Retrofit annotations (`@POST`, `@Multipart`, `@Part`, `@Header`), Gson `@SerializedName`, `ApiService`, DTOs và `CallRecordRepositoryImpl`.
- **Tăng Timeout OkHttpClient ([RetrofitClient.kt](file:///d:/telesales/data/src/main/java/com/nhakhoaquangninh/telesales/data/remote/RetrofitClient.kt)):**
  - Tăng `connectTimeout` (30s), `readTimeout` (60s), và `writeTimeout` (90s) nhằm tránh lỗi `SocketTimeoutException` khi tải file ghi âm dung lượng lớn qua kết nối di động 3G/4G yếu.
- **Tối Ưu Hoá Xác Thực MIME Type ([RecordingUriValidator.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/call/RecordingUriValidator.kt) & [CallRecordRepositoryImpl.kt](file:///d:/telesales/data/src/main/java/com/nhakhoaquangninh/telesales/data/repository/CallRecordRepositoryImpl.kt)):**
  - Bổ sung fallback xác định MIME type dựa trên extension của tệp (`.m4a`, `.mp3`, `.amr`, `.3gp`, v.v.) trong trường hợp `contentResolver.getType(uri)` trả về `application/octet-stream` hoặc `null` trên một số giao diện tùy biến (Samsung, Xiaomi, Oppo).

---

## 38. 📋 Cập Nhật API Verify OTP, Dropdown Loại Chăm Sóc & Truyền `care_type` vào Upload API
- **Domain Layer ([CareTypeOption.kt](file:///d:/New%20folder/TelesalesApp/domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/CareTypeOption.kt), [UserSession.kt](file:///d:/New%20folder/TelesalesApp/domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/UserSession.kt), [CallRecordMetadata.kt](file:///d:/New%20folder/TelesalesApp/domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/CallRecordMetadata.kt), [CallMetadataMapper.kt](file:///d:/New%20folder/TelesalesApp/domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/CallMetadataMapper.kt)):**
  - Khởi tạo data class `CareTypeOption(val value: Int, val label: String)`.
  - Mở rộng model `UserSession` với trường `careTypeOptions: List<CareTypeOption> = emptyList()`.
  - Bổ sung trường `careType: Int? = null` vào `CallRecordMetadata` và mapper `CallMetadataMapper.create()`.
- **Data Layer DTO & API ([AuthDto.kt](file:///d:/New%20folder/TelesalesApp/data/src/main/java/com/nhakhoaquangninh/telesales/data/remote/dto/AuthDto.kt), [ApiService.kt](file:///d:/New%20folder/TelesalesApp/data/src/main/java/com/nhakhoaquangninh/telesales/data/remote/ApiService.kt), [CallRecordRepositoryImpl.kt](file:///d:/New%20folder/TelesalesApp/data/src/main/java/com/nhakhoaquangninh/telesales/data/repository/CallRecordRepositoryImpl.kt)):**
  - Bổ sung `CareTypeOptionDto` và cập nhật `VerifyOtpData` nhận `careTypeOptions` (hỗ trợ alias `care_type_options`).
  - Mở rộng `uploadCallRecord` endpoint gửi kèm part `"care_type"` multipart form-data.
  - Tự động lấy giá trị `careType` từ metadata hoặc fallback sang giá trị đang chọn trong `TokenManager`.
- **Lưu trữ Cục bộ Bảo mật ([SecureSessionStore.kt](file:///d:/New%20folder/TelesalesApp/data/src/main/java/com/nhakhoaquangninh/telesales/data/local/SecureSessionStore.kt) & [TokenManager.kt](file:///d:/New%20folder/TelesalesApp/data/src/main/java/com/nhakhoaquangninh/telesales/data/local/TokenManager.kt)):**
  - Mã hoá và giải mã trường `careTypeOptions` dạng `JSONArray` trong session được bảo vệ bằng Android KeyStore.
  - Lưu trữ và khôi phục lựa chọn loại chăm sóc hiện tại (`selected_care_type_value`).
- **Giao diện HomeScreen & Worker ([HomeScreenContent.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/components/HomeScreenContent.kt), [MainScreen.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/MainScreen.kt), [MainScreenViewModel.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/MainScreenViewModel.kt), [UploadScheduler.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/call/UploadScheduler.kt), [UploadAudioWorker.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/UploadAudioWorker.kt)):**
  - Mặc định tự động chọn giá trị đầu tiên trong danh sách `careTypeOptions` nếu chưa từng lưu cấu hình trước đó.
  - Thẻ Card hiển thị combobox droplist lựa chọn loại hình chăm sóc ngay dưới khối điều khiển dịch vụ GSM.
  - Truyền giá trị `care_type` xuyên suốt qua `UploadScheduler` -> `UploadAudioWorker` -> `CallRecordRepositoryImpl` khi upload tự động hoặc thủ công.
- **Tài liệu Tích hợp ([huong-dan-tich-hop-api.md](file:///d:/New%20folder/TelesalesApp/huong-dan-tich-hop-api.md)):**
  - Cập nhật mẫu response 200 OK của endpoint `POST /api/mobile/auth/verify-otp`.
  - Bổ sung trường `care_type` vào bảng tham số và cURL mẫu của `POST /api/mobile/call-records`.

---

## 39. ⚡ Tối Ưu Hóa Tìm Kiếm File Ghi Âm & Khắc Phục Timeout Upload Giờ Cao Điểm
- **Khắc phục lỗi MediaStore Indexing Lag ([CallEventCoordinator.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/call/CallEventCoordinator.kt)):**
  - Xây dựng hàm `awaitRecordingMatch()` thực hiện cơ chế Multi-Attempt Retry qua 5 lần dãn cách (0s, +3s, +7s, +15s, +25s) tổng thời gian ~50s thay vì chỉ tìm 1 lần sau 3s rồi báo `RecordingNotFound`.
  - Ghi log chẩn đoán `[RECORDING_LOCATOR]` chi tiết lần thử tìm thấy file thành công.
- **Nới rộng khung thời gian tìm kiếm ([RecordingMatch.kt](file:///d:/New%20folder/TelesalesApp/domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/RecordingMatch.kt) & [RecordingLocator.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/call/RecordingLocator.kt)):**
  - Tăng dung sai tìm kiếm `LATE_TOLERANCE_MILLIS` từ `30s` lên `75s` và `EARLY_TOLERANCE_MILLIS` từ `5s` lên `15s` để bao phủ triệt để thời gian đổ chuông dài (Ringing time) khi khách hàng bắt máy muộn.
  - Tăng phạm vi query MediaStore `QUERY_LATE_MILLIS` lên `90s` và `QUERY_EARLY_MILLIS` lên `20s`.
- **Mở rộng bộ lọc đường dẫn ghi âm ([RecordingMatch.kt](file:///d:/New%20folder/TelesalesApp/domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/RecordingMatch.kt)):**
  - Bổ sung toàn diện danh sách thư mục âm thanh cuộc gọi chuẩn trên Android 11-15 cho Samsung, Xiaomi (MIUI/HyperOS), Oppo (ColorOS), Vivo (FuntouchOS), Realme (`recordings/`, `call_rec/`, `voice recorder/`, `sounds/call/`, `audio/recordings/`...).
- **Tăng tính bền bỉ của Upload Worker ([RetrofitClient.kt](file:///d:/New%20folder/TelesalesApp/data/src/main/java/com/nhakhoaquangninh/telesales/data/remote/RetrofitClient.kt) & [UploadScheduler.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/call/UploadScheduler.kt)):**
  - Tăng `connectTimeout` lên `45s`, `readTimeout` và `writeTimeout` lên `120s`, bật `retryOnConnectionFailure(true)` trong `OkHttpClient`.
  - Cấu hình `BackoffPolicy.EXPONENTIAL` với khoảng chờ khởi tạo `15s` cho `UploadAudioWorker` để tự động khôi phục tác vụ khi mạng bị gián đoạn.

---

## 40. 🛡️ Loại Bỏ Hoàn Toàn Màn Hình WarningActivity & Chuyển Sang Lưu Cache + Tự Động Upload Lại
- **Loại bỏ Màn hình Cảnh báo ([WarningActivity.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/WarningActivity.kt) & [AndroidManifest.xml](file:///d:/New%20folder/TelesalesApp/app/src/main/AndroidManifest.xml)):**
  - Xóa bỏ hoàn toàn tệp `WarningActivity.kt` và thẻ `<activity android:name=".WarningActivity" .../>` trong AndroidManifest.
  - Xóa bỏ các quyền overlay nguy hiểm không còn dùng (`USE_FULL_SCREEN_INTENT`, `SYSTEM_ALERT_WINDOW`).
  - Tuyệt đối không bật đè màn hình cảnh báo hay phát chuông báo động, không làm gián đoạn trải nghiệm của nhân viên telesales khi làm việc.
- **Lưu Cache Cục bộ & Tự Động Upload Lại ([CallEventCoordinator.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/call/CallEventCoordinator.kt) & [ComplianceNotifier.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/call/ComplianceNotifier.kt)):**
  - Khi gặp các trường hợp lỗi (không tìm thấy file ngay, `NeedsReview`, `RecordingNotFound` hoặc lỗi mạng), hệ thống tự động lưu toàn bộ thông tin cuộc gọi vào Room Database (`CallRecordEntity` & `FailedCallEventManager`).
  - Đẩy cuộc gọi vào hàng đợi `UploadAudioWorker` để tự động thử upload lại ngầm khi có mạng.
  - Thông báo nhẹ nhàng trên thanh trạng thái (Notification bar) thay cho màn hình full-screen.

---

## 41. 🛠️ Khắc Phục Lỗi Mở Setting Quyền Đè Lên App Khi Khởi Động & Sửa Lỗi Crash Trên Bản Release
- **Khắc phục lỗi tự động mở Setting "Cấp quyền xuất hiện trên cùng" làm che mất App ([MainActivity.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/MainActivity.kt)):**
  - **Nguyên nhân:** Trong `MainActivity.kt` ở hàm `onResume()`, app gọi `checkNextPermissionsAndNavigate()` tự động kiểm tra `Settings.canDrawOverlays(this)` (quyền Vẽ trên ứng dụng khác / Hiển thị trên cùng / Overlay). Vì ứng dụng đã loại bỏ màn hình cảnh báo `WarningActivity` và không còn khai báo quyền `SYSTEM_ALERT_WINDOW`, hàm này luôn trả về `false` và tự động bắn `Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)` đè lên ứng dụng ngay khi vừa mở app, gây vòng lặp vô tận khiến người dùng không nhìn thấy giao diện app.
  - **Khắc phục:** Loại bỏ hoàn toàn mã kiểm tra quyền overlay và tự động mở dialer settings trong `onResume()`. Ứng dụng chỉ khởi chạy `TelesalesForegroundService` một cách êm ái khi đã đăng nhập và được cấp các quyền runtime cần thiết (`READ_PHONE_STATE`, `READ_CALL_LOG`).
  - Chuẩn hóa toàn bộ chuỗi thông báo sang `strings.xml` (`perm_missing_warning`).
- **Khắc phục lỗi Crash trên bản Release Build do R8 / ProGuard ([proguard-rules.pro](file:///d:/telesales/app/proguard-rules.pro)):**
  - **Nguyên nhân:** Trên bản Release (`isMinifyEnabled = true`), cấu hình cũ có dòng `-keep,allowobfuscation,allowshrinking @kotlinx.serialization.Serializable class *` khiến R8 xóa bỏ/thu gọn các lớp serializer nội bộ được sinh tự động (`Login$$serializer`, `OtpVerify$$serializer`, `Main$$serializer`) và các hàm `Companion.serializer()` của các `NavKey` Navigation 3. Khi `rememberNavBackStack` khởi chạy để lưu/khôi phục trạng thái điều hướng, `kotlinx.serialization` ném `SerializationException` dẫn đến Crash ngay khi khởi động app trên bản release.
  - Ngoài ra, thiếu các quy tắc keep đầy đủ cho Room Database (`TelesalesDatabase_Impl`, DAO, Entity) và các Exception tuỳ biến (`TelesalesNonFatalException`).
  - **Khắc phục:** Bổ sung bộ quy tắc ProGuard / R8 chuẩn, toàn diện cho:
    1. **Kotlinx Serialization & Navigation 3:** Giữ lại các serializer (`KSerializer`, `*$$serializer`), companion methods, `@Serializable`, `androidx.navigation3.**` và các class kế thừa `NavKey`.
    2. **Room Database:** Giữ lại `androidx.room.RoomDatabase`, `@Entity`, `@Dao` và package `com.nhakhoaquangninh.telesales.data.local.room.**`.
    3. **Crashlytics & Exceptions:** Giữ lại `TelesalesNonFatalException`, `LineNumberTable`, `SourceFile` để hỗ trợ tra cứu stacktrace.
- **Khắc phục lỗi Crash `IllegalArgumentException: Only VectorDrawables and rasterized asset types are supported` ([LoginScreen.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/ui/auth/LoginScreen.kt)):**
  - **Nguyên nhân:** Trong `LoginScreen.kt`, hàm `painterResource(id = R.mipmap.ic_launcher_round)` được dùng để hiển thị logo. Trên Android 8.0+ (API 26+), `R.mipmap.ic_launcher_round` được hệ thống phân giải thành XML `<adaptive-icon>` trong `mipmap-anydpi-v26/ic_launcher_round.xml`. Jetpack Compose chỉ hỗ trợ `VectorDrawable` (`<vector>`) và raster assets (`PNG`, `JPG`, `WEBP`), do đó ném ra ngoại lệ `IllegalArgumentException` làm ứng dụng crash ngay khi mở màn hình đăng nhập.
  - **Khắc phục:** Tạo tài nguyên ảnh `res/drawable/ic_app_logo.png` và cập nhật `LoginScreen.kt` sử dụng `painterResource(id = R.drawable.ic_app_logo)`, đảm bảo tương thích 100% với Jetpack Compose trên mọi phiên bản Android.

---

## 42. ⚡ Tự Động Kích Hoạt Service Khi Vào App & Thiết Kế Lại UI Combobox Loại Chăm Sóc
- **Luôn tự động kích hoạt Foreground Service ([TelesalesForegroundService.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/TelesalesForegroundService.kt), [MainActivity.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/MainActivity.kt), [MainScreen.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/MainScreen.kt), [Navigation.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/Navigation.kt)):**
  - **Vấn đề trước đó:** Khi vào app nếu quyền đã được cấp trước đó hoặc khi chuyển tiếp từ màn OTP sang Main, `startService` chưa được gọi tự động ở một số luồng khiến trạng thái hiển thị "ĐÃ TẮT" và switch bị tắt.
  - **Khắc phục:** Bổ sung phương thức `TelesalesForegroundService.startService(context)` và kích hoạt đồng bộ ở tất cả các điểm khởi đầu: `MainActivity.onCreate` (khi quyền đã có), `MainActivity.onResume`, `MainScreen.LaunchedEffect(Unit)`, và `OtpVerifyScreen.onVerifySuccess`. Dịch vụ luôn ở trạng thái **HOẠT ĐỘNG (ACTIVE)** và sẵn sàng ghi nhận cuộc gọi GSM ngay khi nhân viên mở app.
- **Thiết kế lại toàn diện UI Combobox / Dropdown Loại hình chăm sóc ([HomeScreenContent.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/components/HomeScreenContent.kt)):**
  - **Nâng cấp giao diện:** Thay thế `DropdownMenu` dạng popup nổi thô sơ trước đây bằng cơ chế **Accordion Expandable Selector** tích hợp mượt mà ngay trong thẻ Thao tác:
    - **Trigger Card:** Hiển thị nhãn `"LOẠI HÌNH ĐANG CHỌN"`, tên loại chăm sóc nổi bật với font đậm, icon chuyên ngành nha khoa và mũi tên xoay 180° sinh động (`animateFloatAsState`).
    - **Danh sách mở rộng (`AnimatedVisibility`):** Hiển thị danh sách thẻ lựa chọn bo góc tròn (`Space12`), phân định rõ ràng giữa mục đang chọn (viền Teal, nền `PrimaryTeal` 8% mờ, icon `CheckCircle`, gắn nhãn `"Đang áp dụng"`) và các mục khác (`RadioButtonUnchecked`, viền xám tinh tế).
    - Khắc phục hoàn toàn hiện tượng lệch lề, tràn màn hình hoặc bị che khuất của dropdown menu cũ trên các thiết bị di động.
- **Cấu hình định danh Release Build `NK_QuocTe` & Nâng Version ([build.gradle.kts](file:///d:/telesales/app/build.gradle.kts)):**
  - Nâng `versionCode = 6` và `versionName = "1.5"`.
  - Cấu hình `setProperty("archivesBaseName", "NK_QuocTe")` trong `defaultConfig` của `:app`, đảm bảo khi build release APK sẽ tự động sinh file `NK_QuocTe-release.apk` có chữ ký điện tử (`release-key.keystore`).

---

## 43. 🎯 Khắc Phục Triệt Để Luồng Upload `care_type` & Toàn Diện Hệ Thống Logging
- **Khắc phục lỗi thiếu trường `care_type` khi upload lên Server ([AuthRepositoryImpl.kt](file:///d:/telesales/data/src/main/java/com/nhakhoaquangninh/telesales/data/repository/AuthRepositoryImpl.kt), [SecureSessionStore.kt](file:///d:/telesales/data/src/main/java/com/nhakhoaquangninh/telesales/data/local/SecureSessionStore.kt), [CallEventCoordinator.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/call/CallEventCoordinator.kt), [UploadScheduler.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/call/UploadScheduler.kt), [AuthDto.kt](file:///d:/telesales/data/src/main/java/com/nhakhoaquangninh/telesales/data/remote/dto/AuthDto.kt)):**
  - **Nguyên nhân:**
    1. Khi cuộc gọi kết thúc, `CallEventCoordinator` tạo `CallRecordMetadata` không gán `careType` (mặc định `null`), dẫn đến `UploadScheduler` không đưa `KEY_CARE_TYPE` vào Data của `UploadAudioWorker`.
    2. Sau khi xác thực OTP thành công, `tokenManager` chưa lưu giá trị `selected_care_type_value` mặc định; `SecureSessionStore.getSelectedCareTypeValue()` trước đây trả về `null` thay vì fallback sang item đầu tiên trong danh sách `careTypeOptions` của session.
    3. Trong Retrofit `@Multipart`, khi trường `careType` nhận `null`, Retrofit tự động lược bỏ (omit) hoàn toàn `@Part("care_type")` khỏi multipart request body, khiến Server nhận request không có `care_type`.
  - **Khắc phục:**
    1. Bổ sung alternate names phong phú cho `CareTypeOptionDto` (`id`, `care_type`, `care_type_id`, `careTypeId`, `type` cho `value`; `name`, `title`, `text`, `description` cho `label`) và hỗ trợ đọc `careTypeOptions` cả từ `VerifyOtpData` lẫn `UserInfoDto`.
    2. Trong `AuthRepositoryImpl.verifyOtp`, tự động lưu giá trị `selected_care_type_value` ngay khi đăng nhập thành công.
    3. Trong `SecureSessionStore.getSelectedCareTypeValue()`, bổ sung fallback tự động lấy `value` của option đầu tiên trong `careTypeOptions` nếu chưa có lựa chọn thủ công.
    4. Trong `CallEventCoordinator.kt`, gán `careType` (từ `TokenManager`) vào `metadata` cho cả cuộc gọi kết nối thành công (`ScheduleUpload`) lẫn cuộc gọi nhỡ/thất bại (`saveFailedCall`).
    5. Trong `UploadScheduler.kt`, tự động kiểm tra và fallback `careType` từ `TokenManager` trước khi build `inputData` gửi sang `UploadAudioWorker`.
- **Toàn diện hóa hệ thống Ghi Log cho `care_type` ([UploadAudioWorker.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/UploadAudioWorker.kt), [CallRecordRepositoryImpl.kt](file:///d:/telesales/data/src/main/java/com/nhakhoaquangninh/telesales/data/repository/CallRecordRepositoryImpl.kt)):**
  - Bổ sung `FileLogger.setCustomKey("call_care_type", ...)` đẩy lên Firebase Crashlytics trong `UploadAudioWorker`.
  - Đưa `careType` vào Logcat `API_LOG` khi bắt đầu upload file trong Worker và in chi tiết toàn bộ các trường metadata trong `CallRecordRepositoryImpl`.
  - Bổ sung trường `care_type` vào `customKeys` khi ghi nhận lỗi `API_FAILURE` và `WORKER_REJECTED` / `WORKER_UNAUTHORIZED`.

---

## 44. 🛡️ Khắc Phục Lỗi Mất File Ghi Âm (`FileNotFoundException` / `empty_recording`) & Cơ Chế Re-Scan + Fallback Metadata
- **Cơ chế Re-Scan MediaStore thông minh khi file bị rename/move ([UploadAudioWorker.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/UploadAudioWorker.kt), [UploadScheduler.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/call/UploadScheduler.kt), [ServiceLocator.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/ServiceLocator.kt)):**
  - **Nguyên nhân:** Trên một số thiết bị Android (Xiaomi, Samsung, Oppo), ứng dụng ghi âm mặc định của hệ thống lưu file tạm trong lúc đàm thoại và tiến hành rename/flush hoặc dọn dẹp temp cache sau khi cuộc gọi kết thúc. `RecordingLocator` ban đầu bắt được URI tạm, nhưng khi Worker đọc file (sau 30-50s), URI cũ đã bị vô hiệu hóa dẫn đến `FileNotFoundException` và `empty_recording`, khiến Worker hủy bỏ (`Result.failure()`) và làm mất dữ liệu cuộc gọi.
  - **Khắc phục:** 
    1. Khi `RecordingUriValidator.validate` phát hiện file không hợp lệ hoặc unreadable, Worker tự động gọi `ServiceLocator.recordingLocator.findMatch(...)` quét lại MediaStore theo SĐT và thời gian cuộc gọi để lấy URI mới nhất đã được đổi tên.
    2. Bổ sung trường `KEY_STARTED_AT_MILLIS` truyền xuyên suốt qua `UploadScheduler` -> `UploadAudioWorker` để hỗ trợ định vị chính xác khung giờ cuộc gọi.
- **Bảo toàn dữ liệu cuộc gọi tuyệt đối (Zero Data Loss Fallback) ([UploadAudioWorker.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/UploadAudioWorker.kt) & [CallRecordRepositoryImpl.kt](file:///d:/telesales/data/src/main/java/com/nhakhoaquangninh/telesales/data/repository/CallRecordRepositoryImpl.kt)):**
  - Trong trường hợp xấu nhất (người dùng vô tình xóa file âm thanh hoặc file bị hỏng hoàn toàn), ứng dụng tự động kích hoạt **Fallback Metadata Upload**: Tiếp tục gửi thông tin cuộc gọi lên Server CRM với `isAnswered=true`, `duration=X`, `care_type=Y`, `recording=null` thay vì hủy bỏ task.
  - Ghi log chẩn đoán `[RECORDING_LOST_FALLBACK]` và thông báo nhẹ nhàng qua `ComplianceNotifier.notifyMissingRecording()`.
  - Trong `CallRecordRepositoryImpl`, xử lý êm ái khi `recordingUri == null` hoặc rỗng khi `isAnswered = true` (không ném lỗi `FILE_ERROR`), đồng thời phân loại chính xác `FileNotFoundException` thành lỗi client `ErrorSource.APP_CLIENT` thay vì ngộ nhận là lỗi mạng `NETWORK_ERROR`.

---

## 45. 🔐 Bổ Sung Chức Năng Xóa Nhật Ký Lỗi (Log) Yêu Cầu Xác Thực OTP Quản Lý
- **Cơ chế Xác thực OTP khi Xóa Nhật Ký Lỗi ([SettingsViewModel.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/SettingsViewModel.kt), [SettingsScreenContent.kt](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/components/SettingsScreenContent.kt), [strings.xml](file:///d:/telesales/app/src/main/res/values/strings.xml)):**
  - **Mục tiêu:** Tăng cường an toàn dữ liệu và bảo mật hệ thống; nhân viên muốn xóa file log chẩn đoán lỗi cục bộ bắt buộc phải xác thực mã OTP 6 chữ số gửi về email quản lý (đồng nhất với cơ chế bảo mật của luồng tạm dừng dịch vụ).
  - **Khắc phục & Triển khai:**
    1. Trong `SettingsViewModel.kt`, bổ sung `requestClearLogOtp(userId)` (gọi `requestOtpUseCase`) và `verifyClearLogOtp(userId, context)` (gọi `verifyOtpUseCase` -> `FileLogger.clearLog(context)`) được quản lý bằng `launchSafe`.
    2. Trong `SettingsScreenContent.kt`, bổ sung hàng tùy chọn "Xóa nhật ký lỗi" mang biểu tượng thùng rác trong nhóm Cài đặt & Hỗ trợ, đồng thời nâng cấp nút "Xóa log" trong Dialog Xem Log để kích hoạt luồng OTP.
    3. Thiết kế 2 Dialog chuẩn Material 3: Dialog Xác nhận gửi OTP và Dialog nhập mã OTP 6 chữ số (`OtpSixDigitInput`) với đầy đủ loading, validate số và thông báo Toast thành công.
## 46. 🚀 Tối Ưu Hóa & Chuẩn Hóa Dữ Liệu Cuộc Gọi Từ Phân Tích Log Chẩn Đoán (Log Audit & Normalization Fixes)
- **Kiểm toán & Đánh giá Log Hệ thống ([1.md](file:///d:/New%20folder/TelesalesApp/1.md), [2.md](file:///d:/New%20folder/TelesalesApp/2.md), [3.md](file:///d:/New%20folder/TelesalesApp/3.md)):**
  - Đã phân tích 275 cuộc gọi trên 3 thiết bị telesales (`user_id = 370`, `641`, `655`).
  - Tỷ lệ upload API thành công đạt **100% (275/275 cuộc, HTTP 201 Created)**, không phát sinh crash hoặc lỗi mạng 4xx/5xx.
  - Tỷ lệ quét khớp file ghi âm đạt **100% (159/159 cuộc có đàm thoại)** ngay tại lần quét đầu tiên với độ tin cậy `STRONG`.
- **Chuẩn Hóa Số Điện Thoại Nâng Cao ([PhoneNumberNormalizer.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/call/PhoneNumberNormalizer.kt) & [CallMetadataMapper.kt](file:///d:/New%20folder/TelesalesApp/domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/CallMetadataMapper.kt)):**
  - Tự động lọc bỏ các ký tự phân cách rác (khoảng trắng, dấu chấm, dấu gạch nối, dấu ngoặc).
  - Tự động chuyển đổi tiền tố quốc tế `+84` và `84` về đầu số `0` chuẩn của mạng viễn thông Việt Nam, giúp Server Backend CRM dễ dàng đối soát khớp hồ sơ bệnh nhân (`id_bndl`), giảm thiểu tình trạng `id_bndl: null`.
  - Cung cấp các hàm kiểm tra tính hợp lệ `isValid()` và `isLikelyIncomplete()` để phát hiện các số điện thoại bấm thiếu số.
- **Đồng Bộ Log Chẩn Đoán Cuộc Gọi Đến Nhỡ ([CallEventCoordinator.kt](file:///d:/New%20folder/TelesalesApp/app/src/main/java/com/nhakhoaquangninh/telesales/call/CallEventCoordinator.kt)):**
  - Bổ sung lệnh ghi log `[CALL_COORDINATOR]` cho nhánh `missedIncoming`, đảm bảo 100% cuộc gọi (cả đến, đi, đàm thoại và nhỡ) đều có nhật ký chẩn đoán thống nhất trước khi đẩy lên hàng đợi `UploadScheduler`.
  - Chuẩn hóa số điện thoại trong cả luồng cuộc gọi thành công và cuộc gọi nhỡ/thất bại.
- **Nâng Cấp Phiên Bản Ứng Dụng lên v1.7 (versionCode 8) ([build.gradle.kts](file:///d:/New%20folder/TelesalesApp/app/build.gradle.kts)):**
  - Cập nhật `versionCode = 8` và `versionName = "1.7"` đóng gói bản phát hành mới tối ưu hóa xử lý cuộc gọi và chuẩn hóa dữ liệu.

*Cập nhật lần cuối: 21/08/2026 23:15 bởi Antigravity AI Pair Programmer.*

---
