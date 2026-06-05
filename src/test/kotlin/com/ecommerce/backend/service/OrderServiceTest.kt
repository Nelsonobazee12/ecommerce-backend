package com.ecommerce.backend.service

import com.ecommerce.backend.exception.BadRequestException
import com.ecommerce.backend.exception.ConflictException
import com.ecommerce.backend.exception.NotFoundException
import com.ecommerce.backend.model.dto.request.PlaceOrderRequest
import com.ecommerce.backend.model.dto.request.UpdateOrderStatusRequest
import com.ecommerce.backend.model.dto.response.CartItemResponse
import com.ecommerce.backend.model.dto.response.CartResponse
import com.ecommerce.backend.model.entity.Category
import com.ecommerce.backend.model.entity.Order
import com.ecommerce.backend.model.entity.OrderItem
import com.ecommerce.backend.model.entity.Product
import com.ecommerce.backend.model.entity.User
import com.ecommerce.backend.model.enums.OrderStatus
import com.ecommerce.backend.model.enums.Role
import com.ecommerce.backend.repository.OrderRepository
import com.ecommerce.backend.repository.ProductRepository
import com.ecommerce.backend.repository.UserRepository
import com.ecommerce.backend.service.impl.OrderServiceImpl
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Optional

class OrderServiceTest {

    private lateinit var orderService: OrderServiceImpl
    private val orderRepository = mockk<OrderRepository>()
    private val userRepository = mockk<UserRepository>()
    private val productRepository = mockk<ProductRepository>()
    private val cartService = mockk<CartService>()
    private val notificationService = mockk<NotificationService>(relaxed = true)

    private val testUser = User(
        id = 1L,
        firstName = "Test",
        lastName = "User",
        email = "test@example.com",
        password = "encoded",
        role = Role.CUSTOMER,
        isEnabled = true,
        isEmailVerified = true
    )

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
        orderService = OrderServiceImpl(
            orderRepository, userRepository, productRepository, cartService, notificationService
        )
    }

    // -------------------------------------------------------------------------
    // placeOrder
    // -------------------------------------------------------------------------

    @Test
    fun `placeOrder should create order, decrement stock, and clear cart`() {
        val cartItem = CartItemResponse(
            productId = 10L,
            productName = "Widget",
            productImage = null,
            price = BigDecimal("25.00"),
            quantity = 2,
            subtotal = BigDecimal("50.00")
        )
        val cart = CartResponse(
            items = listOf(cartItem),
            totalItems = 2,
            totalPrice = BigDecimal("50.00")
        )

        every { userRepository.findByEmail(testUser.email) } returns Optional.of(testUser)
        every { cartService.getCart(testUser.email) } returns cart
        every { productRepository.findActiveByIdForUpdate(10L) } returns testProduct
        every { productRepository.save(testProduct) } returns testProduct   // called after stock decrement
        every { orderRepository.save(any()) } answers { firstArg() }
        every { cartService.clearCart(testUser.email) } just Runs

        val result = orderService.placeOrder(testUser.email, PlaceOrderRequest("123 Main St"))

        assertEquals(BigDecimal("50.00"), result.totalPrice)
        assertEquals(3, testProduct.stock)      // 5 - 2
        verify { orderRepository.save(any()) }
        verify { cartService.clearCart(testUser.email) }
    }

    @Test
    fun `placeOrder should throw BadRequestException when cart is empty`() {
        val emptyCart = CartResponse(items = emptyList(), totalItems = 0, totalPrice = BigDecimal.ZERO)

        every { userRepository.findByEmail(testUser.email) } returns Optional.of(testUser)
        every { cartService.getCart(testUser.email) } returns emptyCart

        assertThrows<BadRequestException> {
            orderService.placeOrder(testUser.email, PlaceOrderRequest("123 Main St"))
        }
    }

    @Test
    fun `placeOrder should throw ConflictException when stock is insufficient`() {
        val cartItem = CartItemResponse(
            productId = 10L,
            productName = "Widget",
            productImage = null,
            price = BigDecimal("25.00"),
            quantity = 10,              // exceeds stock of 5
            subtotal = BigDecimal("250.00")
        )
        val cart = CartResponse(items = listOf(cartItem), totalItems = 10, totalPrice = BigDecimal("250.00"))

        every { userRepository.findByEmail(testUser.email) } returns Optional.of(testUser)
        every { cartService.getCart(testUser.email) } returns cart
        every { productRepository.findActiveByIdForUpdate(10L) } returns testProduct

        assertThrows<ConflictException> {
            orderService.placeOrder(testUser.email, PlaceOrderRequest("123 Main St"))
        }
    }

    @Test
    fun `placeOrder should throw NotFoundException when product is no longer available`() {
        val cartItem = CartItemResponse(
            productId = 99L,
            productName = "Ghost Product",
            productImage = null,
            price = BigDecimal("10.00"),
            quantity = 1,
            subtotal = BigDecimal("10.00")
        )
        val cart = CartResponse(items = listOf(cartItem), totalItems = 1, totalPrice = BigDecimal("10.00"))

        every { userRepository.findByEmail(testUser.email) } returns Optional.of(testUser)
        every { cartService.getCart(testUser.email) } returns cart
        every { productRepository.findActiveByIdForUpdate(99L) } returns null

        assertThrows<NotFoundException> {
            orderService.placeOrder(testUser.email, PlaceOrderRequest("123 Main St"))
        }
    }

    @Test
    fun `placeOrder should throw NotFoundException when user does not exist`() {
        every { userRepository.findByEmail("ghost@example.com") } returns Optional.empty()

        assertThrows<NotFoundException> {
            orderService.placeOrder("ghost@example.com", PlaceOrderRequest("123 Main St"))
        }
    }

    // -------------------------------------------------------------------------
    // cancelOrder
    // -------------------------------------------------------------------------

    @Test
    fun `cancelOrder should set status to CANCELLED and restore product stock`() {
        val order = Order(
            id = 1L,
            user = testUser,
            shippingAddress = "123 Main St",
            status = OrderStatus.PENDING,
            totalPrice = BigDecimal("50.00")
        )
        val orderItem = OrderItem(
            order = order,
            product = testProduct,
            quantity = 2,
            price = BigDecimal("25.00"),
            subtotal = BigDecimal("50.00")
        )
        order.items.add(orderItem)

        every { userRepository.findByEmail(testUser.email) } returns Optional.of(testUser)
        every { orderRepository.findByIdAndUser(1L, testUser) } returns order
        every { productRepository.save(testProduct) } returns testProduct
        every { orderRepository.save(order) } returns order

        orderService.cancelOrder(testUser.email, 1L)

        assertEquals(OrderStatus.CANCELLED, order.status)
        assertEquals(7, testProduct.stock)      // 5 + 2 restored
    }

    @Test
    fun `cancelOrder should throw ConflictException when order is not PENDING`() {
        val shippedOrder = Order(
            id = 1L,
            user = testUser,
            shippingAddress = "123 Main St",
            status = OrderStatus.SHIPPED,
            totalPrice = BigDecimal("50.00")
        )

        every { userRepository.findByEmail(testUser.email) } returns Optional.of(testUser)
        every { orderRepository.findByIdAndUser(1L, testUser) } returns shippedOrder

        assertThrows<ConflictException> {
            orderService.cancelOrder(testUser.email, 1L)
        }
    }

    @Test
    fun `cancelOrder should throw NotFoundException when order does not exist`() {
        every { userRepository.findByEmail(testUser.email) } returns Optional.of(testUser)
        every { orderRepository.findByIdAndUser(99L, testUser) } returns null

        assertThrows<NotFoundException> {
            orderService.cancelOrder(testUser.email, 99L)
        }
    }

    // -------------------------------------------------------------------------
    // updateOrderStatus (admin)
    // -------------------------------------------------------------------------

    @Test
    fun `updateOrderStatus should advance status along a valid transition`() {
        val order = Order(
            id = 1L,
            user = testUser,
            shippingAddress = "123 Main St",
            status = OrderStatus.PENDING,
            totalPrice = BigDecimal("50.00")
        )

        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { orderRepository.save(order) } returns order

        val result = orderService.updateOrderStatus(1L, UpdateOrderStatusRequest(status = "CONFIRMED"))

        assertEquals("CONFIRMED", result.status)
    }

    @Test
    fun `updateOrderStatus should throw BadRequestException for an invalid transition`() {
        val order = Order(
            id = 1L,
            user = testUser,
            shippingAddress = "123 Main St",
            status = OrderStatus.DELIVERED,
            totalPrice = BigDecimal("50.00")
        )

        every { orderRepository.findById(1L) } returns Optional.of(order)

        assertThrows<BadRequestException> {
            orderService.updateOrderStatus(1L, UpdateOrderStatusRequest(status = "PENDING"))
        }
    }
}