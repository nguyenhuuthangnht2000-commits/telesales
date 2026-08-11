# Login Support Footer Responsive Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hiển thị footer hỗ trợ của màn hình đăng nhập thành hai dòng căn giữa, không tràn hoặc cắt chữ trên thiết bị nhỏ.

**Architecture:** Chỉ thay đổi layout footer trong `LoginScreen`; dùng `Column` cho hai dòng và `Row` nội bộ cho nội dung liên hệ cùng icon. Giữ nguyên strings, theme, token kích thước và hành vi hiện tại.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3.

## Global Constraints

- Toàn bộ text tiếp tục lấy từ `strings.xml`.
- Không hardcode màu, dp hoặc sp mới.
- Không thêm hành động click hay thay đổi màn hình khác.
- Không tự động chạy test/build nếu người dùng chưa yêu cầu rõ ràng.

---

### Task 1: Chuyển footer hỗ trợ sang bố cục hai dòng

**Files:**
- Modify: `app/src/main/java/com/nhakhoaquangninh/telesales/ui/auth/LoginScreen.kt:309-341`

**Interfaces:**
- Consumes: `R.string.login_support_prompt`, `R.string.login_support_contact`, `Dimens.Space4`, `Dimens.Space16`.
- Produces: footer hai dòng responsive, không tạo API hoặc component public mới.

- [ ] **Step 1: Xác nhận lỗi hiện tại**

Mở màn hình đăng nhập trên thiết bị nhỏ và xác nhận `Row` hiện tại ép hai text cùng icon trên một dòng, làm nội dung tràn hoặc bị cắt.

- [ ] **Step 2: Thay layout bằng Column và Row nội bộ**

Thay `Row` footer hiện tại bằng cấu trúc:

```kotlin
Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(
        text = stringResource(R.string.login_support_prompt),
        style = MaterialTheme.typography.bodyMedium,
        color = OnSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(Dimens.Space4))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.login_support_contact),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = PrimaryTeal,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(Dimens.Space4))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = PrimaryTeal,
            modifier = Modifier.size(Dimens.Space16)
        )
    }
}
```

Thêm import `androidx.compose.foundation.layout.Arrangement`; giữ nguyên các import còn được sử dụng.

- [ ] **Step 3: Kiểm tra tĩnh phạm vi thay đổi**

Chạy:

```powershell
git diff --check -- app/src/main/java/com/nhakhoaquangninh/telesales/ui/auth/LoginScreen.kt
git diff -- app/src/main/java/com/nhakhoaquangninh/telesales/ui/auth/LoginScreen.kt
```

Kỳ vọng: chỉ footer và import `Arrangement` thay đổi; không có whitespace error.

- [ ] **Step 4: Nghiệm thu trên thiết bị khi được yêu cầu**

Build/cài bản release, mở màn hình đăng nhập và xác nhận cả hai dòng căn giữa, không bị cắt trên thiết bị nhỏ. Không chạy lệnh này nếu người dùng chưa yêu cầu kiểm thử.

- [ ] **Step 5: Cập nhật tài liệu trước khi commit**

Ghi tóm tắt fix responsive footer vào `UPDATE_SUMMARY.md`, sau đó chỉ stage các file thuộc thay đổi này; không stage thay đổi ProGuard đang có nếu không nằm trong commit được người dùng yêu cầu.
