package com.example.life_ledger.data.network

import retrofit2.Response
import retrofit2.http.*

/**
 * API服务接口
 * 定义网络请求接口，主要用于AI功能
 */
interface ApiService {
    
    /**
     * 向DeepSeek AI发送请求
     */
    @POST("v1/chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun sendChatRequest(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
    
    /**
     * 获取AI财务分析
     */
    @POST("v1/chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun getFinancialAnalysis(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
    
    /**
     * 获取AI待办建议
     */
    @POST("v1/chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun getTodoSuggestions(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

/**
 * 聊天请求数据类
 */
data class ChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<ChatMessage>,
    val max_tokens: Int = 2000,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)

/**
 * 聊天消息数据类
 */
data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

/**
 * 聊天响应数据类
 */
data class ChatResponse(
    val id: String,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage?
)

/**
 * 聊天选择数据类
 */
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String?
)

/**
 * 使用统计数据类
 */
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
) 