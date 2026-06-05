package com.ecommerce.backend.controller

import com.ecommerce.backend.service.TestServiceConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestServiceConfig::class, TestRateLimiterConfig::class)
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val testEmail = "nelson@test.com"
    private val testPassword = "Password123!"

    @BeforeEach
    fun setUp() {
        // Clean up approach - each test is @Transactional so rolls back automatically
    }

    @Test
    fun `register - success returns 201 with user data`() {
        val body = mapOf(
            "email" to "fresh@test.com",  // Use unique email to avoid conflicts
            "password" to testPassword,
            "firstName" to "Nelson",
            "lastName" to "Test"
        )

        val result = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("fresh@test.com"))
            .andExpect(jsonPath("$.data.token").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val data = response["data"] as Map<*, *>
        assert(data["token"] != null)
        assert(data["refreshToken"] != null)
    }

    @Test
    fun `register - duplicate email returns 409`() {
        val body = mapOf(
            "email" to testEmail,
            "password" to testPassword,
            "firstName" to "Nelson",
            "lastName" to "Test"
        )

        // First registration
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isCreated)

        // Duplicate registration should return 409 Conflict
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Email already registered"))
    }

    @Test
    fun `register - invalid email returns 400`() {
        val body = mapOf(
            "email" to "not-an-email",
            "password" to testPassword,
            "firstName" to "Nelson",
            "lastName" to "Test"
        )

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `login - valid credentials returns 200 with tokens`() {
        // Register first
        registerUser(testEmail, testPassword)

        val body = mapOf("email" to testEmail, "password" to testPassword)

        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val data = response["data"] as Map<*, *>
        assert(data["token"] != null)
        assert(data["refreshToken"] != null)
    }

    @Test
    fun `login - wrong password returns 401`() {
        registerUser(testEmail, testPassword)

        val body = mapOf("email" to testEmail, "password" to "WrongPassword!")

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Invalid email or password"))
    }

    @Test
    fun `login - non-existent email returns 401`() {
        val body = mapOf("email" to "ghost@test.com", "password" to testPassword)

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Invalid email or password"))
    }

    @Test
    fun `refresh - valid refresh token returns new access token`() {
        registerUser(testEmail, testPassword)
        val tokens = loginAndGetTokens(testEmail, testPassword)

        val body = mapOf("refreshToken" to tokens["refreshToken"])

        val result = mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val data = response["data"] as Map<*, *>
        assert(data["accessToken"] != null)
    }

    @Test
    fun `refresh - invalid token returns 401`() {
        val body = mapOf("refreshToken" to "invalid.token.here")

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Invalid refresh token"))
    }

    @Test
    fun `protected endpoint - valid token returns 200`() {
        registerUser(testEmail, testPassword)
        val tokens = loginAndGetTokens(testEmail, testPassword)

        mockMvc.perform(
            get("/api/users/me")
                .header("Authorization", "Bearer ${tokens["accessToken"]}")
        ).andExpect(status().isOk)
    }

    @Test
    fun `protected endpoint - no token returns 401`() {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)
    }

    // ─── Helper Methods ───────────────────────────────────────────────────────

    private fun registerUser(email: String, password: String) {
        val body = mapOf(
            "email" to email,
            "password" to password,
            "firstName" to "Nelson",
            "lastName" to "Test"
        )
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isCreated)
    }

    private fun loginAndGetTokens(email: String, password: String): Map<String, String> {
        val body = mapOf("email" to email, "password" to password)
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isOk)
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val data = response["data"] as Map<*, *>

        return mapOf(
            "accessToken" to (data["token"]?.toString() ?: ""),
            "refreshToken" to (data["refreshToken"]?.toString() ?: "")
        )
    }
}