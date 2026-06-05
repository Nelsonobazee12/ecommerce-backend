package com.ecommerce.backend.model.dto.response

import java.math.BigDecimal

data class OrderItemResponse(
    val productId: Long,
    val productName: String,
    val productImage: String?,
    val price: BigDecimal,
    val quantity: Int,
    val subtotal: BigDecimal
)