package com.example.appv2.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface NarrateApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/text-to-speech/{voice}")
    suspend fun generateVoice(
        @Path("voice") voice: String,
        @Header("xi-api-key") apiKey: String,
        @Body requestBody: NarrateRequest
    ): Response<ResponseBody>
}
