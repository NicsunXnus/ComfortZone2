package com.example.appv2.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface getVoicesAPIService {
    @Headers("Content-Type: application/json")
    @GET("v1/voices")
    suspend fun getVoices(
        @Header("xi-api-key") apiKey: String
    ): Response<ResponseBody>
}
