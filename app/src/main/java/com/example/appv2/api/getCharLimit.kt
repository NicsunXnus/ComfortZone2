package com.example.appv2.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface getCharLimit {
    @Headers("Content-Type: application/json")
    @GET("v1/user")
    suspend fun getCharLim(
        @Header("xi-api-key") apiKey: String
    ): Response<ResponseBody>
}
