package com.ecommerce.backend.model.dto.request

import jakarta.validation.constraints.NotBlank

data class PlaceOrderRequest(
    @field:NotBlank(message = "Shipping address is required")
    val shippingAddress: String
)