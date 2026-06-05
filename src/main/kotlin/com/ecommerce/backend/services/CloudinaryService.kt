package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.response.ImageUploadResponse
import org.springframework.web.multipart.MultipartFile

interface CloudinaryService {
    fun uploadImage(file: MultipartFile, folder: String): ImageUploadResponse
    fun deleteImage(publicId: String)
}