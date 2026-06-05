package com.ecommerce.backend.model.dto.request

import jakarta.validation.constraints.NotBlank

data class CreateCategoryRequest(
    @field:NotBlank(message = "Category name is required")
    val name: String,

    val description: String? = null
)