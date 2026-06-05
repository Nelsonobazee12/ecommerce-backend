package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.request.ForgotPasswordRequest
import com.ecommerce.backend.model.dto.request.LoginRequest
import com.ecommerce.backend.model.dto.request.RefreshTokenRequest
import com.ecommerce.backend.model.dto.request.RegisterRequest
import com.ecommerce.backend.model.dto.request.ResetPasswordRequest
import com.ecommerce.backend.model.dto.response.AuthResponse
import com.ecommerce.backend.model.dto.response.RefreshTokenResponse

interface AuthService {
    fun register(request: RegisterRequest): AuthResponse
    fun login(request: LoginRequest): AuthResponse
    fun refresh(request: RefreshTokenRequest): RefreshTokenResponse
    fun logout(request: RefreshTokenRequest)
    fun verifyEmail(token: String)
    fun forgotPassword(request: ForgotPasswordRequest)
    fun resetPassword(request: ResetPasswordRequest)
}