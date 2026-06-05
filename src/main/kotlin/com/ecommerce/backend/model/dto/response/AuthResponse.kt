package com.ecommerce.backend.model.dto.response

data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String
)