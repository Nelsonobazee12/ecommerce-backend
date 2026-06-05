package com.ecommerce.backend.service

import com.ecommerce.backend.exception.ConflictException
import com.ecommerce.backend.exception.NotFoundException
import com.ecommerce.backend.model.dto.request.AddToCartRequest
import com.ecommerce.backend.model.dto.request.UpdateCartRequest
import com.ecommerce.backend.model.entity.Category
import com.ecommerce.backend.model.entity.Product
import com.ecommerce.backend.model.redis.CartItem
import com.ecommerce.backend.repository.ProductRepository
import com.ecommerce.backend.service.impl.CartServiceImpl
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisTemplate
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class CartServiceTest {

    private lateinit var cartService: CartServiceImpl
    private val productRepository = mockk<ProductRepository>()

    // RedisTemplate<String, Any> means opsForHash() returns HashOperations<String, Any, Any>.
    // We type the mock accordingly and suppress the unchecked cast warning.
    @Suppress("UNCHECKED_CAST")
    private val hashOps = mockk<HashOperations<String, String, Any>>()

    @Suppress("UNCHECKED_CAST")
    private val redisTemplate = mockk<RedisTemplate<String, Any>>()

    private val testProduct = Product(
        id = 10L,
        name = "Widget",
        price = BigDecimal("25.00"),
        stock = 5,
        isActive = true,
        category = Category()
    )

    @BeforeEach
    fun setup() {
        cartService = CartServiceImpl(redisTemplate, productRepository)

        @Suppress("UNCHECKED_CAST")
        every { redisTemplate.opsForHash<String, Any>() } returns hashOps
        every { redisTemplate.expire(any<String>(), any<Long>(), any<TimeUnit>()) } returns true
        every { redisTemplate.delete(any<String>()) } returns true
    }

    @Test
    fun `addToCart should store item and return correct totals`() {
        every { productRepository.findActiveById(10L) } returns testProduct
        every { hashOps.entries("cart:user@example.com") } returns emptyMap()
        every { hashOps.putAll(any(), any()) } just Runs

        val result = cartService.addToCart("user@example.com", AddToCartRequest(productId = 10L, quantity = 2))

        assertEquals(1, result.items.size)
        assertEquals(BigDecimal("50.00"), result.totalPrice)
        assertEquals(2, result.totalItems)
        verify { hashOps.putAll(any(), any()) }
    }

    @Test
    fun `addToCart should throw NotFoundException when product does not exist`() {
        every { productRepository.findActiveById(99L) } returns null

        assertThrows<NotFoundException> {
            cartService.addToCart("user@example.com", AddToCartRequest(productId = 99L, quantity = 1))
        }
    }

    @Test
    fun `addToCart should throw ConflictException when requested quantity exceeds stock`() {
        every { productRepository.findActiveById(10L) } returns testProduct
        every { hashOps.entries("cart:user@example.com") } returns emptyMap()

        assertThrows<ConflictException> {
            cartService.addToCart("user@example.com", AddToCartRequest(productId = 10L, quantity = 10))
        }
    }

    @Test
    fun `addToCart should accumulate quantity for an item already in the cart`() {
        val existingItem = CartItem(
            productId = 10L, productName = "Widget",
            productImage = null, price = BigDecimal("25.00"), quantity = 2
        )
        every { productRepository.findActiveById(10L) } returns testProduct
        every { hashOps.entries("cart:user@example.com") } returns mapOf("10" to existingItem as Any)
        every { hashOps.putAll(any(), any()) } just Runs

        val result = cartService.addToCart("user@example.com", AddToCartRequest(productId = 10L, quantity = 2))

        assertEquals(4, result.items.first().quantity)
    }

    @Test
    fun `addToCart should throw ConflictException when accumulated quantity exceeds stock`() {
        val existingItem = CartItem(
            productId = 10L, productName = "Widget",
            productImage = null, price = BigDecimal("25.00"), quantity = 4
        )
        every { productRepository.findActiveById(10L) } returns testProduct
        every { hashOps.entries("cart:user@example.com") } returns mapOf("10" to existingItem as Any)

        // stock is 5, already have 4 in cart, adding 2 more = 6 > 5
        assertThrows<ConflictException> {
            cartService.addToCart("user@example.com", AddToCartRequest(productId = 10L, quantity = 2))
        }
    }

    @Test
    fun `getCart should return empty cart when no items are stored`() {
        every { hashOps.entries("cart:user@example.com") } returns emptyMap()

        val result = cartService.getCart("user@example.com")

        assertTrue(result.items.isEmpty())
        assertEquals(BigDecimal.ZERO, result.totalPrice)
        assertEquals(0, result.totalItems)
    }

    @Test
    fun `updateCart should throw ConflictException when new quantity exceeds stock`() {
        val existingItem = CartItem(
            productId = 10L, productName = "Widget",
            productImage = null, price = BigDecimal("25.00"), quantity = 2
        )
        every { productRepository.findActiveById(10L) } returns testProduct
        every { hashOps.entries("cart:user@example.com") } returns mapOf("10" to existingItem as Any)

        assertThrows<ConflictException> {
            cartService.updateCart("user@example.com", UpdateCartRequest(productId = 10L, quantity = 99))
        }
    }

    @Test
    fun `updateCart should throw NotFoundException when item is not in the cart`() {
        every { productRepository.findActiveById(10L) } returns testProduct
        every { hashOps.entries("cart:user@example.com") } returns emptyMap()

        assertThrows<NotFoundException> {
            cartService.updateCart("user@example.com", UpdateCartRequest(productId = 10L, quantity = 1))
        }
    }

    @Test
    fun `removeFromCart should throw NotFoundException when item is not in the cart`() {
        every { hashOps.entries("cart:user@example.com") } returns emptyMap()

        assertThrows<NotFoundException> {
            cartService.removeFromCart("user@example.com", 10L)
        }
    }

    @Test
    fun `removeFromCart should remove item and return updated cart`() {
        val existingItem = CartItem(
            productId = 10L, productName = "Widget",
            productImage = null, price = BigDecimal("25.00"), quantity = 2
        )
        every { hashOps.entries("cart:user@example.com") } returns mapOf("10" to existingItem as Any)
        every { hashOps.putAll(any(), any()) } just Runs

        val result = cartService.removeFromCart("user@example.com", 10L)

        assertTrue(result.items.isEmpty())
    }
}