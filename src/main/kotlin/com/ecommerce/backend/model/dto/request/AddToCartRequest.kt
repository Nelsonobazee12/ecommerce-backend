package com.ecommerce.backend.model.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class AddToCartRequest(
    @field:NotNull(message = "Product ID is required")
    val productId: Long,

    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int
)