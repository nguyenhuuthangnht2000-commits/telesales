import re

file_path = r'd:\telesales\app\src\main\java\com\nhakhoaquangninh\telesales\ui\main\components\HomeScreenContent.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports
imports = '''import androidx.compose.ui.res.stringResource
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.theme.Dimens
'''
content = content.replace('import androidx.compose.ui.unit.dp\n', 'import androidx.compose.ui.unit.dp\n' + imports)

# Replace Dimens
content = re.sub(r'16\.dp', 'Dimens.PaddingMedium', content)
content = re.sub(r'8\.dp', 'Dimens.PaddingSmall', content)
content = re.sub(r'4\.dp', 'Dimens.PaddingExtraSmall', content)
content = re.sub(r'12\.dp', 'Dimens.CornerRadiusMedium', content)
content = re.sub(r'24\.dp', 'Dimens.PaddingLarge', content)
content = re.sub(r'20\.dp', 'Dimens.IconSizeMedium', content)
content = re.sub(r'18\.dp', 'Dimens.IconSizeSmall', content) # roughly
content = re.sub(r'10\.dp', '(10.dp)', content) # maybe leave manual
content = re.sub(r'1\.dp', 'Dimens.BorderThickness', content)
content = re.sub(r'2\.dp', 'Dimens.ElevationSmall', content)
content = re.sub(r'6\.dp', '6.dp', content)

# Replace Strings
replacements = {
    '\"Dịch vụ Ghi âm Cuộc gọi\"': 'stringResource(R.string.service_title)',
    '\"HOẠT ĐỘNG\"': 'stringResource(R.string.service_active)',
    '\"ĐÃ TẮT\"': 'stringResource(R.string.service_inactive)',
    '\"Dịch vụ liên tục theo dõi và tự động ghi âm các cuộc gọi GSM để đảm bảo tuân thủ quy trình.\"': 'stringResource(R.string.service_desc)',
    '\"Thống kê hôm nay\"': 'stringResource(R.string.today_stats)',
    '\"Tổng cuộc gọi\"': 'stringResource(R.string.total_calls)',
    '\"Đã đồng bộ\"': 'stringResource(R.string.synced_calls)',
    '\"Chờ tải lên\"': 'stringResource(R.string.pending_calls)',
    '\"Đồng bộ\"': 'stringResource(R.string.sync_now)',
    '\"Trạng thái Quyền Hệ thống\"': 'stringResource(R.string.sys_permissions_status)',
    '\"Ghi âm cuộc gọi (RECORD_AUDIO)\"': 'stringResource(R.string.perm_record_audio)',
    '\"Nhật ký cuộc gọi (READ_CALL_LOG)\"': 'stringResource(R.string.perm_read_call_log)',
    '\"Tối ưu hóa Pin\"': 'stringResource(R.string.battery_opt)',
    '\"Đã bỏ qua (Tốt)\"': 'stringResource(R.string.battery_opt_ignored)',
    '\"Cần tắt tối ưu pin để không bị dừng ngầm\"': 'stringResource(R.string.battery_opt_needed)',
    '\"Khắc phục\"': 'stringResource(R.string.fix_issue)',
    '\"Quy định Bảo mật & Tuân thủ\"': 'stringResource(R.string.compliance_note_title)',
    '\"Vui lòng tuân thủ quy trình giao tiếp với khách hàng của Nha Khoa Quảng Ninh. File ghi âm được mã hóa bảo mật khi đồng bộ lên Server.\"': 'stringResource(R.string.compliance_note_desc)',
    '\"Đã cấp\"': 'stringResource(R.string.perm_granted)',
    '\"Chưa cấp\"': 'stringResource(R.string.perm_denied)'
}

for k, v in replacements.items():
    content = content.replace(k, v)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Updated HomeScreenContent.kt')
