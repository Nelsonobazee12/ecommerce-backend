package com.ecommerce.backend.model.dto.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateProductRequest(
    @field:NotBlank(message = "Product name is required")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Price is required")
    @field:DecimalMin(value = "0.01", message = "Price must be greater than 0")
    val price: BigDecimal,

    @field:NotNull(message = "Stock is required")
    @field:Min(value = 0, message = "Stock cannot be negative")
    val stock: Int,

    val imageUrl: String? = null,

    @field:NotNull(message = "Category is required")
    val categoryId: Long
)