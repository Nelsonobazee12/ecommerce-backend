package com.ecommerce.backend.service

import com.ecommerce.backend.exception.ConflictException
import com.ecommerce.backend.exception.NotFoundException
import com.ecommerce.backend.model.dto.request.CreatePaymentIntentRequest
import com.ecommerce.backend.model.entity.Order
import com.ecommerce.backend.model.entity.Payment
import com.ecommerce.backend.model.entity.User
import com.ecommerce.backend.model.enums.OrderStatus
import com.ecommerce.backend.model.enums.PaymentStatus
import com.ecommerce.backend.model.enums.Role
import com.ecommerce.backend.repository.OrderRepository
import com.ecommerce.backend.repository.PaymentRepository
import com.ecommerce.backend.repository.UserRepository
import com.ecommerce.backend.service.impl.PaymentServiceImpl
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Optional

class PaymentServiceTest {

    private lateinit var paymentService: PaymentServiceImpl
    private val paymentRepository = mockk<PaymentRepository>()
    private val orderRepository = mockk<OrderRepository>()
    private val userRepository = mockk<UserRepository>()
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

    private val testOrder = Order(
        id = 1L,
        user = testUser,
        shippingAddress = "123 Main St",
        status = OrderStatus.PENDING,
        totalPrice = BigDecimal("100.00")
    )

    @BeforeEach
    fun setup() {
        paymentService = PaymentServiceImpl(
            paymentRepository, orderRepository, userRepository, notificationService
        )
        // inject @Value field via reflection since there's no Spring context
        val field = PaymentServiceImpl::class.java.getDeclaredField("webhookSecret")
        field.isAccessible = true
        field.set(paymentService, "whsec_test")
    }

    @Test
    fun `createPaymentIntent should throw ConflictException when order is already paid`() {
        // toResponse() is a private extension inside PaymentServiceImpl so we
        // cannot call it here — the exception is thrown before it is ever reached
        val paidPayment = mockk<Payment>()
        every { paidPayment.status } returns PaymentStatus.SUCCEEDED

        every { userRepository.findByEmail(testUser.email) } returns Optional.of(testUser)
        every { orderRepository.findByIdAndUser(1L, testUser) } returns testOrder
        every { paymentRepository.findByOrder(testOrder) } returns Optional.of(paidPayment)

        assertThrows<ConflictException> {
            paymentService.createPaymentIntent(testUser.email, CreatePaymentIntentRequest(orderId = 1L))
        }
    }

    @Test
    fun `createPaymentIntent should throw ConflictException for a cancelled order`() {
        // Order is a regular class (not data class), so build a new instance directly
        val cancelledOrder = Order(
            id = 1L,
            user = testUser,
            shippingAddress = "123 Main St",
            status = OrderStatus.CANCELLED,
            totalPrice = BigDecimal("100.00")
        )

        every { userRepository.findByEmail(testUser.email) } returns Optional.of(testUser)
        every { orderRepository.findByIdAndUser(1L, testUser) } returns cancelledOrder
        every { paymentRepository.findByOrder(cancelledOrder) } returns Optional.empty()

        assertThrows<ConflictException> {
            paymentService.createPaymentIntent(testUser.email, CreatePaymentIntentRequest(orderId = 1L))
        }
    }

    @Test
    fun `getPaymentByOrderId should throw NotFoundException when payment does not exist`() {
        every { userRepository.findByEmail(testUser.email) } returns Optional.of(testUser)
        every { orderRepository.findByIdAndUser(1L, testUser) } returns testOrder
        every { paymentRepository.findByOrder(testOrder) } returns Optional.empty()

        assertThrows<NotFoundException> {
            paymentService.getPaymentByOrderId(testUser.email, 1L)
        }
    }

    @Test
    fun `getPaymentByOrderId should throw NotFoundException when order does not exist`() {
        every { userRepository.findByEmail(testUser.email) } returns Optional.of(testUser)
        every { orderRepository.findByIdAndUser(99L, testUser) } returns null

        assertThrows<NotFoundException> {
            paymentService.getPaymentByOrderId(testUser.email, 99L)
        }
    }

    @Test
    fun `createPaymentIntent should throw NotFoundException when user does not exist`() {
        every { userRepository.findByEmail("ghost@example.com") } returns Optional.empty()

        assertThrows<NotFoundException> {
            paymentService.createPaymentIntent("ghost@example.com", CreatePaymentIntentRequest(orderId = 1L))
        }
    }
}