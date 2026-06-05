package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.request.PlaceOrderRequest
import com.ecommerce.backend.model.dto.request.UpdateOrderStatusRequest
import com.ecommerce.backend.model.dto.response.OrderResponse

interface OrderService {
    fun placeOrder(email: String, request: PlaceOrderRequest): OrderResponse
    fun getMyOrders(email: String): List<OrderResponse>
    fun getOrderById(email: String, orderId: Long): OrderResponse
    fun cancelOrder(email: String, orderId: Long)
    fun updateOrderStatus(orderId: Long, request: UpdateOrderStatusRequest): OrderResponse
}