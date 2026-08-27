# TelesalesApp v1.9 Stabilization

## Summary

- Phát hành `versionName = 1.9`, `versionCode = 10`.
- Sửa toàn bộ lỗi Critical/Important/Minor có ảnh hưởng thực chất; không bulk-upgrade dependency.
- Giữ Clean Architecture hiện tại, bổ sung stable call identity, queue theo user, Room migration và monitoring state bền vững.
- Thực hiện trên branch/worktree `codex/v1.9-stabilization`; không push/merge khi chưa có kết quả nghiệm thu.
- Không thay đổi contract backend; idempotency chỉ ở mức app best-effort.

## Interfaces và dữ liệu

- Mở rộng `CallRecordMetadata` với `callId: String`, `ownerUserId: Int`, `startedAtMillis: Long`; mọi caller phải truyền giá trị thực, không dùng default ngầm.
- Tạo `CallIdentity.create(ownerUserId, startedAtMillis, callType, normalizedPhone)` trả SHA-256 rút gọn, không để PII trong Room key/WorkManager name.
- Mở rộng `CallSessionSnapshot` với owner, own phone, care type và trạng thái answered; persist session khi RINGING/OFFHOOK để phục hồi sau process restart.
- Nâng Room lên version 3:
  - `call_records`: lưu owner, care type, started time, URI hiện hành và stable call ID.
  - `failed_calls`: dùng cùng call ID, owner và sync status thực tế.
  - Viết `MIGRATION_2_3`, không destructive migration; legacy row chỉ backfill cho session đang đăng nhập, còn row không xác định owner bị quarantine.
- Repository upload đọc session một lần, bắt buộc `session.userId == metadata.ownerUserId` trước khi dùng token.
- API multipart không thêm field mới; server vẫn nhận contract hiện tại.

## Implementation

1. **Test foundation và ownership**
   - Bổ sung JUnit4, coroutine-test, Room testing, WorkManager testing và Compose UI test bằng version catalog hiện có.
   - Thêm stable call ID, owner-scoped DAO queries và tags `owner_<id>`.
   - Logout hủy scheduling của owner nhưng giữ row; login đúng user tự enqueue lại `PENDING/RETRYABLE`.
   - Không tiếp tục xóa toàn bộ Room history khi logout; UI chỉ đọc dữ liệu của user hiện tại.

2. **Monitoring và call lifecycle**
   - Persist `monitoringEnabled`; login mới mặc định bật, OTP tạm dừng đặt false trước khi dừng FGS.
   - `MainActivity`, `CallStateReceiver` và scheduler cùng kiểm tra login + monitoring flag.
   - Logout đặt monitoring false, dừng FGS và chặn call ingestion mới.
   - Persist active call session; khôi phục hướng gọi sau process restart và dùng answered state độc lập với duration.
   - CallLog matcher ưu tiên normalized phone + direction + time; chỉ fallback time-only khi không có số.

3. **Recording và upload**
   - Loại các generic marker `recordings/`, `recorder/`, `sounds/`, `music/recordings/`, `voice/` và các thư mục không chứng minh là call recording.
   - Giữ whitelist call-specific theo OEM; match không chắc chắn chỉ upload metadata và gắn `NEEDS_REVIEW`.
   - Chỉ chấp nhận `content://`; loại `file://` khỏi UseCase.
   - Khi re-scan tìm URI mới, cập nhật URI trong cùng Room row/call ID trước khi upload.
   - `ProcessCallWorker` rethrow `CancellationException`; mọi side effect dùng stable call ID để retry không enqueue trùng.
   - Retry IOException, 5xx, 408, 425 và 429 với exponential backoff, tối đa 6 lần; 401 và validation 4xx là terminal.
   - Metadata upload thành công cập nhật failed-call row thành `SYNCED` thay vì tiếp tục hiển thị lỗi.

4. **UI, rule và bảo mật**
   - Resend OTP gọi API thật; reset timer chỉ sau thành công và render loading/error.
   - Dialog tạm dừng, lịch sử và MediaStore có state `Loading/Error/Empty/Content` rõ ràng.
   - Trang chủ tính số liệu theo owner + ngày hiện tại và hiển thị danh sách cuộc gọi gần đây thật; bỏ `+12%` giả.
   - Chuyển mọi literal UI/font/màu còn lại sang `strings.xml`, `Dimens` và semantic color tokens; touch target tối thiểu 48dp.
   - Mask số điện thoại/URI trong log và Crashlytics.
   - Xóa API key/mật khẩu signing khỏi tracked `gradle.properties`; đọc từ user Gradle properties hoặc CI environment. API key và signing credentials hiện tại phải được rotate ngoài repo.

5. **Release**
   - Cập nhật `UPDATE_SUMMARY.md` trước commit.
   - Commit theo nhóm: identity/schema, lifecycle/ownership, recording/upload, UI/security, release verification.
   - Không rewrite Git history, push, merge hoặc gửi production build nếu chưa có secrets mới và nghiệm thu hoàn tất.

## Test và nghiệm thu

- Unit tests:
  - Stable call ID, phone normalization, strict recording paths và ambiguity.
  - Call state bình thường, process restart tại RINGING/OFFHOOK/IDLE, answered dưới 1 giây.
  - Upload policy cho offline, IOException, 401, 408, 425, 429, 4xx và 5xx.
  - Cancellation không bị retry; cùng call ID không tạo work/row trùng.
- Room/WorkManager tests:
  - Migration 2→3 giữ dữ liệu.
  - Care type/owner/URI round-trip.
  - A offline → logout → B login không upload dữ liệu A; A đăng nhập lại thì resume.
  - URI rename chuyển status sang URI mới; metadata-only success cập nhật history.
- UI tests:
  - Resend OTP, loading/error dialog, history error/empty/content.
  - Metrics hôm nay, recent calls, touch target và resource strings.
- Chạy tuần tự, dừng tại lỗi đầu tiên:
  1. Targeted unit tests của task.
  2. `.\gradlew.bat testDebugUnitTest`
  3. `.\gradlew.bat assembleDebug`
  4. `.\gradlew.bat lintDebug`
  5. `.\gradlew.bat connectedDebugAndroidTest`
  6. `.\gradlew.bat assembleRelease` khi secrets/signing mới đã sẵn sàng.
- E2E trên Samsung SM‑M346B1 bằng production test account:
  - Login/OTP/resend, pause qua OTP, restart app và logout.
  - Cuộc gọi đến/đi/nhỡ/không kết nối; có file, thiếu file, rename và ambiguous recording.
  - Offline queue, đổi user, phục hồi đúng owner và kiểm tra CRM không sai tài khoản.
  - Cài v1.9 đè v1.8 để xác minh migration và dữ liệu lịch sử.
- Người dùng trực tiếp nhập OTP; test records phải dùng tài khoản QA và được loại khỏi KPI production.

## Assumptions

- Backend không hỗ trợ `client_call_id`; v1.9 giảm duplicate trong app nhưng không cam kết exactly-once khi server lưu thành công rồi response bị mất.
- Secrets mới do backend/release owner cung cấp qua môi trường, không gửi trong chat hoặc commit.
- Không nâng AGP/Kotlin/Compose/Room/WorkManager chỉ để xóa cảnh báo “newer version”.
- Mọi test failure hoặc build error đều kích hoạt Pause-on-Error: dừng, xuất log và chờ chỉ đạo trước khi sửa tiếp.
