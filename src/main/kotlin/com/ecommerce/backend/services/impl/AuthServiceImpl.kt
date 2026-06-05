package com.ecommerce.backend.services.impl

import com.ecommerce.backend.model.dto.request.ForgotPasswordRequest
import com.ecommerce.backend.model.dto.request.LoginRequest
import com.ecommerce.backend.model.dto.request.RefreshTokenRequest
import com.ecommerce.backend.model.dto.request.RegisterRequest
import com.ecommerce.backend.model.dto.request.ResetPasswordRequest
import com.ecommerce.backend.model.dto.response.AuthResponse
import com.ecommerce.backend.model.dto.response.RefreshTokenResponse
import com.ecommerce.backend.model.entity.EmailVerificationToken
import com.ecommerce.backend.model.entity.PasswordResetToken
import com.ecommerce.backend.model.entity.RefreshToken
import com.ecommerce.backend.model.entity.User
import com.ecommerce.backend.model.enums.Role
import com.ecommerce.backend.repository.EmailVerificationTokenRepository
import com.ecommerce.backend.repository.PasswordResetTokenRepository
import com.ecommerce.backend.repository.RefreshTokenRepository
import com.ecommerce.backend.repository.UserRepository
import com.ecommerce.backend.security.JwtService
import com.ecommerce.backend.service.AuthService
import com.ecommerce.backend.service.EmailService
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val emailService: EmailService,
    @Value("\${app.email.enabled:true}") private val emailEnabled: Boolean  // Add this property
) : AuthService {

    @Transactional
    override fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw RuntimeException("Email already registered")
        }

        val user = User(
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            role = Role.CUSTOMER,
            isEnabled = true,
            isEmailVerified = !emailEnabled  // Auto-verify if email is disabled
        )

        userRepository.save(user)

        // Only create and send verification token if email is enabled
        if (emailEnabled) {
            // Generate and save email verification token
            val verificationToken = EmailVerificationToken(
                token = UUID.randomUUID().toString(),
                user = user,
                expiresAt = LocalDateTime.now().plusHours(24)
            )
            emailVerificationTokenRepository.save(verificationToken)

            // Send verification email
            emailService.sendVerificationEmail(
                email = user.email,
                firstName = user.firstName,
                token = verificationToken.token
            )
        }

        // Generate tokens
        val accessToken = jwtService.generateToken(user.email, user.role.name)
        val refreshToken = createRefreshToken(user)

        return AuthResponse(
            token = accessToken,
            refreshToken = refreshToken,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role.name
        )
    }

    @Transactional
    override fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { RuntimeException("Invalid email or password") }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw RuntimeException("Invalid email or password")
        }

        // Only check email verification if email is enabled
        if (emailEnabled && !user.isEmailVerified) {
            throw RuntimeException("Please verify your email before logging in")
        }

        if (!user.isEnabled) {
            throw RuntimeException("Account is disabled")
        }

        val accessToken = jwtService.generateToken(user.email, user.role.name)
        val refreshToken = createRefreshToken(user)

        return AuthResponse(
            token = accessToken,
            refreshToken = refreshToken,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role.name
        )
    }

    @Transactional
    override fun refresh(request: RefreshTokenRequest): RefreshTokenResponse {
        val refreshToken = refreshTokenRepository.findByToken(request.refreshToken)
            .orElseThrow { RuntimeException("Invalid refresh token") }

        if (refreshToken.isRevoked) {
            throw RuntimeException("Refresh token has been revoked")
        }

        if (refreshToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw RuntimeException("Refresh token has expired")
        }

        val user = refreshToken.user

        // Revoke old refresh token
        refreshToken.isRevoked = true
        refreshTokenRepository.save(refreshToken)

        // Generate new tokens
        val newAccessToken = jwtService.generateToken(user.email, user.role.name)
        val newRefreshToken = createRefreshToken(user)

        return RefreshTokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional
    override fun logout(request: RefreshTokenRequest) {
        val refreshToken = refreshTokenRepository.findByToken(request.refreshToken)
            .orElseThrow { RuntimeException("Invalid refresh token") }

        refreshToken.isRevoked = true
        refreshTokenRepository.save(refreshToken)
    }

    @Transactional
    override fun verifyEmail(token: String) {
        if (!emailEnabled) {
            // If email is disabled, verification is not needed
            return
        }

        val verificationToken = emailVerificationTokenRepository.findByToken(token)
            .orElseThrow { RuntimeException("Invalid verification token") }

        if (verificationToken.isUsed) {
            throw RuntimeException("Token already used")
        }

        if (verificationToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw RuntimeException("Verification token has expired")
        }

        val user = verificationToken.user
        user.isEmailVerified = true
        userRepository.save(user)

        verificationToken.isUsed = true
        emailVerificationTokenRepository.save(verificationToken)
    }

    @Transactional
    override fun forgotPassword(request: ForgotPasswordRequest) {
        // Silent fail if email is disabled or user doesn't exist
        if (!emailEnabled) {
            return
        }

        val user = userRepository.findByEmail(request.email)
            .orElse(null) ?: return // Silent fail — don't reveal if email exists

        // Invalidate existing tokens
        passwordResetTokenRepository.invalidateAllUserTokens(user)

        // Generate new token
        val token = UUID.randomUUID().toString()
        val resetToken = PasswordResetToken(
            token = token,
            user = user,
            expiresAt = LocalDateTime.now().plusMinutes(15)
        )
        passwordResetTokenRepository.save(resetToken)

        // Send email
        emailService.sendPasswordResetEmail(
            email = user.email,
            firstName = user.firstName,
            token = token
        )
    }

    @Transactional
    override fun resetPassword(request: ResetPasswordRequest) {
        if (request.newPassword != request.confirmPassword) {
            throw RuntimeException("Passwords do not match")
        }

        val resetToken = passwordResetTokenRepository.findByToken(request.token)
            .orElseThrow { RuntimeException("Invalid or expired reset token") }

        if (resetToken.isUsed) {
            throw RuntimeException("Reset token has already been used")
        }

        if (resetToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw RuntimeException("Reset token has expired")
        }

        val user = resetToken.user

        if (passwordEncoder.matches(request.newPassword, user.password)) {
            throw RuntimeException("New password must be different from current password")
        }

        user.password = passwordEncoder.encode(request.newPassword)
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)

        resetToken.isUsed = true
        passwordResetTokenRepository.save(resetToken)

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllUserTokens(user)
    }

    private fun createRefreshToken(user: User): String {
        // Revoke any existing refresh tokens
        refreshTokenRepository.revokeAllUserTokens(user)

        val token = UUID.randomUUID().toString()
        val refreshToken = RefreshToken(
            token = token,
            user = user,
            expiresAt = LocalDateTime.now().plusDays(7)
        )
        refreshTokenRepository.save(refreshToken)
        return token
    }
}