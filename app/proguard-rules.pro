-keepattributes Signature,*Annotation*
-keep interface com.nhakhoaquangninh.telesales.data.remote.ApiService { *; }
-keep class com.nhakhoaquangninh.telesales.data.remote.dto.** { *; }

# Giữ lại các data class / object phục vụ cho Navigation
-keep class com.nhakhoaquangninh.telesales.Login** { *; }
-keep class com.nhakhoaquangninh.telesales.OtpVerify** { *; }
-keep class com.nhakhoaquangninh.telesales.Main** { *; }
-keep class com.nhakhoaquangninh.telesales.NavigationKeysKt** { *; }
-keep,allowobfuscation,allowshrinking @kotlinx.serialization.Serializable class *
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Giữ lại các model trong domain tránh lỗi JSON Parser khi bị rút gọn
-keep class com.nhakhoaquangninh.telesales.domain.model.** { *; }