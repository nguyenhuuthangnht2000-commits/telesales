# Build APK theo chi nhánh

Hai bản dùng chung application ID `com.nhakhoaquangninh.telesales`, API key
`TELESALES_API_KEY` và cấu hình ký release hiện tại.

| Chi nhánh | Variant release | Base URL |
| --- | --- | --- |
| Quốc Tế (hiện tại) | `quocTeRelease` | `https://quanly.thanhtoannoibo.com/api/mobile/` |
| Quảng Ninh | `quangNinhRelease` | `https://quanly.nhakhoaquangninh.vn/api/mobile/` |

Flavors được khai báo đồng bộ trong `app/build.gradle.kts` và
`data/build.gradle.kts`. Module `data` sinh `BuildConfig.BASE_URL` theo flavor;
Retrofit dùng giá trị này cho toàn bộ API xác thực và upload.

## Chọn bản trong Android Studio

1. Sau khi thay đổi Gradle, thực hiện Sync Project with Gradle Files.
2. Chọn Build > Generate Signed Bundle / APK > APK và module `app`.
3. Chọn keystore release hiện tại.
4. Chọn `quocTeRelease` hoặc `quangNinhRelease` ở bước chọn build variant.
5. Build APK và lấy file trong thư mục output của chi nhánh đã chọn.

Để chạy debug, chọn `quocTeDebug` hoặc `quangNinhDebug` trong cửa sổ Build Variants.

## Build bằng PowerShell

Chạy từ thư mục gốc dự án, khi đã có API key và cấu hình ký release:

```powershell
# Chi nhánh hiện tại
.\gradlew.bat :app:assembleQuocTeRelease

# Chi nhánh Quảng Ninh
.\gradlew.bat :app:assembleQuangNinhRelease
```

Thư mục output tương ứng:

- `app/build/outputs/apk/quocTe/release/`
- `app/build/outputs/apk/quangNinh/release/`

Tiền tố tên APK là `NK_Telesales`; flavor và build type phân biệt hai bản.
Lệnh `:app:assembleRelease` sẽ build tất cả flavors release; dùng lệnh cụ thể
ở trên khi chỉ cần một chi nhánh. Thiếu `TELESALES_API_KEY` sẽ chặn build release
của cả hai chi nhánh.

## Cài đặt và chuyển chi nhánh

- Hai bản có cùng application ID nên không cài đồng thời. Để cài đè, APK phải
  được ký cùng chứng chỉ và đáp ứng quy tắc versionCode của Android.
- Cập nhật cùng chi nhánh giữ nguyên dữ liệu ứng dụng.
- Khi chuyển sang chi nhánh khác, cài đè vẫn giữ token, lịch sử và hàng đợi
  WorkManager cũ. Ứng dụng chưa tự động tách dữ liệu theo chi nhánh.
- Trước khi chuyển, đồng bộ hết dữ liệu lên server chi nhánh cũ, lưu lại dữ liệu
  cần giữ, rồi xóa dữ liệu ứng dụng trong Cài đặt Android trước khi cài/mở bản mới
  và đăng nhập lại. Xóa dữ liệu sẽ xóa phiên đăng nhập, cache và hàng đợi cục bộ;
  không thực hiện khi còn bản ghi chưa đồng bộ cần giữ.

## Trạng thái xác minh

Chưa chạy Gradle sync, unit test, integration test, lint, build hoặc assemble.
Chưa xác minh trên thiết bị thật hoặc server mới. Khi kiểm thử được yêu cầu,
cần build từng variant release và xác minh OTP/upload đến đúng server.
