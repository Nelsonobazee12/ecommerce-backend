package com.ecommerce.backend.repository

import com.ecommerce.backend.model.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByNameIgnoreCase(name: String): Optional<Category>
    fun findAllByIsActiveTrue(): List<Category>
    fun existsByNameIgnoreCase(name: String): Boolean
}