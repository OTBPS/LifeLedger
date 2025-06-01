package com.example.life_ledger.data.network

import com.example.life_ledger.constants.AppConstants
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络客户端
 */
object NetworkClient {
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (android.util.Log.isLoggable("NetworkClient", android.util.Log.DEBUG)) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${AppConstants.Network.API_KEY}")
            .addHeader("Content-Type", "application/json")
            .build()
        chain.proceed(newRequest)
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(AppConstants.Network.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(AppConstants.Network.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(AppConstants.Network.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(AppConstants.Network.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val deepSeekApiService: DeepSeekApiService = retrofit.create(DeepSeekApiService::class.java)
} 