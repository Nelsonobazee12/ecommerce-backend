package com.ecommerce.backend.controller

import com.ecommerce.backend.model.dto.request.CreateProductRequest
import com.ecommerce.backend.model.dto.request.UpdateProductRequest
import com.ecommerce.backend.model.dto.response.ApiResponse
import com.ecommerce.backend.model.dto.response.PageResponse
import com.ecommerce.backend.model.dto.response.ProductResponse
import com.ecommerce.backend.service.ProductService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
class ProductController(
    private val productService: ProductService
) {

    @PostMapping("/api/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    fun createProduct(
        @Valid @RequestBody request: CreateProductRequest
    ): ResponseEntity<ApiResponse<ProductResponse>> {
        val product = productService.createProduct(request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Product created successfully", data = product)
        )
    }

    @GetMapping("/api/products")
    fun getProducts(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) sortBy: String?
    ): ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> {
        val products = productService.getProducts(search, categoryId, page, size, sortBy)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Products retrieved successfully", data = products)
        )
    }

    @GetMapping("/api/products/{id}")
    fun getProductById(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<ProductResponse>> {
        val product = productService.getProductById(id)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Product retrieved successfully", data = product)
        )
    }

    @PutMapping("/api/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateProduct(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProductRequest
    ): ResponseEntity<ApiResponse<ProductResponse>> {
        val product = productService.updateProduct(id, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Product updated successfully", data = product)
        )
    }

    @DeleteMapping("/api/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteProduct(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing>> {
        productService.deleteProduct(id)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Product deleted successfully")
        )
    }
}