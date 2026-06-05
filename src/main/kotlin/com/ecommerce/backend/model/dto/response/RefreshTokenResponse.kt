package com.ecommerce.backend.model.dto.response

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)