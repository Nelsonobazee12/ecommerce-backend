package com.ecommerce.backend.service.impl

import com.ecommerce.backend.model.dto.request.ChangePasswordRequest
import com.ecommerce.backend.model.dto.request.UpdateProfileRequest
import com.ecommerce.backend.model.dto.response.UserProfileResponse
import com.ecommerce.backend.repository.UserRepository
import com.ecommerce.backend.service.UserService
import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    override fun getProfile(email: String): UserProfileResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("User not found") }

        return UserProfileResponse(
            id = user.id,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            role = user.role.name,
            isEmailVerified = user.isEmailVerified,
            createdAt = user.createdAt
        )
    }

    @Transactional
    override fun updateProfile(email: String, request: UpdateProfileRequest): UserProfileResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("User not found") }

        user.firstName = request.firstName
        user.lastName = request.lastName
        user.updatedAt = LocalDateTime.now()

        userRepository.save(user)

        return UserProfileResponse(
            id = user.id,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            role = user.role.name,
            isEmailVerified = user.isEmailVerified,
            createdAt = user.createdAt
        )
    }

    @Transactional
    override fun changePassword(email: String, request: ChangePasswordRequest) {
        if (request.newPassword != request.confirmPassword) {
            throw RuntimeException("New password and confirm password do not match")
        }

        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("User not found") }

        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw RuntimeException("Current password is incorrect")
        }

        if (passwordEncoder.matches(request.newPassword, user.password)) {
            throw RuntimeException("New password must be different from current password")
        }

        user.password = passwordEncoder.encode(request.newPassword)
        user.updatedAt = LocalDateTime.now()

        userRepository.save(user)
    }
}