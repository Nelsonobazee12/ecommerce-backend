package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.request.CreatePaymentIntentRequest
import com.ecommerce.backend.model.dto.response.PaymentResponse

interface PaymentService {
    fun createPaymentIntent(email: String, request: CreatePaymentIntentRequest): PaymentResponse
    fun confirmPayment(email: String, orderId: Long): PaymentResponse
    fun getPaymentByOrderId(email: String, orderId: Long): PaymentResponse
    fun handleWebhook(payload: String, sigHeader: String)
}