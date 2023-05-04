package com.example.appv2.ui.home

import com.example.appv2.api.Message
import java.time.LocalDateTime

data class ChatHistoryItem(
    val title: String,
    val messages: List<Message>,
    val timestamp: LocalDateTime
)
