package com.ecommerce.backend.model.dto.response

import java.math.BigDecimal

data class CartResponse(
    val items: List<CartItemResponse>,
    val totalItems: Int,
    val totalPrice: BigDecimal
)