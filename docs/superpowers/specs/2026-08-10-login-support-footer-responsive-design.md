# Thiết kế footer hỗ trợ responsive trên màn hình đăng nhập

## Mục tiêu

Footer hỗ trợ không bị tràn hoặc ép chữ trên thiết bị có chiều rộng nhỏ, đồng thời giữ nguyên typography và nội dung tiếng Việt hiện tại.

## Thiết kế

- Thay hàng ngang duy nhất bằng một cột căn giữa gồm hai dòng.
- Dòng đầu hiển thị `Cần hỗ trợ truy cập?`.
- Dòng hai nhóm `Liên hệ bộ phận hỗ trợ CNTT` và biểu tượng mở liên kết trong cùng một hàng, căn giữa.
- Nội dung dòng liên hệ được phép tự xuống dòng và căn giữa nếu không đủ chiều rộng; không giảm cỡ chữ hoặc hardcode kích thước mới.
- Giữ nguyên màu sắc, typography, spacing token và toàn bộ hành vi hiện tại.

## Kiểm tra

- Xác nhận footer không tràn trên màn hình nhỏ và text không bị cắt.
- Xác nhận bố cục vẫn cân đối trên thiết bị hiện tại.
- Build release và kiểm tra trực tiếp màn hình đăng nhập trên thiết bị kết nối khi người dùng yêu cầu kiểm thử.

## Ngoài phạm vi

- Không thêm hành động click hoặc thông tin liên hệ mới.
- Không thay đổi chuỗi hiển thị, theme hay bố cục phần đăng nhập khác.
