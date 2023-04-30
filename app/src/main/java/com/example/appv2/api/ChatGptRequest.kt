package com.example.appv2.api


data class ChatGptRequest(
    val model: String,
    val messages: List<Message>,
    val options: RequestOptions? = null
)




