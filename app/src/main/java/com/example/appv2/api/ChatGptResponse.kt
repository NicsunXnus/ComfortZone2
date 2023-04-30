package com.example.appv2.api

import com.google.gson.annotations.SerializedName

data class ChatGptResponse(
    val id: String,
    @SerializedName("object")
    val response_object: String,
    val created: Long,
    val model: String,
    val usage: Usage,
    val choices: List<Choice>
) {
    data class Usage(
        val prompt_tokens: Int,
        val completion_tokens: Int,
        val total_tokens: Int
    )

    data class Choice(
        val message: Message,
        val index: Int,
        val logprobs: Any?, // The log probabilities, set to null if not required
        val finish_reason: String
    )
}
