package com.ecommerce.backend.model.dto.request

import jakarta.validation.constraints.NotNull

data class UpdateUserStatusRequest(
    @field:NotNull(message = "Status is required")
    val isEnabled: Boolean
)