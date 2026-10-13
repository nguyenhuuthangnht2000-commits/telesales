# Icon chi nhánh Quảng Ninh

`logo.png` là ảnh gốc đã bỏ toàn bộ chữ, được chỉnh bằng công cụ imagegen tích hợp từ ảnh người dùng cung cấp ngày 13/10/2026.

Yêu cầu chỉnh ảnh: bỏ chữ “NHA KHOA VIỆT MỸ”; giữ biểu tượng răng, vòng cung, ngôi sao và màu cam; căn giữa trên nền trắng vuông, có khoảng đệm; không thêm chữ, bóng hoặc chi tiết mới.

Các tài nguyên Android nằm trong `app/src/quangNinh/res/`, chỉ áp dụng cho `quangNinhDebug` và `quangNinhRelease`. Manifest dùng tên tài nguyên hiện có nên không cần thay đổi Gradle hay Manifest.

- Icon thường và tròn: WebP lossless, 48/72/96/144/192 px theo mật độ mdpi đến xxxhdpi.
- Foreground adaptive icon: 108/162/216/324/432 px, ảnh vuông được thu xuống 68% khung và căn giữa để biểu tượng nằm trong vùng an toàn. Nền trắng khai báo tại `res/values/colors.xml` của flavor.
- Ảnh cửa hàng: `app/src/quangNinh/ic_launcher-playstore.png`, 512 × 512 px. Ảnh này dùng khi đăng cửa hàng, không tự đóng gói như tài nguyên launcher.

Ngày 13/10/2026, đã build thành công cả hai variants release phiên bản 2.1.2 / code 14 theo yêu cầu người dùng; lintVital đã qua. Đã đối chiếu SHA-256 tài nguyên trong APK: Quảng Ninh có đủ 15/15 ảnh launcher riêng, Quốc Tế có đủ 15/15 ảnh launcher cũ. Chưa chạy unit test, integration test hoặc kiểm tra launcher tròn/bo góc trên thiết bị.
