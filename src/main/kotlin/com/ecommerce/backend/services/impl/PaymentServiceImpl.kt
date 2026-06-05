package com.ecommerce.backend.service.impl

import com.ecommerce.backend.exception.AuthException
import com.ecommerce.backend.exception.ConflictException
import com.ecommerce.backend.exception.NotFoundException
import com.ecommerce.backend.model.dto.request.CreatePaymentIntentRequest
import com.ecommerce.backend.model.dto.response.PaymentResponse
import com.ecommerce.backend.model.entity.Payment
import com.ecommerce.backend.model.enums.OrderStatus
import com.ecommerce.backend.model.enums.PaymentStatus
import com.ecommerce.backend.repository.OrderRepository
import com.ecommerce.backend.repository.PaymentRepository
import com.ecommerce.backend.repository.UserRepository
import com.ecommerce.backend.service.NotificationService
import com.ecommerce.backend.service.PaymentService
import com.stripe.model.PaymentIntent
import com.stripe.model.Event
import com.stripe.net.Webhook
import com.stripe.param.PaymentIntentCreateParams
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class PaymentServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) : PaymentService {

    @Value("\${stripe.webhook-secret}")
    private lateinit var webhookSecret: String

    @Transactional
    override fun createPaymentIntent(email: String, request: CreatePaymentIntentRequest): PaymentResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { NotFoundException("User not found") }

        val order = orderRepository.findByIdAndUser(request.orderId, user)
            ?: throw NotFoundException("Order not found")

        if (order.status == OrderStatus.CANCELLED) {
            throw ConflictException("Cannot pay for a cancelled order")
        }

        val existingPayment = paymentRepository.findByOrder(order)
        if (existingPayment.isPresent) {
            val payment = existingPayment.get()
            if (payment.status == PaymentStatus.SUCCEEDED) {
                throw ConflictException("Order already paid")
            }
            return payment.toResponse()
        }

        val params = PaymentIntentCreateParams.builder()
            .setAmount((order.totalPrice * BigDecimal(100)).toLong())
            .setCurrency("usd")
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                    .build()
            )
            .putMetadata("orderId", order.id.toString())
            .putMetadata("userEmail", email)
            .build()

        val paymentIntent = PaymentIntent.create(params)

        val payment = Payment(
            order = order,
            stripePaymentIntentId = paymentIntent.id,
            amount = order.totalPrice,
            currency = "usd",
            status = PaymentStatus.PENDING,
            stripeClientSecret = paymentIntent.clientSecret
        )

        paymentRepository.save(payment)
        return payment.toResponse()
    }

    @Transactional
    override fun confirmPayment(email: String, orderId: Long): PaymentResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { NotFoundException("User not found") }

        val order = orderRepository.findByIdAndUser(orderId, user)
            ?: throw NotFoundException("Order not found")

        val payment = paymentRepository.findByOrder(order)
            .orElseThrow { NotFoundException("Payment not found for this order") }

        val paymentIntent = PaymentIntent.retrieve(payment.stripePaymentIntentId)

        when (paymentIntent.status) {
            "succeeded" -> {
                payment.status = PaymentStatus.SUCCEEDED
                payment.updatedAt = LocalDateTime.now()
                order.status = OrderStatus.CONFIRMED
                orderRepository.save(order)

                notificationService.sendToUser(
                    email = email,
                    title = "Payment Successful",
                    message = "Your payment of $${payment.amount} for order #${order.id} was successful.",
                    type = "PAYMENT"
                )
            }
            "canceled" -> {
                payment.status = PaymentStatus.CANCELLED
                payment.updatedAt = LocalDateTime.now()
            }
            "payment_failed" -> {
                payment.status = PaymentStatus.FAILED
                payment.updatedAt = LocalDateTime.now()

                notificationService.sendToUser(
                    email = email,
                    title = "Payment Failed",
                    message = "Your payment for order #${order.id} failed. Please try again.",
                    type = "PAYMENT"
                )
            }
        }

        paymentRepository.save(payment)
        return payment.toResponse()
    }

    override fun getPaymentByOrderId(email: String, orderId: Long): PaymentResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { NotFoundException("User not found") }

        val order = orderRepository.findByIdAndUser(orderId, user)
            ?: throw NotFoundException("Order not found")

        val payment = paymentRepository.findByOrder(order)
            .orElseThrow { NotFoundException("Payment not found for this order") }

        return payment.toResponse()
    }

    @Transactional
    override fun handleWebhook(payload: String, sigHeader: String) {
        val event: Event = try {
            Webhook.constructEvent(payload, sigHeader, webhookSecret)
        } catch (e: Exception) {
            throw AuthException("Invalid webhook signature")
        }

        when (event.type) {
            "payment_intent.succeeded" -> {
                val paymentIntent = event.dataObjectDeserializer
                    .deserializeUnsafe() as PaymentIntent

                val payment = paymentRepository
                    .findByStripePaymentIntentId(paymentIntent.id)
                    .orElse(null) ?: return

                payment.status = PaymentStatus.SUCCEEDED
                payment.updatedAt = LocalDateTime.now()
                payment.order.status = OrderStatus.CONFIRMED
                orderRepository.save(payment.order)
                paymentRepository.save(payment)

                notificationService.sendToUser(
                    email = payment.order.user.email,
                    title = "Payment Successful",
                    message = "Your payment for order #${payment.order.id} was successful.",
                    type = "PAYMENT"
                )
            }

            "payment_intent.payment_failed" -> {
                val paymentIntent = event.dataObjectDeserializer
                    .deserializeUnsafe() as PaymentIntent

                val payment = paymentRepository
                    .findByStripePaymentIntentId(paymentIntent.id)
                    .orElse(null) ?: return

                payment.status = PaymentStatus.FAILED
                payment.updatedAt = LocalDateTime.now()
                paymentRepository.save(payment)

                notificationService.sendToUser(
                    email = payment.order.user.email,
                    title = "Payment Failed",
                    message = "Your payment for order #${payment.order.id} failed.",
                    type = "PAYMENT"
                )
            }
        }
    }

    private fun Payment.toResponse() = PaymentResponse(
        id = id,
        orderId = order.id,
        stripePaymentIntentId = stripePaymentIntentId,
        amount = amount,
        currency = currency,
        status = status.name,
        clientSecret = stripeClientSecret,
        createdAt = createdAt
    )
}