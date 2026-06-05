package com.ecommerce.backend.model.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentResponse(
    val id: Long,
    val orderId: Long,
    val stripePaymentIntentId: String,
    val amount: BigDecimal,
    val currency: String,
    val status: String,
    val clientSecret: String?,
    val createdAt: LocalDateTime
)