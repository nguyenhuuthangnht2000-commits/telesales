# ─────────────────────────────────────────────────────────────────────────────
# General Attributes & Optimization
# ─────────────────────────────────────────────────────────────────────────────
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keepattributes SourceFile,LineNumberTable

# ─────────────────────────────────────────────────────────────────────────────
# Kotlinx Serialization & Navigation 3
# ─────────────────────────────────────────────────────────────────────────────
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keep class *$$serializer { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
    *** Companion;
}
-keep class androidx.navigation3.** { *; }
-keep class * implements androidx.navigation3.runtime.NavKey { *; }
-keep class com.nhakhoaquangninh.telesales.Login** { *; }
-keep class com.nhakhoaquangninh.telesales.OtpVerify** { *; }
-keep class com.nhakhoaquangninh.telesales.Main** { *; }
-keep class com.nhakhoaquangninh.telesales.NavigationKeysKt** { *; }

# ─────────────────────────────────────────────────────────────────────────────
# Room Database
# ─────────────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.nhakhoaquangninh.telesales.data.local.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ─────────────────────────────────────────────────────────────────────────────
# WorkManager Workers
# ─────────────────────────────────────────────────────────────────────────────
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.InputMerger {
    public <init>();
}
-keep class androidx.work.OverwritingInputMerger { *; }
-keep class androidx.work.ArrayCreatingInputMerger { *; }
-keep class com.nhakhoaquangninh.telesales.UploadAudioWorker { *; }
-keep class com.nhakhoaquangninh.telesales.ProcessCallWorker { *; }

# ─────────────────────────────────────────────────────────────────────────────
# Retrofit, OkHttp & Gson
# ─────────────────────────────────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ─────────────────────────────────────────────────────────────────────────────
# Data, Remote DTOs, Local Stores & Domain Models
# ─────────────────────────────────────────────────────────────────────────────
-keep interface com.nhakhoaquangninh.telesales.data.remote.ApiService { *; }
-keep class com.nhakhoaquangninh.telesales.data.remote.dto.** { *; }
-keep class com.nhakhoaquangninh.telesales.data.local.** { *; }
-keep class com.nhakhoaquangninh.telesales.data.repository.** { *; }
-keep class com.nhakhoaquangninh.telesales.domain.model.** { *; }
-keep class com.nhakhoaquangninh.telesales.call.** { *; }
-keep class com.nhakhoaquangninh.telesales.UploadWorkPolicy** { *; }

# ─────────────────────────────────────────────────────────────────────────────
# Coroutines & Exceptions
# ─────────────────────────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**
-keep public class * extends java.lang.Exception
-keep class com.nhakhoaquangninh.telesales.core.TelesalesNonFatalException { *; }