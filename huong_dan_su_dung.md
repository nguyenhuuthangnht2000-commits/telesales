# Hướng Dẫn Sử Dụng Ứng Dụng Telesales App

Tài liệu này cung cấp toàn bộ thông tin về cách thức hoạt động, hướng dẫn sử dụng và các thiết lập bắt buộc để ứng dụng Telesales có thể hoạt động ngầm 24/7 mà không bị gián đoạn.

---

## 1. Cách Thức Hoạt Động (Workflow)

Ứng dụng được thiết kế hoàn toàn tự động để nhân viên không cần thao tác thủ công trong quá trình làm việc:
- **Tự động Ghi âm:** Nhận diện khi nhân viên bắt đầu cuộc gọi (hoặc nhấc máy cuộc gọi đến) để kích hoạt thu âm qua Microphone.
- **Tự động Lưu trữ:** Kết thúc cuộc gọi, hệ thống lập tức chốt file âm thanh (định dạng `.m4a`).
- **Tự động Đồng bộ (Upload):** Chuyển file ngầm lên Máy chủ (Server) của công ty. Nếu mất mạng, hệ thống tự lưu lại và sẽ tải lên ngay khi có WiFi/4G trở lại. File trên máy sẽ bị tự động xóa khi upload thành công để tiết kiệm dung lượng.

---

## 2. Hướng Dẫn Sử Dụng (Dành Cho Nhân Viên)

1. Mở ứng dụng **Telesales App** lần đầu tiên.
2. Màn hình sẽ bật lên các yêu cầu cấp quyền. Bắt buộc nhấn **Cho Phép (Allow)** đối với toàn bộ các quyền:
   - Truy cập Nhật ký cuộc gọi.
   - Truy cập Trạng thái điện thoại.
   - Cho phép Ghi âm (Microphone).
   - Cho phép Gửi Thông báo (Notification).
3. Nếu cấp quyền thành công, thanh trạng thái (phía trên cùng màn hình) sẽ hiển thị một thông báo không thể xóa: **"Telesales App Đang Hoạt Động"**.
4. Ẩn ứng dụng ra màn hình chính và bắt đầu thực hiện các cuộc gọi tư vấn như bình thường.

---

## 3. Cảnh Báo Quan Trọng (Tại sao App lại ngừng thu âm?)

Mặc dù ứng dụng có hệ thống chống tắt ngầm (Foreground Service), điện thoại của bạn vẫn có thể "giết" ứng dụng trong các trường hợp sau:

> [!WARNING]
> **Tắt ứng dụng từ Đa nhiệm (Recent Apps)**
> - Máy chuẩn Android (Pixel, Samsung cao cấp): Vuốt tắt app thì ứng dụng **vẫn chạy ngầm bình thường**.
> - Máy Trung Quốc (Xiaomi, Oppo, Vivo, Redmi...): Vuốt tắt app đồng nghĩa với việc **ÉP DỪNG (Force Stop)**. Ứng dụng sẽ bị tắt hoàn toàn và không thể thu âm được nữa.

> [!CAUTION]
> **Trình Tối ưu Pin của Hãng (Battery Optimizer)**
> Các dòng máy Oppo, Xiaomi có xu hướng quét và tắt các ứng dụng chạy nền tiêu thụ pin sau khi màn hình tắt khoảng 15-30 phút. 

> [!NOTE]
> **Hết dung lượng RAM (Out of Memory)**
> Khi nhân viên mở quá nhiều ứng dụng nặng (ví dụ: chơi game) khiến RAM đầy, Android sẽ buộc phải giết ứng dụng. Tuy nhiên, hệ thống thường tự động khởi động lại app khi có RAM trống.

---

## 4. Bắt Buộc Cấu Hình (Để tránh bị lỗi mất file)

Để đảm bảo tỷ lệ ghi âm thành công 100%, bạn **bắt buộc phải yêu cầu nhân viên cấu hình** trên điện thoại của họ một lần duy nhất:

### A. Khóa Đa Nhiệm (Vô cùng quan trọng với máy Tàu)
1. Mở app Telesales.
2. Mở trình **Đa nhiệm (Recent Apps)** (vuốt từ dưới màn hình lên và giữ).
3. **Nhấn giữ** vào thẻ của ứng dụng Telesales.
4. Chọn biểu tượng **Ổ Khóa (Lock)**. App sẽ bị khóa lại và không thể bị vuốt để tắt nhầm nữa.

### B. Tắt Tối Ưu Pin & Bật Tự Khởi Chạy
1. Vào **Cài đặt (Settings)** > **Ứng dụng (Apps)** > Tìm đến **Telesales App**.
2. Tìm mục **Pin (Battery / Battery Saver)** ➔ Chọn chế độ **Không Hạn Chế (Unrestricted / No Restrictions)**.
3. Tìm mục **Tự Khởi Chạy (Auto-start)** ➔ Bật **On**. (Giúp app tự động kích hoạt sau khi khởi động lại máy).

> [!TIP]
> **Lời khuyên cho bộ phận kỹ thuật:**
> Nếu doanh nghiệp cung cấp máy công ty cho nhân viên, hãy cân nhắc cài đặt app này thông qua các phần mềm quản lý thiết bị MDM (Mobile Device Management) để tự động ép các quyền hệ thống mà không phụ thuộc vào thao tác của nhân viên.


## 5. Thiết lập vị trí cuộc gọi

Lần mở đầu (hoặc lần mở đầu sau khi cập nhật tính năng này), ứng dụng tự hiện hướng dẫn vị trí sau khi hoàn tất các yêu cầu quyền hiện có, kể cả khi chưa đăng nhập. Hướng dẫn chỉ tự hiện một lần. Nếu đã bỏ qua hoặc từ chối, trên màn hình chính chọn **Thiết lập vị trí** để mở lại.
Đọc phần giải thích và chọn **Tiếp tục** để cấp quyền vị trí. Có thể chọn vị trí
chính xác hoặc gần đúng. Tiếp theo, cấp **Luôn cho phép** để hỗ trợ khi màn hình
tắt hoặc ứng dụng chạy ngầm. Với Android 11 trở lên, mở **Quyền → Vị trí → Luôn
cho phép** trong cài đặt ứng dụng. Android 9 không có bước quyền nền riêng.
Nếu dịch vụ vị trí đang tắt, chọn thiết lập để mở cài đặt và bật lại.

Ứng dụng chỉ lấy vị trí khi kết thúc cuộc gọi và gửi cùng dữ liệu cuộc gọi đến
máy chủ để quản lý hoạt động nhân viên. Không theo dõi vị trí liên tục. Có thể
chọn **Để sau**; thiếu tọa độ vẫn đồng bộ cuộc gọi. Vị trí không được đảm bảo
cho mọi cuộc gọi nếu hệ điều hành hạn chế truy cập nền hoặc tín hiệu yếu.

### Danh sách kiểm tra dành cho người kiểm thử (chưa thực hiện)

- Trên Android 9, 10 và 11 trở lên: cấp vị trí khi dùng trước quyền nền, từ chối hoặc thu hồi quyền không làm gián đoạn upload. Kiểm tra cả vị trí gần đúng trên Android 12 trở lên.
- Kết thúc cuộc gọi khi app hiển thị, chạy ngầm và màn hình khóa: có thể nhận tọa độ nếu quyền và hệ điều hành cho phép. Kiểm tra cuộc gọi đến, đi, nhỡ và không kết nối.
- Tắt dịch vụ vị trí hoặc không có tín hiệu: kết thúc chờ tối đa 5 giây, tiếp tục xử lý cuộc gọi, không gửi tọa độ giả. Vị trí lưu sẵn quá 60 giây không được dùng.
- Kết thúc cuộc gọi A ở một nơi, mất mạng, di chuyển rồi kết nối lại: tọa độ gửi cho A phải giữ nguyên. Thử hai cuộc gọi liên tiếp để kiểm tra không lẫn tọa độ.
- Thử gửi lại thủ công, khởi động lại app và đăng nhập lại: cặp tọa độ đã lưu phải được giữ nguyên. Cuộc gọi cũ không có tọa độ phải bỏ hai phần multipart.
- Nâng cấp từ database phiên bản 3: giữ nguyên call_records và failed_calls; hai cột mới mặc định NULL. Kiểm tra cả dữ liệu đi qua migration từ phiên bản 1/2.
- Trên môi trường tích hợp backend: xác nhận multipart chứa đúng latitude/longitude với dấu chấm, lưu được cả cặp, chấp nhận thiếu cả hai và vẫn nhận file ghi âm/metadata như trước.

- Kiểm tra bổ sung sau rà soát: thu hồi quyền/tắt vị trí/tạm dừng giám sát hoặc đổi phiên trong lúc chờ định vị; tọa độ phải bị bỏ khi không còn phù hợp nhưng sự kiện cuộc gọi vẫn được xử lý. Khi server trả lại tọa độ trong body hoặc thông báo lỗi, log upload không được chứa dữ liệu này.


### Đọc log vị trí trong Logcat

Lọc theo tag `API_LOG` và tìm các dòng sau:

- `location_capture session_id=… outcome=cached/current location_available=true`: lấy được vị trí từ cache còn mới hoặc cảm biến.
- `outcome=permission_missing/location_disabled/no_provider/timeout/no_valid_fix/provider_error`: nguyên nhân không lấy được vị trí. `session_changed` hoặc `session_check_failed` nghĩa là tọa độ bị bỏ tại bước kiểm tra phiên.
- `location_link session_id=… call_id=…`: nối mã phiên thu thập với mã cuộc gọi lưu/upload.
- `location_upload call_id=… latitude_present=true longitude_present=true location_attached=true reason=attached`: cả hai phần multipart chuẩn bị được gửi. Các giá trị false và reason tương ứng chỉ ra tọa độ thiếu hoặc không hợp lệ.

Retry dùng lại `call_id` và tọa độ đã lưu. Bản ghi cũ không có vị trí sẽ có
`reason=missing_coordinates` và không có log thu thập tương ứng. Các log này không
chứa tọa độ cụ thể và không chứng minh server đã lưu dữ liệu; cần đối chiếu request
phía server để xác nhận.


**Cập nhật theo yêu cầu ngày 10/09/2026:** Dòng Logcat `API_LOG` →
`location_upload` nay hiển thị cả `latitude=<giá trị>` và `longitude=<giá trị>`
trên Debug lẫn Release. Nếu không gửi tọa độ, cả hai giá trị là `null`.
Cập nhật này thay thế mô tả ẩn giá trị tọa độ trong Logcat ở trên; các cờ
`latitude_present`, `longitude_present`, `location_attached` và `reason` vẫn giữ nguyên.


**Cập nhật ghi file log (10/09/2026):** Các dòng `location_capture`,
`location_link`, `location_upload` có mặt trong cả Logcat và file
`telesales_upload_error_log.txt` hiện có. Mở hoặc chia sẻ file log trong app để
xem trạng thái thu thập, mã đối chiếu cuộc gọi và `latitude`/`longitude` thực tế
của payload trên cả Debug/Release. Không có tọa độ hợp lệ thì giá trị là `null`.
Các dòng này được ghi nền, không gửi sang Crashlytics; không tự bổ sung dữ liệu
cho các log cũ. Cần cài bản build mới và phát sinh cuộc gọi mới để thấy thay đổi.
