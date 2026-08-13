-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Keep WorkManager Workers & ListenableWorker reflection initialization
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

# Keep Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Gson SerializedName fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Network API Service & DTOs
-keep interface com.nhakhoaquangninh.telesales.data.remote.ApiService { *; }
-keep class com.nhakhoaquangninh.telesales.data.remote.dto.** { *; }

# Keep Upload components & Repositories
-keep class com.nhakhoaquangninh.telesales.call.** { *; }
-keep class com.nhakhoaquangninh.telesales.UploadWorkPolicy** { *; }
-keep class com.nhakhoaquangninh.telesales.data.repository.CallRecordRepositoryImpl** { *; }

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
-keep class * extends androidx.room.RoomDatabase { void <init>(); }