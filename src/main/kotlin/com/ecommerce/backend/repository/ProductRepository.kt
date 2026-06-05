package com.ecommerce.backend.repository

import com.ecommerce.backend.model.entity.Product
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface ProductRepository : JpaRepository<Product, Long> {

    @Query("""
    SELECT p FROM Product p 
    JOIN FETCH p.category c
    WHERE p.isActive = true
    AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
    AND (:categoryId IS NULL OR c.id = :categoryId)
""")
    fun searchProducts(
        search: String?,
        categoryId: Long?,
        pageable: Pageable
    ): Page<Product>

    @Query("""
        SELECT p FROM Product p
        JOIN FETCH p.category
        WHERE p.id = :id AND p.isActive = true
    """)
    fun findActiveById(id: Long): Product?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM Product p
        JOIN FETCH p.category
        WHERE p.id = :id AND p.isActive = true
    """)
    fun findActiveByIdForUpdate(id: Long): Product?
}