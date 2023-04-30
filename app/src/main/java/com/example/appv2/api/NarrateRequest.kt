package com.example.appv2.api

import com.google.gson.annotations.SerializedName

data class NarrateRequest(
    @SerializedName("text") val text: String,
    @SerializedName("voice_settings") val voice: VoiceSettings
)

data class VoiceSettings(
    val stability: Float,
    val similarity_boost: Float
)
