# Tính năng Upload Trạng thái Cuộc gọi (is_answered) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bổ sung trường `is_answered` vào API Upload cuộc gọi, cho phép cuộc gọi không bắt máy (is_answered = false) được upload tự động mà không yêu cầu tệp ghi âm.

**Architecture:** Mở rộng `CallRecordMetadata` và `CallRecordEntity` với field `isAnswered`. Chỉnh sửa API `POST call-records` hỗ trợ `is_answered` part và `recording` null. Ở tầng App, `CallEventCoordinator` tự động enqueue các cuộc gọi không kết nối.

**Tech Stack:** Kotlin, Android, Retrofit, Room, WorkManager.

## Global Constraints

- Không hardcode màu sắc, chuỗi trên UI.
- Mã nguồn viết bằng Tiếng Việt cho các message.
- Sử dụng legacyJniPackaging = true (không tác động build.gradle).
- Không tự động chạy unit test nếu user không yêu cầu (dùng `./gradlew assembleDebug` để verify).

---

### Task 1: Domain & Model Update

**Files:**
- Modify: `domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/CallRecordMetadata.kt`
- Modify: `domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/CallMetadataMapper.kt`
- Modify: `domain/src/main/java/com/nhakhoaquangninh/telesales/domain/usecase/UploadCallRecordUseCase.kt`

**Interfaces:**
- Consumes: Models hiện tại của module `domain`.
- Produces: `CallRecordMetadata` với `isAnswered` boolean và `recordingUri` nullable.

- [ ] **Step 1: Cập nhật CallRecordMetadata**

```kotlin
// domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/CallRecordMetadata.kt
package com.nhakhoaquangninh.telesales.domain.model

data class CallRecordMetadata(
    val recordingUri: String?,
    val phoneNumberFrom: String? = null,
    val phoneNumberTo: String? = null,
    val callType: CallType = CallType.OUTGOING,
    val durationSeconds: Int = 0,
    val callAtFormatted: String? = null,
    val isAnswered: Boolean = true
)
```

- [ ] **Step 2: Cập nhật CallMetadataMapper**

```kotlin
// domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/CallMetadataMapper.kt
package com.nhakhoaquangninh.telesales.domain.model

object CallMetadataMapper {
    fun create(
        recordingUri: String?,
        callType: CallType,
        otherPhoneNumber: String?,
        ownPhoneNumber: String?,
        durationSeconds: Int,
        callAtFormatted: String?,
        isAnswered: Boolean = true
    ): CallRecordMetadata {
        val otherNumber = otherPhoneNumber.normalizePhoneNumber()
        val ownNumber = ownPhoneNumber.normalizePhoneNumber()
        return CallRecordMetadata(
            recordingUri = recordingUri,
            phoneNumberFrom = if (callType == CallType.INCOMING) otherNumber else ownNumber,
            phoneNumberTo = if (callType == CallType.INCOMING) ownNumber else otherNumber,
            callType = callType,
            durationSeconds = durationSeconds.coerceAtLeast(0),
            callAtFormatted = callAtFormatted,
            isAnswered = isAnswered
        )
    }

    private fun String?.normalizePhoneNumber(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
```

- [ ] **Step 3: Cập nhật UploadCallRecordUseCase**

```kotlin
// domain/src/main/java/com/nhakhoaquangninh/telesales/domain/usecase/UploadCallRecordUseCase.kt
package com.nhakhoaquangninh.telesales.domain.usecase

import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.repository.CallRecordRepository

class UploadCallRecordUseCase(private val repository: CallRecordRepository) {
    suspend operator fun invoke(metadata: CallRecordMetadata): Resource<Boolean> {
        if (metadata.isAnswered) {
            val uri = metadata.recordingUri
            if (uri == null || !uri.startsWith("content://")) {
                return Resource.Error(
                    message = "Nguồn tệp ghi âm không hợp lệ",
                    source = ErrorSource.APP_CLIENT
                )
            }
        }
        return repository.uploadCallRecord(metadata)
    }
}
```

- [ ] **Step 4: Kiểm tra build cơ bản**
Run: `./gradlew :domain:assemble`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
```bash
git add domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/CallRecordMetadata.kt domain/src/main/java/com/nhakhoaquangninh/telesales/domain/model/CallMetadataMapper.kt domain/src/main/java/com/nhakhoaquangninh/telesales/domain/usecase/UploadCallRecordUseCase.kt
git commit -m "feat(domain): hỗ trợ is_answered và recording tùy chọn"
```

---

### Task 2: API Service & Data Layer Update

**Files:**
- Modify: `data/src/main/java/com/nhakhoaquangninh/telesales/data/remote/ApiService.kt`
- Modify: `data/src/main/java/com/nhakhoaquangninh/telesales/data/repository/CallRecordRepositoryImpl.kt`

**Interfaces:**
- Consumes: `CallRecordMetadata` từ `domain`.
- Produces: API HTTP request chuẩn theo yêu cầu mới.

- [ ] **Step 1: Cập nhật ApiService.kt**

Cập nhật `uploadCallRecord` hàm:
```kotlin
    @Multipart
    @POST("call-records")
    suspend fun uploadCallRecord(
        @Header("X-Api-Key") apiKey: String,
        @Header("Authorization") authorization: String,
        @Part recording: MultipartBody.Part? = null,
        @Part("phone_number_from") phoneNumberFrom: RequestBody? = null,
        @Part("phone_number_to") phoneNumberTo: RequestBody? = null,
        @Part("call_type") callType: RequestBody? = null,
        @Part("duration") duration: RequestBody? = null,
        @Part("call_at") callAt: RequestBody? = null,
        @Part("is_answered") isAnswered: RequestBody? = null
    ): Response<ResponseBody>
```

- [ ] **Step 2: Cập nhật CallRecordRepositoryImpl.kt**

Sửa `uploadCallRecord`:
```kotlin
        val textMediaType = "text/plain".toMediaTypeOrNull()
        val isAnsweredString = if (metadata.isAnswered) "true" else "false"

        var bodyPart: MultipartBody.Part? = null
        if (metadata.isAnswered) {
            val payload = metadata.recordingUri?.let { resolvePayload(it) }
                ?: return Resource.Error(
                    message = "Không thể đọc tệp ghi âm hợp lệ",
                    source = ErrorSource.APP_CLIENT
                )
            val recordingBody = ContentUriRequestBody(
                resolver = resolver,
                uri = payload.uri,
                mediaType = payload.mimeType.toMediaType(),
                contentLength = payload.sizeBytes
            )
            bodyPart = MultipartBody.Part.createFormData("recording", payload.displayName, recordingBody)
        }

        val response = try {
            apiService.uploadCallRecord(
                apiKey = RetrofitClient.DEFAULT_API_KEY,
                authorization = "Bearer $token",
                recording = bodyPart,
                phoneNumberFrom = metadata.phoneNumberFrom?.toRequestBody(textMediaType),
                phoneNumberTo = metadata.phoneNumberTo?.toRequestBody(textMediaType),
                callType = metadata.callType.wireValue.toRequestBody(textMediaType),
                duration = metadata.durationSeconds.toString().toRequestBody(textMediaType),
                callAt = metadata.callAtFormatted?.toRequestBody(textMediaType),
                isAnswered = isAnsweredString.toRequestBody(textMediaType)
            )
        } catch (_: IOException) {
            return Resource.Error(message = "Không thể kết nối máy chủ", source = ErrorSource.NETWORK)
        }
```

- [ ] **Step 3: Kiểm tra biên dịch Data layer**
Run: `./gradlew :data:assemble`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**
```bash
git add data/src/main/java/com/nhakhoaquangninh/telesales/data/remote/ApiService.kt data/src/main/java/com/nhakhoaquangninh/telesales/data/repository/CallRecordRepositoryImpl.kt
git commit -m "feat(data): gửi field is_answered và optional recording"
```

---

### Task 3: Local Storage / Database Update

**Files:**
- Modify: `data/src/main/java/com/nhakhoaquangninh/telesales/data/local/room/CallRecordEntity.kt`
- Modify: `data/src/main/java/com/nhakhoaquangninh/telesales/data/local/SyncStatusManager.kt`

**Interfaces:**
- Consumes: Cấu trúc Entity.
- Produces: Room Entity chứa thông tin `isAnswered`.

- [ ] **Step 1: Cập nhật CallRecordEntity**

```kotlin
// Trong data/src/main/java/com/nhakhoaquangninh/telesales/data/local/room/CallRecordEntity.kt
package com.nhakhoaquangninh.telesales.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nhakhoaquangninh.telesales.data.local.SyncStatus

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey val id: String,
    val status: String = SyncStatus.PENDING.name,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val recordingUri: String? = null,
    val phoneNumberFrom: String? = null,
    val phoneNumberTo: String? = null,
    val callType: String? = null,
    val durationSeconds: Int = 0,
    val callAtFormatted: String? = null,
    val failureReason: String? = null,
    val isAnswered: Boolean = true
)
```

- [ ] **Step 2: Cập nhật SyncStatusManager**

Trong `setMetadata`:
```kotlin
            callAtFormatted = metadata?.callAtFormatted ?: existing?.callAtFormatted,
            failureReason = existing?.failureReason,
            isAnswered = metadata?.isAnswered ?: existing?.isAnswered ?: true
```
Trong `getMetadata`:
```kotlin
        return CallRecordMetadata(
            recordingUri = recordingUri, // Lưu ý: Nếu DB đang không bắt buộc recordingUri trong trường hợp isAnswered = false, có thể sửa đoạn này
            phoneNumberFrom = entity.phoneNumberFrom,
            phoneNumberTo = entity.phoneNumberTo,
            callType = callType,
            durationSeconds = entity.durationSeconds,
            callAtFormatted = entity.callAtFormatted,
            isAnswered = entity.isAnswered
        )
```
*(Sửa logic `recordingUri` ở `getMetadata` thành nullable:*
```kotlin
        val recordingUri = entity.recordingUri.takeIf { !it.isNullOrBlank() }
        if (entity.isAnswered && recordingUri == null) return null 
```
*)*

- [ ] **Step 3: Kiểm tra biên dịch**
Run: `./gradlew :data:assemble`

- [ ] **Step 4: Commit**
```bash
git add data/src/main/java/com/nhakhoaquangninh/telesales/data/local/room/CallRecordEntity.kt data/src/main/java/com/nhakhoaquangninh/telesales/data/local/SyncStatusManager.kt
git commit -m "feat(db): thêm isAnswered vào CallRecordEntity"
```

---

### Task 4: App Worker & Coordinator Logic Update

**Files:**
- Modify: `app/src/main/java/com/nhakhoaquangninh/telesales/UploadAudioWorker.kt`
- Modify: `app/src/main/java/com/nhakhoaquangninh/telesales/call/UploadScheduler.kt`
- Modify: `app/src/main/java/com/nhakhoaquangninh/telesales/call/CallEventCoordinator.kt`

**Interfaces:**
- Consumes: Mọi thành phần từ `domain` và `data`.

- [ ] **Step 1: Cập nhật UploadScheduler.kt**

```kotlin
// Bổ sung KEY_IS_ANSWERED = "is_answered" vào data:
            .putString(UploadAudioWorker.KEY_CALL_AT, metadata.callAtFormatted)
            .putBoolean(UploadAudioWorker.KEY_IS_ANSWERED, metadata.isAnswered)
            .build()
```

- [ ] **Step 2: Cập nhật UploadAudioWorker.kt**

Khai báo `const val KEY_IS_ANSWERED = "is_answered"`.
Trong `doWork()`:
```kotlin
        val isAnswered = inputData.getBoolean(KEY_IS_ANSWERED, true)
        val recordingUri = inputData.getString(KEY_RECORDING_URI)
        val recordingId = inputData.getString(KEY_RECORDING_ID)
            ?: recordingUri
            ?: (if (!isAnswered) "missed_${System.currentTimeMillis()}" else return Result.failure())
```
Và logic gọi:
```kotlin
        if (isAnswered) {
             // Logic validate cũ
             when (val validation = RecordingUriValidator.validate(applicationContext, recordingUri)) {
                // ... (như cũ)
                is RecordingUriValidation.Valid -> {
                    // setup metadata và upload
                }
             }
        } else {
             // Không bắt máy: Bỏ qua validate file
             val callType = CallType.fromWire(inputData.getString(KEY_CALL_TYPE))
             val duration = inputData.getInt(KEY_DURATION, 0)
             if (callType == null) {
                 failureReason = "invalid_call_metadata"
                 Result.failure()
             } else {
                 ServiceLocator.init(applicationContext)
                 val metadata = CallRecordMetadata(
                     recordingUri = recordingUri, // null là hợp lệ
                     phoneNumberFrom = inputData.getString(KEY_PHONE_FROM),
                     phoneNumberTo = inputData.getString(KEY_PHONE_TO),
                     callType = callType,
                     durationSeconds = duration,
                     callAtFormatted = inputData.getString(KEY_CALL_AT),
                     isAnswered = false
                 )
                 // Gọi Upload tương tự...
             }
        }
```
*(Refactor logic cho DRY trong WorkManager)*

- [ ] **Step 3: Cập nhật CallEventCoordinator.kt**

Trong `saveFailedCall`, thay vì chỉ lưu cục bộ, gọi thêm `uploadScheduler.enqueue(metadata)`:
```kotlin
        val metadata = CallMetadataMapper.create(
            recordingUri = null,
            callType = callType,
            otherPhoneNumber = callLog?.phoneNumber ?: snapshot.otherPhoneNumber,
            ownPhoneNumber = OwnPhoneNumberResolver.resolve(appContext),
            durationSeconds = 0,
            callAtFormatted = formatCallTime(eventTime),
            isAnswered = false
        )
        // ... (phần code lưu failedCallEvents cũ)
        
        // Mới: Gửi tự động lên server
        uploadScheduler.enqueue(metadata)
```
*(Đồng thời đảm bảo truyền `isAnswered = true` trong case `CallEventDecision.ScheduleUpload`)*.

- [ ] **Step 4: Chạy Verify**
Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nhakhoaquangninh/telesales/UploadAudioWorker.kt app/src/main/java/com/nhakhoaquangninh/telesales/call/UploadScheduler.kt app/src/main/java/com/nhakhoaquangninh/telesales/call/CallEventCoordinator.kt
git commit -m "feat(app): đẩy metadata lên server khi cuộc gọi không bắt máy (is_answered = false)"
```
