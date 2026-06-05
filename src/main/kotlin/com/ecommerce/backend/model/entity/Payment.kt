package com.ecommerce.backend.model.entity

import com.ecommerce.backend.model.enums.PaymentStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "payments")
class Payment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    var order: Order = Order(),

    @Column(nullable = false, unique = true)
    var stripePaymentIntentId: String = "",

    @Column(nullable = false)
    var amount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var currency: String = "usd",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column
    var stripeClientSecret: String? = null,

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)