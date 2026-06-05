package com.ecommerce.backend.model.dto.response

data class ImageUploadResponse(
    val url: String,
    val publicId: String,
    val format: String,
    val width: Int,
    val height: Int
)