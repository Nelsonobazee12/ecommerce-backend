package com.ecommerce.backend.service.impl

import com.cloudinary.Cloudinary
import com.ecommerce.backend.model.dto.response.ImageUploadResponse
import com.ecommerce.backend.service.CloudinaryService
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class CloudinaryServiceImpl(
    private val cloudinary: Cloudinary
) : CloudinaryService {

    override fun uploadImage(file: MultipartFile, folder: String): ImageUploadResponse {
        if (file.isEmpty) {
            throw RuntimeException("File is empty")
        }

        val allowedTypes = listOf("image/jpeg", "image/png", "image/webp", "image/jpg")
        if (file.contentType !in allowedTypes) {
            throw RuntimeException("Only JPEG, PNG and WebP images are allowed")
        }

        if (file.size > 5 * 1024 * 1024) {
            throw RuntimeException("File size must not exceed 5MB")
        }

        val options = mapOf(
            "folder" to "ecommerce/$folder",
            "resource_type" to "image"
        )

        val result = cloudinary.uploader().upload(file.bytes, options)

        return ImageUploadResponse(
            url = result["secure_url"] as String,
            publicId = result["public_id"] as String,
            format = result["format"] as String,
            width = result["width"] as Int,
            height = result["height"] as Int
        )
    }

    override fun deleteImage(publicId: String) {
        cloudinary.uploader().destroy(publicId, mapOf("resource_type" to "image"))
    }
}