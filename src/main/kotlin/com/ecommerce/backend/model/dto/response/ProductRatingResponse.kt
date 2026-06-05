package com.ecommerce.backend.model.dto.response

data class ProductRatingResponse(
    val productId: Long,
    val productName: String,
    val averageRating: Double,
    val totalReviews: Int,
    val reviews: List<ReviewResponse>
)