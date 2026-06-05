package com.ecommerce.backend.model.dto.response

import java.time.LocalDateTime

data class ReviewResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val userFirstName: String,
    val userLastName: String,
    val rating: Int,
    val comment: String?,
    val createdAt: LocalDateTime
)