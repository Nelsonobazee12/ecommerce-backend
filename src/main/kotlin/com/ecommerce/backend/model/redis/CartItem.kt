package com.ecommerce.backend.model.redis

import java.io.Serializable
import java.math.BigDecimal

data class CartItem(
    val productId: Long = 0,
    val productName: String = "",
    val productImage: String? = null,
    val price: BigDecimal = BigDecimal.ZERO,
    var quantity: Int = 0
) : Serializable