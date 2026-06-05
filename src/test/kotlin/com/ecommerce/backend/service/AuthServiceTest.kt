package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.request.LoginRequest
import com.ecommerce.backend.model.dto.request.RegisterRequest
import com.ecommerce.backend.model.entity.User
import com.ecommerce.backend.model.enums.Role
import com.ecommerce.backend.repository.EmailVerificationTokenRepository
import com.ecommerce.backend.repository.PasswordResetTokenRepository
import com.ecommerce.backend.repository.RefreshTokenRepository
import com.ecommerce.backend.repository.UserRepository
import com.ecommerce.backend.security.JwtService
import com.ecommerce.backend.services.impl.AuthServiceImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

class AuthServiceTest {

    private lateinit var authService: AuthServiceImpl
    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val emailVerificationTokenRepository = mockk<EmailVerificationTokenRepository>()
    private val passwordResetTokenRepository = mockk<PasswordResetTokenRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val jwtService = mockk<JwtService>()
    private val emailService = mockk<EmailService>()

    private val testUser = User(
        id = 1L,
        firstName = "Nelson",
        lastName = "Doe",
        email = "nelson@example.com",
        password = "encodedPassword",
        role = Role.CUSTOMER,
        isEnabled = true,
        isEmailVerified = true
    )

    @BeforeEach
    fun setup() {
        authService = AuthServiceImpl(
            userRepository,
            refreshTokenRepository,
            emailVerificationTokenRepository,
            passwordResetTokenRepository,
            passwordEncoder,
            jwtService,
            emailService,
            emailEnabled = false  // Disable email for tests
        )
    }

    @Test
    fun `register should create user and return auth response`() {
        val request = RegisterRequest(
            firstName = "Nelson",
            lastName = "Doe",
            email = "nelson@example.com",
            password = "password123"
        )

        every { userRepository.existsByEmail(request.email) } returns false
        every { passwordEncoder.encode(request.password) } returns "encodedPassword"
        every { userRepository.save(any()) } returns testUser
        // Note: When emailEnabled=false, these won't be called
        // every { emailVerificationTokenRepository.save(any()) } returns mockk()
        // every { emailService.sendVerificationEmail(any(), any(), any()) } returns Unit
        every { jwtService.generateToken(any(), any()) } returns "accessToken"
        every { refreshTokenRepository.revokeAllUserTokens(any()) } returns Unit
        every { refreshTokenRepository.save(any()) } returns mockk()

        val result = authService.register(request)

        assertEquals("accessToken", result.token)
        assertEquals("nelson@example.com", result.email)
        verify { userRepository.save(any()) }
        // Email service should NOT be called when emailEnabled=false
        verify(exactly = 0) { emailService.sendVerificationEmail(any(), any(), any()) }
    }

    @Test
    fun `register should throw exception when email already exists`() {
        val request = RegisterRequest(
            firstName = "Nelson",
            lastName = "Doe",
            email = "nelson@example.com",
            password = "password123"
        )

        every { userRepository.existsByEmail(request.email) } returns true

        val exception = assertThrows<RuntimeException> {
            authService.register(request)
        }

        assertEquals("Email already registered", exception.message)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `login should return auth response with valid credentials`() {
        val request = LoginRequest(
            email = "nelson@example.com",
            password = "password123"
        )

        every { userRepository.findByEmail(request.email) } returns Optional.of(testUser)
        every { passwordEncoder.matches(request.password, testUser.password) } returns true
        every { jwtService.generateToken(any(), any()) } returns "accessToken"
        every { refreshTokenRepository.revokeAllUserTokens(any()) } returns Unit
        every { refreshTokenRepository.save(any()) } returns mockk()

        val result = authService.login(request)

        assertEquals("accessToken", result.token)
        assertEquals("nelson@example.com", result.email)
    }

    @Test
    fun `login should throw exception with wrong password`() {
        val request = LoginRequest(
            email = "nelson@example.com",
            password = "wrongpassword"
        )

        every { userRepository.findByEmail(request.email) } returns Optional.of(testUser)
        every { passwordEncoder.matches(request.password, testUser.password) } returns false

        val exception = assertThrows<RuntimeException> {
            authService.login(request)
        }

        assertEquals("Invalid email or password", exception.message)
    }

    @Test
    fun `login should throw exception when user not found`() {
        val request = LoginRequest(
            email = "nobody@example.com",
            password = "password123"
        )

        every { userRepository.findByEmail(request.email) } returns Optional.empty()

        val exception = assertThrows<RuntimeException> {
            authService.login(request)
        }

        assertEquals("Invalid email or password", exception.message)
    }

    @Test
    fun `login should throw exception when account is disabled`() {
        // User is a regular class (not data class), so build a new instance directly
        val disabledUser = User(
            id = 1L,
            firstName = "Nelson",
            lastName = "Doe",
            email = "nelson@example.com",
            password = "encodedPassword",
            role = Role.CUSTOMER,
            isEnabled = false,
            isEmailVerified = true
        )
        val request = LoginRequest(
            email = "nelson@example.com",
            password = "password123"
        )

        every { userRepository.findByEmail(request.email) } returns Optional.of(disabledUser)
        every { passwordEncoder.matches(request.password, disabledUser.password) } returns true

        val exception = assertThrows<RuntimeException> {
            authService.login(request)
        }

        assertEquals("Account is disabled", exception.message)
    }
}