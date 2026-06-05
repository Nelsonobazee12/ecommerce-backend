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
class CartOrderIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private lateinit var userToken: String
    private lateinit var adminToken: String
    private var productId: Int = 0

    @BeforeEach
    fun setup() {
        // Create admin user first
        createAdminUser()
        adminToken = loginAs("admin@ecommerce.com", "Admin123!")

        // Create regular user
        userToken = registerAndLogin("buyer@test.com", "Password123!")
        productId = createTestProduct(adminToken)
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

    // ─── Cart ──────────────────────────────────────────────────────────────

    @Test
    fun `get cart - empty cart returns 200`() {
        mockMvc.perform(
            get("/api/cart")
                .header("Authorization", "Bearer $userToken")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `add to cart - success returns updated cart`() {
        val body = mapOf("productId" to productId, "quantity" to 2)

        mockMvc.perform(
            post("/api/cart/add")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isArray)
            .andExpect(jsonPath("$.data.items[0].quantity").value(2))
    }

    @Test
    fun `add to cart - unauthenticated returns 401`() {
        val body = mapOf("productId" to productId, "quantity" to 1)

        mockMvc.perform(
            post("/api/cart/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `add to cart - non-existent product returns 404`() {
        val body = mapOf("productId" to 99999, "quantity" to 1)

        mockMvc.perform(
            post("/api/cart/add")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `remove from cart - success returns updated cart`() {
        addToCart(userToken, productId, 1)

        mockMvc.perform(
            delete("/api/cart/remove/$productId")
                .header("Authorization", "Bearer $userToken")
        ).andExpect(status().isOk)
    }

    @Test
    fun `clear cart - success empties cart`() {
        addToCart(userToken, productId, 3)

        mockMvc.perform(
            delete("/api/cart/clear")
                .header("Authorization", "Bearer $userToken")
        ).andExpect(status().isOk)

        // Cart should now be empty
        mockMvc.perform(
            get("/api/cart")
                .header("Authorization", "Bearer $userToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isEmpty)
    }

    // ─── Order ─────────────────────────────────────────────────────────────

    @Test
    fun `place order - empty cart returns 400`() {
        mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("shippingAddress" to "123 Lagos St")))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `place order - with items returns 201 with PENDING status`() {
        addToCart(userToken, productId, 1)

        val body = mapOf("shippingAddress" to "123 Lagos, Nigeria")

        mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.items").isArray)
    }

    @Test
    fun `get my orders - returns order list`() {
        addToCart(userToken, productId, 1)
        placeOrder(userToken)

        mockMvc.perform(
            get("/api/orders")
                .header("Authorization", "Bearer $userToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(1))
    }

    @Test
    fun `cancel order - PENDING order can be cancelled`() {
        addToCart(userToken, productId, 1)
        val orderId = placeOrder(userToken)

        mockMvc.perform(
            delete("/api/orders/$orderId/cancel")
                .header("Authorization", "Bearer $userToken")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("CANCELLED"))
    }

    @Test
    fun `admin update order status - success`() {
        addToCart(userToken, productId, 1)
        val orderId = placeOrder(userToken)

        val body = mapOf("status" to "CONFIRMED")

        mockMvc.perform(
            put("/api/admin/orders/$orderId/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private fun loginAs(email: String, password: String): String {
        val body = mapOf("email" to email, "password" to password)
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isOk).andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val data = response["data"] as Map<*, *>
        return data["token"].toString()
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

    private fun createTestProduct(adminToken: String): Int {
        val catBody = mapOf("name" to "Electronics", "description" to "Electronic items")
        val catResult = mockMvc.perform(
            post("/api/admin/categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(catBody))
        ).andExpect(status().isCreated).andReturn()

        val catResponse = objectMapper.readValue(catResult.response.contentAsString, Map::class.java)
        val catData = catResponse["data"] as Map<*, *>
        val categoryId = catData["id"] as Int

        val prodBody = mapOf(
            "name" to "Test Widget",
            "description" to "A widget for testing",
            "price" to 1500.00,
            "stock" to 100,
            "categoryId" to categoryId
        )
        val prodResult = mockMvc.perform(
            post("/api/admin/products")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prodBody))
        ).andExpect(status().isCreated).andReturn()

        val prodResponse = objectMapper.readValue(prodResult.response.contentAsString, Map::class.java)
        val prodData = prodResponse["data"] as Map<*, *>
        return prodData["id"] as Int
    }

    private fun addToCart(token: String, productId: Int, qty: Int) {
        val body = mapOf("productId" to productId, "quantity" to qty)
        mockMvc.perform(
            post("/api/cart/add")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isOk)
    }

    private fun placeOrder(token: String): Int {
        val body = mapOf("shippingAddress" to "123 Lagos, Nigeria")
        val result = mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isCreated).andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val data = response["data"] as Map<*, *>
        return data["id"] as Int
    }
}