# 📋 Design Spec: Home Dashboard & Bottom Navigation Layout

- **Dự án**: `TelesalesApp` (Nha Khoa Quảng Ninh)
- **Ngày tạo**: 2026-08-05
- **Tệp liên quan**:
  - Design Tokens: [`DESIGN.md`](file:///d:/telesales/DESIGN.md)
  - Mẫu HTML: [`code.html`](file:///C:/Users/thangnh7/Downloads/stitch_dental_call_connect_manager/code.html)
  - Navigation: [`Navigation.kt`](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/Navigation.kt)
  - Main Layout: [`MainScreen.kt`](file:///d:/telesales/app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/MainScreen.kt)

---

## 1. Mục tiêu (Objective)

Thiết kế và triển khai màn hình **Home Dashboard** mới cho nhân viên Telesales sau khi đăng nhập và xác thực OTP thành công. Màn hình giúp nhân viên:
1. Theo dõi và bật/tắt dịch vụ ghi âm cuộc gọi ngầm (`TelesalesForegroundService`).
2. Xem nhanh số liệu thống kê cuộc gọi trong ngày (Tổng số cuộc gọi, Đã đồng bộ, Chờ đồng bộ).
3. Kiểm tra trạng thái cấp quyền hệ thống Android (`RECORD_AUDIO`, `READ_CALL_LOG`, `BATTERY_OPTIMIZATION`).
4. Dễ dàng chuyển đổi giữa các tab **Trang chủ**, **Lịch sử ghi âm** và **Cài đặt** thông qua Bottom Navigation Bar.

---

## 2. Kiến trúc & Cấu trúc Component (Architecture & Layout)

### 2.1. Cấu trúc Điều hướng (Bottom Navigation Bar)
`MainScreen` sẽ đóng vai trò là container chính quản lý `Scaffold`:
- **TopAppBar**: Hiển thị avatar nhân viên, tên thương hiệu "DentalConnect - Nha Khoa Quảng Ninh", trạng thái ca làm việc ("Ca hoạt động"), và nút thông báo.
- **BottomNavigationBar**: 3 Items:
  1. `Trang chủ` (Home Tab): Gọi `HomeScreenContent()`.
  2. `Lịch sử` (History Tab): Gọi `HistoryScreenContent()` (Danh sách ghi âm + Media Player).
  3. `Cài đặt` (Settings Tab): Gọi `SettingsScreenContent()`.

---

## 3. Chi tiết Màn hình Home Dashboard (`HomeScreenContent`)

### 3.1. Thẻ Bật/Tắt Dịch Vụ Ghi Âm (Service Control Card)
- **Màu nền**: `SurfaceContainer` (`#EEEEEE`) với viền mỏng `OutlineVariant`.
- **Nội dung**:
  - Tiêu đề: "Dịch vụ Ghi âm Cuộc gọi Telesales"
  - Badge Trạng thái: **ĐANG HOẠT ĐỘNG (ACTIVE)** (màu xanh Emerald `#10B981` kèm chấm nhấp nháy pulse) hoặc **ĐÃ TẮT (INACTIVE)**.
  - Công tắc (Switch): Cho phép bật/tắt trực tiếp `TelesalesForegroundService`. Khi bật ➔ chạy service ngầm; Khi tắt ➔ dừng service ngầm.

### 3.2. Lưới Thống Kê Nhanh (Quick Metrics Bento Grid)
gồm 3 thẻ Bento Grid:
1. **Tổng cuộc gọi hôm nay**:
   - Icon `phone_in_talk` (Material Symbol Teal `#005C55`).
   - Hiển thị số lượng cuộc gọi GSM phát hiện trong ngày (ví dụ: `42`).
2. **Đã đồng bộ**:
   - Icon `cloud_done` (Turquoise `#0D9488`).
   - Số lượng cuộc gọi + audio đã upload thành công lên Server backend.
3. **Chờ đồng bộ**:
   - Icon `pending_actions` (Warm Amber `#D97706`).
   - Số lượng file ghi âm chờ đồng bộ + Nút **"Đồng bộ ngay"** (Gọi `UploadAudioWorker` thủ công).

### 3.3. Widget Kiểm Tra Quyền Hệ Thống (Permissions Checklist)
Kiểm tra trực tiếp quyền Android runtime:
1. `RECORD_AUDIO`: Biểu tượng tích xanh (Đã cấp) hoặc Cảnh báo đỏ/cam.
2. `READ_CALL_LOG` / `READ_PHONE_STATE`: Biểu tượng tích xanh hoặc Cảnh báo.
3. `BATTERY_OPTIMIZATION`: Hiển thị "Cần xử lý" + Nút **"Khắc phục"** mở trực tiếp cài đặt Tối ưu pin Android (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).

### 3.4. Thẻ Lưu Ý Tuân Thủ (Compliance Note)
- Thẻ thông tin màu `PrimaryContainer` (`#0F766E`) nhắc nhở tuân thủ quy định bảo mật và bảo vệ dữ liệu bệnh nhân của Nha Khoa Quảng Ninh.

---

## 4. Ngôn ngữ & Phong cách (UI Language & Design Tokens)

- **Ngôn ngữ**: 100% Tiếng Việt theo chuẩn Rule 6 của dự án.
- **Color Palette**:
  - Primary Teal: `#005C55`
  - Primary Container: `#0F766E`
  - Active Emerald: `#10B981`
  - Warning Amber: `#D97706`
  - Background Light: `#F9F9F9`
- **Typography**: Inter / Material 3 Typography.

---

## 5. Kế hoạch Kiểm thử & Nghiệm thu (Verification)

1. Kiểm tra build thành công qua `./gradlew assembleDebug`.
2. Kiểm tra thao tác chuyển đổi mượt mà giữa các tab Bottom Navigation Bar.
3. Kiểm tra công tắc Switch bật/tắt Foreground Service thực tế.
