package com.example.appv2.api

data class GetUserResponse(
    val subscription: UserInfo,
    val is_new_user: Boolean,
    val xi_api_key : String
)

data class UserInfo(
    val character_limit: Int,
    val tier: String,
    // Add other properties if needed
)