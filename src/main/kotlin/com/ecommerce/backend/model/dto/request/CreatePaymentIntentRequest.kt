package com.ecommerce.backend.model.dto.request

import jakarta.validation.constraints.NotNull

data class CreatePaymentIntentRequest(
    @field:NotNull(message = "Order ID is required")
    val orderId: Long
)