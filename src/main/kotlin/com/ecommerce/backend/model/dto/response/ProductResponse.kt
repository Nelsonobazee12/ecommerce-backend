package com.ecommerce.backend.model.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val stock: Int,
    val imageUrl: String?,
    val isActive: Boolean,
    val category: CategoryResponse,
    val createdAt: LocalDateTime
)