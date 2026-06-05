package com.ecommerce.backend.controller

import com.ecommerce.backend.model.dto.request.PlaceOrderRequest
import com.ecommerce.backend.model.dto.request.UpdateOrderStatusRequest
import com.ecommerce.backend.model.dto.response.ApiResponse
import com.ecommerce.backend.model.dto.response.OrderResponse
import com.ecommerce.backend.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping("/orders")
    fun placeOrder(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: PlaceOrderRequest
    ): ResponseEntity<ApiResponse<OrderResponse>> {
        val order = orderService.placeOrder(userDetails.username, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Order placed successfully", data = order)
        )
    }

    @GetMapping("/orders")
    fun getMyOrders(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<OrderResponse>>> {
        val orders = orderService.getMyOrders(userDetails.username)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Orders retrieved successfully", data = orders)
        )
    }

    @GetMapping("/orders/{id}")
    fun getOrderById(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<OrderResponse>> {
        val order = orderService.getOrderById(userDetails.username, id)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Order retrieved successfully", data = order)
        )
    }

    @DeleteMapping("/orders/{id}/cancel")
    fun cancelOrder(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing>> {
        orderService.cancelOrder(userDetails.username, id)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Order cancelled successfully")
        )
    }

    @PutMapping("/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateOrderStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateOrderStatusRequest
    ): ResponseEntity<ApiResponse<OrderResponse>> {
        val order = orderService.updateOrderStatus(id, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Order status updated successfully", data = order)
        )
    }
}