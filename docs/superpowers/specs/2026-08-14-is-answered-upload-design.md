# Thiết kế Tính năng Upload Trạng thái Cuộc gọi (is_answered) & Ghi âm Tùy chọn

- **Ngày tạo:** 2026-08-14
- **Trạng thái:** Chờ Duyệt (Pending Review)
- **Tác giả:** Antigravity Assistant

---

## 1. 🎯 Mục tiêu
Bổ sung trường `is_answered` (boolean: `true`/`false`) vào API Upload cuộc gọi (`POST /api/mobile/call-records`).
- Khi cuộc gọi **được bắt máy** (`is_answered = true`): Yêu cầu bắt buộc phải có tệp ghi âm `recording`.
- Khi cuộc gọi **không được bắt máy** hoặc **cuộc gọi nhỡ** (`is_answered = false`): Tệp ghi âm `recording` là **tùy chọn (có thể null)**. Hệ thống lập tức đẩy metadata cuộc gọi này lên Server Backend ngay khi cuộc gọi kết thúc.

---

## 2. 📐 Kiến trúc & Chi tiết Thay đổi

### A. Core / Domain Layer (`:domain`)
1. **`CallRecordMetadata`**:
   - Thêm thuộc tính `val isAnswered: Boolean = true`.
   - Thay đổi kiểu dữ liệu `val recordingUri: String?` (có thể null khi `isAnswered = false`).
2. **`CallMetadataMapper`**:
   - Thêm tham số `isAnswered: Boolean = true` vào hàm `create(...)`.
3. **`UploadCallRecordUseCase`**:
   - Nếu `isAnswered == true`: Bắt buộc kiểm tra `recordingUri != null` và có định dạng `content://`.
   - Nếu `isAnswered == false`: Bỏ qua kiểm tra `recordingUri` bắt buộc.

### B. Data & Network Layer (`:data`)
1. **`ApiService` (`POST call-records`)**:
   - Đổi `@Part recording: MultipartBody.Part` $\rightarrow$ `@Part recording: MultipartBody.Part? = null`.
   - Bổ sung `@Part("is_answered") isAnswered: RequestBody? = null`.
2. **`CallRecordRepositoryImpl`**:
   - Truyền `is_answered` dưới dạng `text/plain` với giá trị `"true"` hoặc `"false"`.
   - Nếu `isAnswered == true`: Yêu cầu `resolvePayload` phải thành công và gửi `MultipartBody.Part`.
   - Nếu `isAnswered == false`: Nếu không có file ghi âm, gửi `recording = null`.
3. **Room Database (`CallRecordEntity` & `SyncStatusManager`)**:
   - Thêm `val isAnswered: Boolean = true` vào `CallRecordEntity`.
   - Cập nhật `SyncStatusManager` để lưu/đọc thuộc tính `isAnswered`.

### C. App Layer (`:app`)
1. **`UploadScheduler` & `UploadAudioWorker`**:
   - Thêm `KEY_IS_ANSWERED = "is_answered"` vào `Data` gửi sang Worker.
   - Cho phép `KEY_RECORDING_URI` có thể null nếu `is_answered == false`.
   - Trong `UploadAudioWorker`, nếu `is_answered == false`, bỏ qua bước kiểm tra `RecordingUriValidator` bắt buộc.
2. **`CallEventCoordinator`**:
   - **Cuộc gọi kết nối & bắt máy (`ConnectedEnded`)**: Tạo `CallRecordMetadata` với `isAnswered = true`, enqueue upload qua `UploadScheduler`.
   - **Cuộc gọi nhỡ / Không nhấc máy (`saveFailedCall` / `SaveNotConnected`)**:
     - Lưu sự kiện vào DB cục bộ (`FailedCallEventManager`).
     - Tạo `CallRecordMetadata` với `isAnswered = false`, `recordingUri = null`.
     - Tự động gọi `uploadScheduler.enqueue(metadata)` để đẩy ngay metadata lên Server Backend.

---

## 3. 🧪 Kế hoạch Kiểm thử & Nghiệm thu
1. Build không có lỗi biên dịch trên toàn bộ multi-module (`app`, `domain`, `data`, `core`).
2. Đảm bảo cuộc gọi thành công tạo đúng multipart request chứa `is_answered = "true"` và `recording`.
3. Đảm bảo cuộc gọi nhỡ / không bắt máy gửi đúng multipart request chứa `is_answered = "false"` và `recording = null`.
