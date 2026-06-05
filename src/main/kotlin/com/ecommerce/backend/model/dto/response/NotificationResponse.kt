package com.ecommerce.backend.model.dto.response

import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val type: String,
    val createdAt: LocalDateTime
)