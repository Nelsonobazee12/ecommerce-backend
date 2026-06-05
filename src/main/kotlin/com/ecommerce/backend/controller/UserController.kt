package com.ecommerce.backend.controller

import com.ecommerce.backend.model.dto.request.ChangePasswordRequest
import com.ecommerce.backend.model.dto.request.UpdateProfileRequest
import com.ecommerce.backend.model.dto.response.ApiResponse
import com.ecommerce.backend.model.dto.response.UserProfileResponse
import com.ecommerce.backend.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/me")
    fun getProfile(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<UserProfileResponse>> {
        val profile = userService.getProfile(userDetails.username)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Profile retrieved successfully", data = profile)
        )
    }

    @PutMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<ApiResponse<UserProfileResponse>> {
        val profile = userService.updateProfile(userDetails.username, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Profile updated successfully", data = profile)
        )
    }

    @PutMapping("/me/change-password")
    fun changePassword(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        userService.changePassword(userDetails.username, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Password changed successfully")
        )
    }
}