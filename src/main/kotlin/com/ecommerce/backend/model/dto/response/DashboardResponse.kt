package com.ecommerce.backend.model.dto.response

import java.math.BigDecimal

data class DashboardResponse(
    val totalUsers: Long,
    val totalOrders: Long,
    val totalProducts: Long,
    val totalRevenue: BigDecimal,
    val pendingOrders: Long,
    val deliveredOrders: Long,
    val cancelledOrders: Long,
    val topProducts: List<TopProductResponse>
)

data class TopProductResponse(
    val productId: Long,
    val productName: String,
    val totalSold: Long,
    val revenue: BigDecimal
)