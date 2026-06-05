package com.ecommerce.backend.controller

import com.ecommerce.backend.model.dto.request.ForgotPasswordRequest
import com.ecommerce.backend.model.dto.request.LoginRequest
import com.ecommerce.backend.model.dto.request.RefreshTokenRequest
import com.ecommerce.backend.model.dto.request.RegisterRequest
import com.ecommerce.backend.model.dto.request.ResetPasswordRequest
import com.ecommerce.backend.model.dto.response.ApiResponse
import com.ecommerce.backend.model.dto.response.AuthResponse
import com.ecommerce.backend.model.dto.response.RefreshTokenResponse
import com.ecommerce.backend.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        return try {
            val response = authService.register(request)
            ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse(
                    success = true,
                    message = "Registration successful. Please verify your email.",
                    data = response
                )
            )
        } catch (e: RuntimeException) {
            when {
                e.message == "Email already registered" -> {
                    ResponseEntity.status(HttpStatus.CONFLICT).body(
                        ApiResponse(
                            success = false,
                            message = e.message!!,
                            data = null
                        )
                    )
                }
                else -> {
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResponse(
                            success = false,
                            message = e.message ?: "Registration failed",
                            data = null
                        )
                    )
                }
            }
        }
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(
                ApiResponse(success = true, message = "Login successful", data = response)
            )
        } catch (e: RuntimeException) {
            val status = when {
                e.message == "Invalid email or password" -> HttpStatus.UNAUTHORIZED
                e.message == "Account is disabled" -> HttpStatus.FORBIDDEN
                e.message == "Please verify your email before logging in" -> HttpStatus.FORBIDDEN
                else -> HttpStatus.INTERNAL_SERVER_ERROR
            }
            ResponseEntity.status(status).body(
                ApiResponse(
                    success = false,
                    message = e.message ?: "Login failed",
                    data = null
                )
            )
        }
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<RefreshTokenResponse>> {
        return try {
            val response = authService.refresh(request)
            ResponseEntity.ok(
                ApiResponse(success = true, message = "Token refreshed successfully", data = response)
            )
        } catch (e: RuntimeException) {
            val status = when {
                e.message == "Invalid refresh token" -> HttpStatus.UNAUTHORIZED
                e.message == "Refresh token has been revoked" -> HttpStatus.UNAUTHORIZED
                e.message == "Refresh token has expired" -> HttpStatus.UNAUTHORIZED
                else -> HttpStatus.INTERNAL_SERVER_ERROR
            }
            ResponseEntity.status(status).body(
                ApiResponse(
                    success = false,
                    message = e.message ?: "Refresh failed",
                    data = null
                )
            )
        }
    }

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            authService.logout(request)
            ResponseEntity.ok(
                ApiResponse(success = true, message = "Logged out successfully")
            )
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = e.message ?: "Logout failed"
                )
            )
        }
    }

    @GetMapping("/verify-email")
    fun verifyEmail(@RequestParam token: String): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            authService.verifyEmail(token)
            ResponseEntity.ok(
                ApiResponse(success = true, message = "Email verified successfully")
            )
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = e.message ?: "Email verification failed"
                )
            )
        }
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(
        @Valid @RequestBody request: ForgotPasswordRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            authService.forgotPassword(request)
            ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "If your email is registered you will receive a password reset link"
                )
            )
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = e.message ?: "Password reset request failed"
                )
            )
        }
    }

    @PostMapping("/reset-password")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            authService.resetPassword(request)
            ResponseEntity.ok(
                ApiResponse(success = true, message = "Password reset successfully")
            )
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = e.message ?: "Password reset failed"
                )
            )
        }
    }
}