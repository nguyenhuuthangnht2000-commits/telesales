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
        isDebug: Boolean
    ): OkHttpClient {
        val customLogger = HttpLoggingInterceptor.Logger { message ->
            android.util.Log.d("API_LOG", message)
        }
        val bodyLogger = HttpLoggingInterceptor(customLogger).apply {
            redactHeader("Authorization")
            redactHeader("X-Api-Key")
            level = HttpLoggingInterceptor.Level.BODY
        }
        val headerLogger = HttpLoggingInterceptor(customLogger).apply {
            redactHeader("Authorization")
            redactHeader("X-Api-Key")
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder().apply {
            if (isDebug) {
                addInterceptor { chain ->
                    val request = chain.request()
                    if (request.url.encodedPath.contains("call-records")) {
                        headerLogger.intercept(chain)
                    } else {
                        bodyLogger.intercept(chain)
                    }
                }
            }
        }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
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