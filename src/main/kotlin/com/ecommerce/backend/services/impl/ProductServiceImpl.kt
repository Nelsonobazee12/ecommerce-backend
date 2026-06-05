package com.ecommerce.backend.service.impl

import com.ecommerce.backend.exception.NotFoundException
import com.ecommerce.backend.model.dto.request.CreateCategoryRequest
import com.ecommerce.backend.model.dto.request.CreateProductRequest
import com.ecommerce.backend.model.dto.request.UpdateProductRequest
import com.ecommerce.backend.model.dto.response.CategoryResponse
import com.ecommerce.backend.model.dto.response.PageResponse
import com.ecommerce.backend.model.dto.response.ProductResponse
import com.ecommerce.backend.model.entity.Category
import com.ecommerce.backend.model.entity.Product
import com.ecommerce.backend.repository.CategoryRepository
import com.ecommerce.backend.repository.ProductRepository
import com.ecommerce.backend.service.ProductService
import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ProductService {

    override fun createCategory(request: CreateCategoryRequest): CategoryResponse {
        if (categoryRepository.existsByNameIgnoreCase(request.name)) {
            throw RuntimeException("Category already exists")
        }

        val category = Category(
            name = request.name,
            description = request.description
        )

        categoryRepository.save(category)
        return category.toResponse()
    }

    override fun getAllCategories(): List<CategoryResponse> {
        return categoryRepository.findAllByIsActiveTrue().map { it.toResponse() }
    }

    @Transactional
    override fun createProduct(request: CreateProductRequest): ProductResponse {
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { RuntimeException("Category not found") }

        val product = Product(
            name = request.name,
            description = request.description,
            price = request.price,
            stock = request.stock,
            imageUrl = request.imageUrl,
            category = category
        )

        productRepository.save(product)
        return product.toResponse()
    }

    override fun getProducts(
        search: String?,
        categoryId: Long?,
        page: Int,
        size: Int,
        sortBy: String?
    ): PageResponse<ProductResponse> {
        val sort = when (sortBy) {
            "PRICE_ASC" -> Sort.by("price").ascending()
            "PRICE_DESC" -> Sort.by("price").descending()
            "NAME" -> Sort.by("name").ascending()
            else -> Sort.by("createdAt").descending()
        }

        val pageable = PageRequest.of(page, size, sort)
        val result = productRepository.searchProducts(search, categoryId, pageable)

        return PageResponse(
            content = result.content.map { it.toResponse() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            isLast = result.isLast
        )
    }

    override fun getProductById(id: Long): ProductResponse {
        val product = productRepository.findActiveById(id)
            ?: throw NotFoundException("Product not found")
        return product.toResponse()
    }

    @Transactional
    override fun updateProduct(id: Long, request: UpdateProductRequest): ProductResponse {
        val product = productRepository.findById(id)
            .orElseThrow { RuntimeException("Product not found") }

        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { RuntimeException("Category not found") }

        product.name = request.name
        product.description = request.description
        product.price = request.price
        product.stock = request.stock
        product.imageUrl = request.imageUrl
        product.isActive = request.isActive
        product.category = category
        product.updatedAt = LocalDateTime.now()

        productRepository.save(product)
        return product.toResponse()
    }

    @Transactional
    override fun deleteProduct(id: Long) {
        val product = productRepository.findById(id)
            .orElseThrow { RuntimeException("Product not found") }

        product.isActive = false
        product.updatedAt = LocalDateTime.now()
        productRepository.save(product)
    }

    // Mapper extensions
    private fun Category.toResponse() = CategoryResponse(
        id = id,
        name = name,
        description = description,
        isActive = isActive
    )

    private fun Product.toResponse() = ProductResponse(
        id = id,
        name = name,
        description = description,
        price = price,
        stock = stock,
        imageUrl = imageUrl,
        isActive = isActive,
        category = category.toResponse(),
        createdAt = createdAt
    )
}