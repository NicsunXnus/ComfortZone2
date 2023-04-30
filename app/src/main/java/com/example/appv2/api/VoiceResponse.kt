package com.example.appv2.api

data class VoiceResponse(
    val voices: List<Voice>
)

data class Voice(
    val voice_id: String,
    val name: String,
    val preview_url: String
    // Add other properties if needed
)

data class FineTuning(
    val model_id: String?,
    val is_allowed_to_fine_tune: Boolean,
    val fine_tuning_requested: Boolean,
    val finetuning_state: String
    // Add other properties if needed
)