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
