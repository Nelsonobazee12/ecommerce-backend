package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.request.ChangePasswordRequest
import com.ecommerce.backend.model.dto.request.UpdateProfileRequest
import com.ecommerce.backend.model.dto.response.UserProfileResponse

interface UserService {
    fun getProfile(email: String): UserProfileResponse
    fun updateProfile(email: String, request: UpdateProfileRequest): UserProfileResponse
    fun changePassword(email: String, request: ChangePasswordRequest)
}