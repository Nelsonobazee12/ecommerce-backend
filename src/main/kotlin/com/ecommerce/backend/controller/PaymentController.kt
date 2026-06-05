package com.ecommerce.backend.controller

import com.ecommerce.backend.model.dto.request.CreatePaymentIntentRequest
import com.ecommerce.backend.model.dto.response.ApiResponse
import com.ecommerce.backend.model.dto.response.PaymentResponse
import com.ecommerce.backend.service.PaymentService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val paymentService: PaymentService
) {

    @PostMapping("/create-intent")
    fun createPaymentIntent(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: CreatePaymentIntentRequest
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        val payment = paymentService.createPaymentIntent(userDetails.username, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Payment intent created", data = payment)
        )
    }

    @PostMapping("/confirm/{orderId}")
    fun confirmPayment(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable orderId: Long
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        val payment = paymentService.confirmPayment(userDetails.username, orderId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Payment status updated", data = payment)
        )
    }

    @GetMapping("/order/{orderId}")
    fun getPaymentByOrderId(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable orderId: Long
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        val payment = paymentService.getPaymentByOrderId(userDetails.username, orderId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Payment retrieved", data = payment)
        )
    }

    @PostMapping("/webhook")
    fun handleWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") sigHeader: String
    ): ResponseEntity<String> {
        paymentService.handleWebhook(payload, sigHeader)
        return ResponseEntity.ok("Webhook processed")
    }
}