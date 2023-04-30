package com.example.appv2.api

data class RequestOptions(
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    // Add other options as needed
)