package com.ecommerce.backend.model.dto.response

data class CategoryResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val isActive: Boolean
)