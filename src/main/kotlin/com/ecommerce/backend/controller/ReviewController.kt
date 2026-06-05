package com.ecommerce.backend.controller

import com.ecommerce.backend.model.dto.request.CreateReviewRequest
import com.ecommerce.backend.model.dto.response.ApiResponse
import com.ecommerce.backend.model.dto.response.ProductRatingResponse
import com.ecommerce.backend.model.dto.response.ReviewResponse
import com.ecommerce.backend.service.ReviewService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ReviewController(
    private val reviewService: ReviewService
) {

    @PostMapping("/reviews")
    fun createReview(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: CreateReviewRequest
    ): ResponseEntity<ApiResponse<ReviewResponse>> {
        val review = reviewService.createReview(userDetails.username, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Review submitted successfully", data = review)
        )
    }

    @GetMapping("/products/{productId}/reviews")
    fun getProductReviews(
        @PathVariable productId: Long
    ): ResponseEntity<ApiResponse<ProductRatingResponse>> {
        val reviews = reviewService.getProductReviews(productId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Reviews retrieved successfully", data = reviews)
        )
    }

    @GetMapping("/reviews/my")
    fun getMyReviews(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<ReviewResponse>>> {
        val reviews = reviewService.getMyReviews(userDetails.username)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Reviews retrieved successfully", data = reviews)
        )
    }

    @DeleteMapping("/reviews/{id}")
    fun deleteReview(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing>> {
        reviewService.deleteReview(userDetails.username, id)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Review deleted successfully")
        )
    }
}