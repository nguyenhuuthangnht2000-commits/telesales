package com.nhakhoaquangninh.telesales.data.remote

import com.nhakhoaquangninh.telesales.data.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val BASE_URL = "https://quanly.thanhtoannoibo.com/api/mobile/"
    val DEFAULT_API_KEY: String
        get() = BuildConfig.TELESALES_API_KEY.trim()

    internal fun createOkHttpClient(
        isDebug: Boolean,
        logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT
    ): OkHttpClient {
        return OkHttpClient.Builder().apply {
            if (isDebug) {
                addInterceptor(
                    HttpLoggingInterceptor(logger).apply {
                        redactHeader("Authorization")
                        redactHeader("X-Api-Key")
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                )
            }
        }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val okHttpClient by lazy {
        createOkHttpClient(isDebug = BuildConfig.DEBUG)
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}