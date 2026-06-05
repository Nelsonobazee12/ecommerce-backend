package com.ecommerce.backend.controller

import com.ecommerce.backend.model.dto.request.CreateCategoryRequest
import com.ecommerce.backend.model.dto.response.ApiResponse
import com.ecommerce.backend.model.dto.response.CategoryResponse
import com.ecommerce.backend.service.ProductService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
class CategoryController(
    private val productService: ProductService
) {

    @PostMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    fun createCategory(
        @Valid @RequestBody request: CreateCategoryRequest
    ): ResponseEntity<ApiResponse<CategoryResponse>> {
        val category = productService.createCategory(request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Category created successfully", data = category)
        )
    }

    @GetMapping("/api/categories")
    fun getAllCategories(): ResponseEntity<ApiResponse<List<CategoryResponse>>> {
        val categories = productService.getAllCategories()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Categories retrieved successfully", data = categories)
        )
    }
}