package com.example.appv2.ui.home

import com.example.appv2.api.Message


data class ChatSession(val name: String, var context: String, var messages: List<Message>)
