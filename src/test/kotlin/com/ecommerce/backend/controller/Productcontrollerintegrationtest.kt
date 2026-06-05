package com.ecommerce.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private lateinit var adminToken: String
    private lateinit var userToken: String
    private var testCategoryId: Int = 0

    @BeforeEach
    fun setup() {
        // Create admin user first
        createAdminUser()
        adminToken = loginAs("admin@ecommerce.com", "Admin123!")

        // Create regular user
        userToken = registerAndLogin("customer@test.com", "Password123!")

        // Create a test category for product tests
        testCategoryId = createCategory(adminToken)
    }

    private fun createAdminUser() {
        val regBody = mapOf(
            "email" to "admin@ecommerce.com",
            "password" to "Admin123!",
            "firstName" to "Admin",
            "lastName" to "User"
        )
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regBody))
        ).andExpect(status().isCreated)
    }

    // ─── Public product listing ────────────────────────────────────────────

    @Test
    fun `list products - public access returns 200`() {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    fun `list products - supports pagination params`() {
        mockMvc.perform(
            get("/api/products")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.size").value(10))
    }

    @Test
    fun `get product by id - non-existent returns 404`() {
        mockMvc.perform(get("/api/products/99999"))
            .andExpect(status().isNotFound)
    }

    // ─── Admin product creation ────────────────────────────────────────────

    @Test
    fun `create product - admin success returns 201`() {
        val body = mapOf(
            "name" to "Test Product",
            "description" to "A test product description",
            "price" to 5000.00,
            "stock" to 100,
            "categoryId" to testCategoryId
        )

        mockMvc.perform(
            post("/api/admin/products")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.name").value("Test Product"))
            .andExpect(jsonPath("$.data.price").value(5000.00))
    }

    @Test
    fun `create product - non-admin returns 403`() {
        val body = mapOf(
            "name" to "Test Product",
            "description" to "desc",
            "price" to 5000.00,
            "stock" to 10,
            "categoryId" to testCategoryId
        )

        mockMvc.perform(
            post("/api/admin/products")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `create product - unauthenticated returns 401`() {
        val body = mapOf("name" to "Test Product", "price" to 5000.00)

        mockMvc.perform(
            post("/api/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `create product - missing required fields returns 400`() {
        val body = mapOf("description" to "no name or price")

        mockMvc.perform(
            post("/api/admin/products")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isBadRequest)
    }

    // ─── Full create-then-fetch flow ───────────────────────────────────────

    @Test
    fun `create product then fetch by id - returns correct product`() {
        val createBody = mapOf(
            "name" to "Fetchable Product",
            "description" to "description",
            "price" to 9999.00,
            "stock" to 50,
            "categoryId" to testCategoryId
        )

        val createResult = mockMvc.perform(
            post("/api/admin/products")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val apiResponse = objectMapper.readValue(
            createResult.response.contentAsString, Map::class.java
        )
        @Suppress("UNCHECKED_CAST")
        val createdProduct = apiResponse["data"] as Map<String, Any>
        val productId = createdProduct["id"]

        mockMvc.perform(get("/api/products/$productId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("Fetchable Product"))
            .andExpect(jsonPath("$.data.price").value(9999.00))
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private fun loginAs(email: String, password: String): String {
        val body = mapOf("email" to email, "password" to password)
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isOk).andReturn()

        check(result.response.contentLength > 0) {
            "Login returned empty body — check rate limiter is disabled in test profile"
        }
        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        @Suppress("UNCHECKED_CAST")
        return (response["data"] as Map<String, Any>)["token"].toString()
    }

    private fun registerAndLogin(email: String, password: String): String {
        val regBody = mapOf(
            "email" to email, "password" to password,
            "firstName" to "Test", "lastName" to "User"
        )
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regBody))
        ).andExpect(status().isCreated)

        return loginAs(email, password)
    }

    private fun createCategory(adminToken: String): Int {
        val body = mapOf("name" to "Test Category", "description" to "Test category desc")
        val result = mockMvc.perform(
            post("/api/admin/categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        @Suppress("UNCHECKED_CAST")
        return (response["data"] as Map<String, Any>)["id"] as Int
    }
}