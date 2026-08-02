package com.nhakhoaquangninh.telesales.data.remote

import com.nhakhoaquangninh.telesales.data.remote.dto.RequestOtpRequest
import com.nhakhoaquangninh.telesales.data.remote.dto.RequestOtpResponse
import com.nhakhoaquangninh.telesales.data.remote.dto.VerifyOtpRequest
import com.nhakhoaquangninh.telesales.data.remote.dto.VerifyOtpResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @POST("auth/request-otp")
    suspend fun requestOtp(
        @Header("X-Api-Key") apiKey: String,
        @Body request: RequestOtpRequest
    ): Response<RequestOtpResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(
        @Header("X-Api-Key") apiKey: String,
        @Body request: VerifyOtpRequest
    ): Response<VerifyOtpResponse>

    @Multipart
    @POST("call-records")
    suspend fun uploadCallRecord(
        @Header("X-Api-Key") apiKey: String,
        @Header("Authorization") authorization: String,
        @Part recording: MultipartBody.Part,
        @Part("phone_number_from") phoneNumberFrom: RequestBody? = null,
        @Part("phone_number_to") phoneNumberTo: RequestBody? = null,
        @Part("call_type") callType: RequestBody? = null,
        @Part("duration") duration: RequestBody? = null,
        @Part("call_at") callAt: RequestBody? = null
    ): Response<ResponseBody>
}
