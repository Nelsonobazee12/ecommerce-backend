package com.ecommerce.backend.model.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SendNotificationRequest(
    @field:NotNull(message = "User ID is required")
    val userId: Long,

    @field:NotBlank(message = "Title is required")
    val title: String,

    @field:NotBlank(message = "Message is required")
    val message: String,

    val type: String = "INFO"
)