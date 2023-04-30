package com.example.appv2.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun generateChatGptResponse(
        @Body request: ChatGptRequest,
        @Header("Authorization") authHeader: String
    ): Response<ChatGptResponse>

}
