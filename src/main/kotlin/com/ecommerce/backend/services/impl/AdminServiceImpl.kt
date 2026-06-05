package com.ecommerce.backend.service.impl

import com.ecommerce.backend.model.dto.request.UpdateUserRoleRequest
import com.ecommerce.backend.model.dto.request.UpdateUserStatusRequest
import com.ecommerce.backend.model.dto.response.*
import com.ecommerce.backend.model.entity.Order
import com.ecommerce.backend.model.enums.OrderStatus
import com.ecommerce.backend.model.enums.Role
import com.ecommerce.backend.repository.OrderRepository
import com.ecommerce.backend.repository.ProductRepository
import com.ecommerce.backend.repository.UserRepository
import com.ecommerce.backend.service.AdminService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class AdminServiceImpl(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) : AdminService {

    override fun getDashboard(): DashboardResponse {
        val totalUsers = userRepository.count()
        val totalOrders = orderRepository.count()
        val totalProducts = productRepository.count()
        val totalRevenue = orderRepository.getTotalRevenue()
        val pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING)
        val deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED)
        val cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED)

        val topProducts = orderRepository.getTopProducts().take(5).map {
            TopProductResponse(
                productId = (it[0] as Long),
                productName = (it[1] as String),
                totalSold = (it[2] as Long),
                revenue = (it[3] as BigDecimal)
            )
        }

        return DashboardResponse(
            totalUsers = totalUsers,
            totalOrders = totalOrders,
            totalProducts = totalProducts,
            totalRevenue = totalRevenue,
            pendingOrders = pendingOrders,
            deliveredOrders = deliveredOrders,
            cancelledOrders = cancelledOrders,
            topProducts = topProducts
        )
    }

    override fun getAllUsers(): List<UserProfileResponse> {
        return userRepository.findAll().map { it.toResponse() }
    }

    override fun getUserById(id: Long): UserProfileResponse {
        val user = userRepository.findById(id)
            .orElseThrow { RuntimeException("User not found") }
        return user.toResponse()
    }

    @Transactional
    override fun updateUserRole(id: Long, request: UpdateUserRoleRequest): UserProfileResponse {
        val user = userRepository.findById(id)
            .orElseThrow { RuntimeException("User not found") }

        val newRole = try {
            Role.valueOf(request.role.uppercase())
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("Invalid role. Valid values: ${Role.entries.joinToString()}")
        }

        user.role = newRole
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)
        return user.toResponse()
    }

    @Transactional
    override fun updateUserStatus(id: Long, request: UpdateUserStatusRequest): UserProfileResponse {
        val user = userRepository.findById(id)
            .orElseThrow { RuntimeException("User not found") }

        user.isEnabled = request.isEnabled
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)
        return user.toResponse()
    }

    override fun getAllOrders(): List<OrderResponse> {
        return orderRepository.findAllOrders().map { it.toResponse() }
    }

    private fun com.ecommerce.backend.model.entity.User.toResponse() = UserProfileResponse(
        id = id,
        firstName = firstName,
        lastName = lastName,
        email = email,
        role = role.name,
        isEmailVerified = isEmailVerified,
        createdAt = createdAt
    )

    private fun Order.toResponse() = OrderResponse(
        id = id,
        status = status.name,
        totalPrice = totalPrice,
        shippingAddress = shippingAddress,
        items = items.map {
            OrderItemResponse(
                productId = it.product.id,
                productName = it.product.name,
                productImage = it.product.imageUrl,
                price = it.price,
                quantity = it.quantity,
                subtotal = it.subtotal
            )
        },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}