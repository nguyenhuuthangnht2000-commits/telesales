# HƯỚNG DẪN KHẮC PHỤC CÁC TRƯỜNG HỢP SERVICE BỊ NGẮT (BACKGROUND SERVICE TROUBLESHOOTING)

Tài liệu này tổng hợp chi tiết các nguyên nhân từ hệ điều hành Android (Samsung, Xiaomi, OPPO, Vivo, Realme...) có thể làm gián đoạn hoặc ngắt **Foreground Service** của ứng dụng **TelesalesApp**, cùng hướng dẫn từng bước để xử lý triệt để.

---

## 🛑 BẢNG TỔNG HỢP NGUYÊN NHÂN & CÁCH XỬ LÝ

| STT | Nguyên nhân gây ngắt Service | Triệu chứng | Cách khắc phục trên thiết bị |
|---|---|---|---|
| **1** | **Tối ưu hóa Pin (Battery Saver / Doze Mode)** | Service chạy được vài tiếng thì bị OS đóng băng để tiết kiệm pin. | Chuyển chế độ pin của App sang **"Không hạn chế" (Unrestricted)**. |
| **2** | **Cấm Tự khởi chạy (Autostart)** *(Xiaomi, OPPO, Vivo, Realme)* | Khởi động lại máy hoặc cúp máy xong không thấy app hoạt động. | Bật quyền **"Tự khởi chạy" (Autostart)** trong cài đặt bảo mật của hãng. |
| **3** | **Bị thu hồi quyền "Vẽ trên ứng dụng khác"** | Cuộc gọi kết thúc không có file ghi âm nhưng màn hình đỏ không hiện ra. | Cấp lại quyền **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)**. |
| **4** | **Ép đóng ứng dụng (Force Stop / Clear RAM)** | Nhân viên cố tình vào Cài đặt bấm "Buộc dừng". | Mở lại ứng dụng TelesalesApp 1 lần — Service sẽ tự động tái sinh. |

---

## 📋 HƯỚNG DẪN CHI TIẾT THEO TỪNG HÃNG ĐIỆN THOẠI

### 1. 📱 Điện thoại SAMSUNG (One UI)
1. Vào **Cài đặt (Settings)** → **Ứng dụng (Apps)** → Tìm **TelesalesApp**.
2. Chọn **Pin (Battery)**.
3. Chuyển từ *Tối ưu hóa (Optimized)* sang **Không hạn chế (Unrestricted)**.
4. Vào **Cài đặt** → **Chăm sóc thiết bị (Device Care)** → **Pin** → **Giới hạn sử dụng ngầm** → Đảm bảo **TelesalesApp** *KHÔNG* nằm trong danh sách "Ứng dụng đặt vào chế độ nghỉ sâu" (Deep sleeping apps).

---

### 2. 📱 Điện thoại XIAOMI / REDMI / POCO (MIUI / HyperOS)
1. Nhấn giữ biểu tượng ứng dụng **TelesalesApp** trên màn hình chính → Chọn **Thông tin ứng dụng (App Info)**.
2. Tìm mục **Tự khởi chạy (Autostart)** → Gạt **BẬT (ON)** → Chọn *Cho phép (Allow)*.
3. Cuộn xuống mục **Tiết kiệm pin (Battery saver)** → Chuyển sang **Không hạn chế (No restrictions)**.
4. Vào mục **Quyền khác (Other permissions)** → Bật **Hiển thị cửa sổ bật lên khi chạy ngầm (Display pop-up windows while running in the background)**.

---

### 3. 📱 Điện thoại OPPO / REALME (ColorOS / Realme UI)
1. Mở ứng dụng hệ thống **Quản lý di động (Phone Manager)**.
2. Chọn **Quyền công cụ (Privacy permissions)** → **Quản lý tự khởi chạy (Autostart manager)**.
3. Gạt **BẬT (ON)** cho ứng dụng **TelesalesApp**.
4. Vào **Cài đặt** → **Ứng dụng** → **TelesalesApp** → **Sử dụng pin** → Bật cả 2 mục:
   - *Cho phép hoạt động ngầm (Allow background activity)*
   - *Cho phép tự khởi chạy (Allow auto-launch)*.

---

### 4. 📱 Điện thoại VIVO (Funtouch OS / OriginOS)
1. Mở ứng dụng hệ thống **iManager**.
2. Chọn **Quản lý ứng dụng (App manager)** → **Quản lý tự khởi động (Autostart manager)**.
3. Cho phép **TelesalesApp** tự khởi chạy.
4. Vào **Cài đặt** → **Pin** → **Quản lý tiêu thụ điện năng ngầm (High background power consumption)** → Gạt **BẬT (ON)** cho TelesalesApp.

---

## 🛠 HƯỚNG DẪN KHÔI PHỤC VÀ KHỞI ĐỘNG LẠI SERVICE

### Cách 1: Khôi phục bằng thao tác trên App (Dành cho nhân viên)
- Khi mở ứng dụng **TelesalesApp**, ứng dụng đã được lập trình sẵn hàm `onResume()` tự động kiểm tra:
  - Nếu Service đang tắt → **Tự động bật lại Service ngầm ngay lập tức**.
  - Nếu chưa có quyền "Vẽ trên ứng dụng khác" → **Tự động chuyển sang màn hình Cài đặt để gạt bật**.

### Cách 2: Cơ chế Tự chữa lành trong Mã nguồn (Self-Healing Code)
Hệ thống mã nguồn của ứng dụng đã bao gồm các cơ chế tự khôi phục sau:
1. **`START_STICKY`**: Khi Android thiếu RAM và diệt Service, ngay khi giải phóng đủ bộ nhớ, hệ thống sẽ tự tái tạo lại `TelesalesForegroundService`.
2. **Manifest Static Receiver**: Dù Service bị ngắt, sự kiện cúp máy (`PHONE_STATE`) vẫn được gửi thẳng tới `CallStateReceiver` thông qua đăng ký trong `AndroidManifest.xml`.
3. **Notification Lock**: Dịch vụ chạy ngầm được khóa bằng thuộc tính `setOngoing(true)`, ngăn chặn việc vuốt tắt thông báo vô ý.
