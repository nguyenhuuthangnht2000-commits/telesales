package com.nhakhoaquangninh.telesales

import android.util.Log
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date
import javax.crypto.SecretKey

/**
 * Tạo JWT token để xác thực với Stringee server.
 *
 * JWT được ký bằng HMAC-SHA256 với API Secret Key của Stringee.
 * Token có thời hạn 1 giờ, sau đó cần tạo lại.
 *
 * ⚠️ NOTE: Trong môi trường production thực tế, JWT nên được tạo trên server
 * để bảo vệ API Secret. Với app nội bộ này, tạo trực tiếp trên device là chấp nhận được.
 */
object StringeeTokenHelper {

    private const val TAG = "StringeeTokenHelper"

    // Stringee API credentials
    private const val API_KEY_SID = "SK.0.bTqUUcpJ4rczUzwI4iInavOcQCpiGuak"
    private const val API_KEY_SECRET = "cXRIam1TSjQzVzh4dHJrZ2V0QWNhbWRuNDVuM0xl"

    /**
     * Tạo JWT token cho một userId cụ thể.
     * @param userId ID duy nhất của người dùng (ví dụ: "agent_001", "nhanvien_01")
     * @return JWT token string hoặc null nếu có lỗi
     */
    fun generateToken(userId: String): String? {
        return try {
            // Stringee yêu cầu sử dụng trực tiếp API Secret làm chuỗi byte (không decode Base64)
            val key: SecretKey = Keys.hmacShaKeyFor(API_KEY_SECRET.toByteArray(Charsets.UTF_8))

            val now = Date()
            val exp = Date(now.time + 3600 * 1000L) // hết hạn sau 1 giờ
            val jti = "$API_KEY_SID-${now.time}"

            val token = Jwts.builder()
                .header()
                .add("cty", "stringee-api;v=1")
                .and()
                .claim("jti", jti)
                .issuer(API_KEY_SID)
                .subject(userId)
                .issuedAt(now)
                .expiration(exp)
                .claim("userId", userId)
                .claim(
                    "icc_api",
                    true
                ) // Bắt buộc cho tính năng gọi ra số điện thoại (App-to-Phone)

                .signWith(key)
                .compact()

            Log.d(TAG, "✅ JWT token tạo thành công cho userId=$userId")
            token
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi tạo JWT token: ${e.message}", e)
            null
        }
    }
}
