package com.jervis.jarvis_assistant.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// API Request/Response ke liye Data Models
data class Message(val role: String, val content: String)
data class ChatRequest(val model: String, val messages: List<Message>)
data class ChatResponse(val choices: List<Choice>)
data class Choice(val message: Message)

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") token: String, // Bearer YOUR_API_KEY
        @Header("HTTP-Referer") referer: String = "http://localhost",
        @Body request: ChatRequest
    ): ChatResponse
}