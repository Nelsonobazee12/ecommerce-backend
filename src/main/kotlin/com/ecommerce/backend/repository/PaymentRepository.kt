package com.ecommerce.backend.repository

import com.ecommerce.backend.model.entity.Order
import com.ecommerce.backend.model.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByOrder(order: Order): Optional<Payment>
    fun findByStripePaymentIntentId(paymentIntentId: String): Optional<Payment>
}