package com.ecommerce.backend.model.dto.request

import jakarta.validation.constraints.NotBlank

data class UpdateUserRoleRequest(
    @field:NotBlank(message = "Role is required")
    val role: String
)