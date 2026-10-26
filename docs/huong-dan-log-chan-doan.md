# Đọc log chẩn đoán upload

Log mới chỉ xuất hiện sau khi cài APK chứa thay đổi này. Log cũ không được bổ sung ngược.

## Liên kết sự kiện

- `session`: phiên chạy tiến trình, thay đổi khi tiến trình khởi động lại.
- `search_id`: thời điểm bắt đầu và chiều cuộc gọi; dùng nối tìm ghi âm với bản ghi được xếp hàng. Đây không phải định danh duy nhất giữa các thiết bị.
- `call_id`: định danh nghiệp vụ, nối hàng đợi và các lần upload.
- `work_id`: định danh WorkManager; `attempt` tính từ 1. Hủy và chạy lại vẫn có thể cùng work_id.
- `attempt_id`: định danh từng lần gọi repository/API. Không được gửi sang backend; không dùng nó để tra backend. Dùng `server_request_id` nếu backend trả X-Request-ID/X-Correlation-ID, hoặc đối chiếu thời điểm với server.

## Các dòng cần đọc

1. `WORK_CONTEXT`: phiên bản app, thiết bị/SDK, backend, chủ bản ghi, tuổi cuộc gọi, số trong hàng đợi và số sau khi áp dụng resolver.
2. `PHONE_SOURCE`: từ thay đổi ngày 26/10/2026, ưu tiên SIM rồi mới dùng số tài khoản (`priority=sim_then_account`). `observed_source` là nguồn quan sát ở lượt đọc chẩn đoán riêng, không phải xác nhận nguồn tại thời điểm tạo bản ghi: sim, account_fallback, unavailable hoặc unknown khi truy vấn lỗi. Ghi quyền/trạng thái đọc SIM và số che theo từng slot/subscription. `account_matches_no_sim=true` nghĩa là số tài khoản không khớp bất kỳ số SIM đọc được. `unknown` không có nghĩa là khớp. `call_subscription=unknown` nghĩa là chưa xác định SIM thực sự tham gia cuộc gọi. Không suy ra SIM đang gọi từ thứ tự danh sách SIM.
3. `RECORDING_CANDIDATES`/`RECORDING_CANDIDATE`: số ứng viên, tối đa 20 ứng viên mỗi lượt tìm, thời gian/thời lượng/kích thước/MIME và lý do loại. `eligible_requires_unique_match` chỉ nói ứng viên hợp lệ đơn lẻ; toàn bộ kết quả có thể vẫn mơ hồ nếu nhiều ứng viên hợp lệ.
4. `UPLOAD_ENQUEUE`: yêu cầu enqueue, không phải xác nhận worker mới được chạy. Chính sách KEEP có thể giữ worker cũ.
5. `UPLOAD_START`/`UPLOAD_RESULT`: cùng attempt_id; dữ liệu metadata đã che số, thông tin file, HTTP, thời gian xử lý, mã request server và Retry-After nếu có. Header không có sẽ là unknown. Không log raw response/token/API key.
6. `WORK_DECISION`/`WORK_RESULT`: quyết định retry/thành công/thất bại và trạng thái cuối lượt worker. `WORKER_CANCELLED` không tự chứng minh OS kill hay mất mạng; nguyên nhân chưa biết được ghi unknown.
7. `QUEUE_SNAPSHOT`: số bản ghi theo trạng thái của chủ bản ghi, tuổi cuộc gọi chưa xử lý xong lâu nhất (ms; -1 nếu không có). Đây là số liệu cache, không phải số worker đang chạy; các truy vấn có thể thay đổi khi upload đồng thời. Các trạng thái chưa xử lý bao gồm PENDING, UPLOADING, RETRYABLE, FAILED và NEEDS_REVIEW.

## Khoanh vùng

- HTTP 500 với recording_attached=true: backend xử lý thất bại; tra server_request_id/thời điểm trong log backend để biết exception thực tế.
- HTTP 422 rejected_fields=recording, is_answered=true, recording_attached=false: metadata cuộc gọi đã nghe được gửi thiếu file, backend từ chối.
- HTTP 429: đọc Retry-After và QUEUE_SNAPSHOT; header chỉ được ghi nhận, thay đổi này không điều chỉnh lịch retry.
- Số sai: so sánh queued/effective, PHONE_SOURCE và SIM đã che. Việc đọc được SIM chưa chứng minh đó là SIM thực sự dùng gọi.
- Ứng viên bằng 0 khác với có ứng viên nhưng bị loại; log lý do giúp chọn bước điều tra tiếp theo.

Không thể khẳng định đã hết tồn chỉ vì lượt cuối trả 201. Kiểm tra QUEUE_SNAPSHOT và từng call_id. Các thay đổi này bổ sung chẩn đoán, chưa sửa thuật toán số điện thoại, ghép file hay retry.

## Xác minh khi được yêu cầu

Chưa chạy build, lint hoặc kiểm thử. Có thêm RecordingDiagnosticTest cho lý do loại ứng viên; chưa chạy. Khi kiểm tra trên máy thật: tạo cuộc gọi thành công, không nghe máy, mất mạng/khôi phục mạng, thiếu ghi âm, và đối chiếu hai SIM/số tài khoản; kiểm tra call_id/attempt_id nối được kết quả và không có token/số đầy đủ trong log mới.
