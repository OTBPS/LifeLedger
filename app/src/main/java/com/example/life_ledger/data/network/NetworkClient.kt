package com.example.life_ledger.data.network

import com.example.life_ledger.BuildConfig
import com.example.life_ledger.constants.AppConstants
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络客户端单例
 * 配置和管理网络请求
 */
object NetworkClient {
    
    private var retrofit: Retrofit? = null
    
    /**
     * 获取Retrofit实例
     */
    fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            retrofit = buildRetrofit()
        }
        return retrofit!!
    }
    
    /**
     * 获取API服务
     */
    fun getApiService(): ApiService {
        return getRetrofit().create(ApiService::class.java)
    }
    
    /**
     * 构建Retrofit实例
     */
    private fun buildRetrofit(): Retrofit {
        val gson = GsonBuilder()
            .setLenient()
            .create()
        
        return Retrofit.Builder()
            .baseUrl(AppConstants.Network.BASE_URL)
            .client(buildOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * 构建OkHttpClient
     */
    private fun buildOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(AppConstants.Network.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(AppConstants.Network.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(AppConstants.Network.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(createAuthInterceptor())
            .addInterceptor(createLoggingInterceptor())
        
        return builder.build()
    }
    
    /**
     * 创建认证拦截器
     */
    private fun createAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Authorization", "Bearer ${AppConstants.Network.API_KEY}")
                .header("Content-Type", "application/json")
                .method(original.method, original.body)
            
            chain.proceed(requestBuilder.build())
        }
    }
    
    /**
     * 创建日志拦截器
     */
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        val loggingInterceptor = HttpLoggingInterceptor()
        // 在Debug模式下启用详细日志，Release模式下只记录基本信息
        loggingInterceptor.level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.BASIC
        }
        return loggingInterceptor
    }
    
    /**
     * 检查网络连接状态
     */
    fun isNetworkAvailable(): Boolean {
        // 这里需要Context，暂时返回true
        // 实际实现时需要使用ConnectivityManager检查网络状态
        return true
    }
} 