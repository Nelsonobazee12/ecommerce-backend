package com.ecommerce.backend.service.impl

import com.ecommerce.backend.model.dto.request.CreateReviewRequest
import com.ecommerce.backend.model.dto.response.ProductRatingResponse
import com.ecommerce.backend.model.dto.response.ReviewResponse
import com.ecommerce.backend.model.entity.Review
import com.ecommerce.backend.repository.OrderRepository
import com.ecommerce.backend.repository.ProductRepository
import com.ecommerce.backend.repository.ReviewRepository
import com.ecommerce.backend.repository.UserRepository
import com.ecommerce.backend.service.ReviewService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ReviewServiceImpl(
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) : ReviewService {

    @Transactional
    override fun createReview(email: String, request: CreateReviewRequest): ReviewResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("User not found") }

        val product = productRepository.findActiveById(request.productId)
            ?: throw RuntimeException("Product not found")

        // Check if user has purchased the product in a non-cancelled order
        val orders = orderRepository.findAllByUser(user)
        val hasPurchased = orders.any { order ->
            order.status != com.ecommerce.backend.model.enums.OrderStatus.CANCELLED &&
                    order.items.any { it.product.id == product.id }
        }

        if (!hasPurchased) {
            throw RuntimeException("You can only review products you have purchased")
        }

        if (reviewRepository.existsByUserAndProduct(user, product)) {
            throw RuntimeException("You have already reviewed this product")
        }

        val review = Review(
            user = user,
            product = product,
            rating = request.rating,
            comment = request.comment
        )

        reviewRepository.save(review)
        return review.toResponse()
    }

    override fun getProductReviews(productId: Long): ProductRatingResponse {
        val product = productRepository.findActiveById(productId)
            ?: throw RuntimeException("Product not found")

        val reviews = reviewRepository.findByProduct(product)
        val averageRating = reviewRepository.getAverageRating(product) ?: 0.0
        val totalReviews = reviewRepository.getTotalReviews(product)

        return ProductRatingResponse(
            productId = product.id,
            productName = product.name,
            averageRating = Math.round(averageRating * 10.0) / 10.0,
            totalReviews = totalReviews,
            reviews = reviews.map { it.toResponse() }
        )
    }

    override fun getMyReviews(email: String): List<ReviewResponse> {
        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("User not found") }

        return reviewRepository.findByUser(user).map { it.toResponse() }
    }

    @Transactional
    override fun deleteReview(email: String, reviewId: Long) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("User not found") }

        val review = reviewRepository.findById(reviewId)
            .orElseThrow { RuntimeException("Review not found") }

        if (review.user.id != user.id) {
            throw RuntimeException("You can only delete your own reviews")
        }

        reviewRepository.delete(review)
    }

    private fun Review.toResponse() = ReviewResponse(
        id = id,
        productId = product.id,
        productName = product.name,
        userFirstName = user.firstName,
        userLastName = user.lastName,
        rating = rating,
        comment = comment,
        createdAt = createdAt
    )
}