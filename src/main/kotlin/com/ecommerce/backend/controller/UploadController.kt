package com.ecommerce.backend.controller

import com.ecommerce.backend.model.dto.response.ApiResponse
import com.ecommerce.backend.model.dto.response.ImageUploadResponse
import com.ecommerce.backend.model.dto.response.ProductResponse
import com.ecommerce.backend.repository.ProductRepository
import com.ecommerce.backend.service.CloudinaryService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/upload")
class UploadController(
    private val cloudinaryService: CloudinaryService,
    private val productRepository: ProductRepository
) {

    @PostMapping("/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("folder", defaultValue = "general") folder: String
    ): ResponseEntity<ApiResponse<ImageUploadResponse>> {
        val result = cloudinaryService.uploadImage(file, folder)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Image uploaded successfully", data = result)
        )
    }

    @PostMapping(
        "/product-image/{productId}",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun uploadProductImage(
        @PathVariable productId: Long,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ApiResponse<ImageUploadResponse>> {
        val product = productRepository.findById(productId)
            .orElseThrow { RuntimeException("Product not found") }

        // Delete old image if exists
        if (!product.imageUrl.isNullOrEmpty()) {
            try {
                val publicId = extractPublicId(product.imageUrl!!)
                cloudinaryService.deleteImage(publicId)
            } catch (e: Exception) {
                // ignore if old image deletion fails
            }
        }

        val result = cloudinaryService.uploadImage(file, "products")

        product.imageUrl = result.url
        product.updatedAt = LocalDateTime.now()
        productRepository.save(product)

        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Product image uploaded successfully", data = result)
        )
    }

    @DeleteMapping("/image")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteImage(
        @RequestParam publicId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        cloudinaryService.deleteImage(publicId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Image deleted successfully")
        )
    }

    private fun extractPublicId(imageUrl: String): String {
        // Extract public_id from Cloudinary URL
        // e.g. https://res.cloudinary.com/cloud/image/upload/v123/ecommerce/products/abc.jpg
        // returns ecommerce/products/abc
        val regex = Regex("upload/(?:v\\d+/)?(.+)\\.[^.]+$")
        return regex.find(imageUrl)?.groupValues?.get(1)
            ?: throw RuntimeException("Could not extract public ID from URL")
    }
}