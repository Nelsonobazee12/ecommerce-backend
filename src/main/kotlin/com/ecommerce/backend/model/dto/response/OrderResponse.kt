package com.ecommerce.backend.model.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderResponse(
    val id: Long,
    val status: String,
    val totalPrice: BigDecimal,
    val shippingAddress: String,
    val items: List<OrderItemResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)