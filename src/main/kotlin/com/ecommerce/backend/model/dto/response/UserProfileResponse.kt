package com.ecommerce.backend.model.dto.response

import java.time.LocalDateTime

data class UserProfileResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val isEmailVerified: Boolean,
    val createdAt: LocalDateTime
)