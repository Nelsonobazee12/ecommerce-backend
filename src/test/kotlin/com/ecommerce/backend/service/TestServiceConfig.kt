package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.response.ImageUploadResponse
import com.ecommerce.backend.model.dto.response.PaymentResponse
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Test configuration that replaces external service beans with mocks.
 * Prevents real HTTP calls to Stripe and Cloudinary during tests.
 *
 * Place at:
 *   src/test/kotlin/com/ecommerce/backend/service/TestServiceConfig.kt
 *
 * Auto-picked up by @SpringBootTest when in the test classpath.
 * For per-test control, use @MockkBean on individual test classes instead.
 */
@TestConfiguration
class TestServiceConfig {

    /**
     * Mocks PaymentService — returns a fake PaymentResponse for any
     * createPaymentIntent or confirmPayment call, preventing real Stripe API hits.
     */
    @Bean
    @Primary
    fun paymentService(): PaymentService {
        val mock = mockk<PaymentService>(relaxed = true)

        every { mock.createPaymentIntent(any(), any()) } returns PaymentResponse(
            id = 1L,
            orderId = 1L,
            stripePaymentIntentId = "pi_test_dummy_id",
            amount = BigDecimal("1500.00"),
            currency = "usd",
            status = "PENDING",
            clientSecret = "pi_test_dummy_client_secret",
            createdAt = LocalDateTime.now()
        )

        every { mock.confirmPayment(any(), any()) } returns PaymentResponse(
            id = 1L,
            orderId = 1L,
            stripePaymentIntentId = "pi_test_dummy_id",
            amount = BigDecimal("1500.00"),
            currency = "usd",
            status = "SUCCEEDED",
            clientSecret = "pi_test_dummy_client_secret",
            createdAt = LocalDateTime.now()
        )

        return mock
    }

    /**
     * Mocks CloudinaryService — returns a fake ImageUploadResponse,
     * preventing real Cloudinary uploads during tests.
     */
    @Bean
    @Primary
    fun cloudinaryService(): CloudinaryService {
        val mock = mockk<CloudinaryService>(relaxed = true)

        every { mock.uploadImage(any(), any()) } returns ImageUploadResponse(
            url = "https://res.cloudinary.com/test/image/upload/dummy.jpg",
            publicId = "ecommerce/test/dummy",
            format = "jpg",
            width = 800,
            height = 600
        )

        every { mock.deleteImage(any()) } returns Unit

        return mock
    }

        @Bean
        @Primary
        fun emailService(): EmailService = mockk(relaxed = true)

}