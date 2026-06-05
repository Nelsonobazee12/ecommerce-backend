package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.request.CreateCategoryRequest
import com.ecommerce.backend.model.dto.request.CreateProductRequest
import com.ecommerce.backend.model.dto.request.UpdateProductRequest
import com.ecommerce.backend.model.dto.response.CategoryResponse
import com.ecommerce.backend.model.dto.response.PageResponse
import com.ecommerce.backend.model.dto.response.ProductResponse

interface ProductService {
    fun createCategory(request: CreateCategoryRequest): CategoryResponse
    fun getAllCategories(): List<CategoryResponse>
    fun createProduct(request: CreateProductRequest): ProductResponse
    fun getProducts(search: String?, categoryId: Long?, page: Int, size: Int, sortBy: String?): PageResponse<ProductResponse>
    fun getProductById(id: Long): ProductResponse
    fun updateProduct(id: Long, request: UpdateProductRequest): ProductResponse
    fun deleteProduct(id: Long)
}