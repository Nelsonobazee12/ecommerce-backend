package com.ecommerce.backend.repository

import com.ecommerce.backend.model.entity.Review
import com.ecommerce.backend.model.entity.User
import com.ecommerce.backend.model.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ReviewRepository : JpaRepository<Review, Long> {

    fun existsByUserAndProduct(user: User, product: Product): Boolean

    fun findByProduct(product: Product): List<Review>

    fun findByUser(user: User): List<Review>

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product")
    fun getAverageRating(product: Product): Double?

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product = :product")
    fun getTotalReviews(product: Product): Int
}