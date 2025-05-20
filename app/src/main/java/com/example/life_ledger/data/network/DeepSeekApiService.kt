package com.example.life_ledger.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * DeepSeek API服务接口
 */
interface DeepSeekApiService {
    
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: ChatCompletionRequest): Response<ChatCompletionResponse>
    
    companion object {
        const val BASE_URL = "https://api.deepseek.com/"
    }
}

/**
 * 聊天完成请求
 */
data class ChatCompletionRequest(
    val model: String = "deepseek-chat",
    val messages: List<Message>,
    val max_tokens: Int = 2048,
    val temperature: Double = 0.7,
    val top_p: Double = 0.95,
    val frequency_penalty: Double = 0.0,
    val presence_penalty: Double = 0.0,
    val stream: Boolean = false
)

/**
 * 消息
 */
data class Message(
    val role: String, // "system", "user", "assistant"
    val content: String
)

/**
 * 聊天完成响应
 */
data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
)

/**
 * 选择
 */
data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

/**
 * 使用情况
 */
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
) 