package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.request.CreateReviewRequest
import com.ecommerce.backend.model.dto.response.ProductRatingResponse
import com.ecommerce.backend.model.dto.response.ReviewResponse

interface ReviewService {
    fun createReview(email: String, request: CreateReviewRequest): ReviewResponse
    fun getProductReviews(productId: Long): ProductRatingResponse
    fun getMyReviews(email: String): List<ReviewResponse>
    fun deleteReview(email: String, reviewId: Long)
}