package com.ecommerce.backend.service.impl

import com.ecommerce.backend.exception.BadRequestException
import com.ecommerce.backend.exception.ConflictException
import com.ecommerce.backend.exception.NotFoundException
import com.ecommerce.backend.model.dto.request.PlaceOrderRequest
import com.ecommerce.backend.model.dto.request.UpdateOrderStatusRequest
import com.ecommerce.backend.model.dto.response.OrderItemResponse
import com.ecommerce.backend.model.dto.response.OrderResponse
import com.ecommerce.backend.model.entity.Order
import com.ecommerce.backend.model.entity.OrderItem
import com.ecommerce.backend.model.enums.OrderStatus
import com.ecommerce.backend.repository.OrderRepository
import com.ecommerce.backend.repository.ProductRepository
import com.ecommerce.backend.repository.UserRepository
import com.ecommerce.backend.service.CartService
import com.ecommerce.backend.service.NotificationService
import com.ecommerce.backend.service.OrderService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val cartService: CartService,
    private val notificationService: NotificationService
) : OrderService {

    @Transactional
    override fun placeOrder(email: String, request: PlaceOrderRequest): OrderResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { NotFoundException("User not found") }

        val cart = cartService.getCart(email)

        if (cart.items.isEmpty()) {
            throw BadRequestException("Cart is empty")
        }

        val order = Order(
            user = user,
            shippingAddress = request.shippingAddress,
            status = OrderStatus.PENDING
        )

        var totalPrice = BigDecimal.ZERO

        for (cartItem in cart.items) {
            val product = productRepository.findActiveByIdForUpdate(cartItem.productId)
                ?: throw NotFoundException("Product ${cartItem.productName} is no longer available")

            if (product.stock < cartItem.quantity) {
                throw ConflictException("Insufficient stock for ${product.name}. Available: ${product.stock}")
            }

            val subtotal = cartItem.price.multiply(BigDecimal(cartItem.quantity))
            totalPrice = totalPrice.add(subtotal)

            val orderItem = OrderItem(
                order = order,
                product = product,
                quantity = cartItem.quantity,
                price = cartItem.price,
                subtotal = subtotal
            )

            order.items.add(orderItem)

            product.stock -= cartItem.quantity
            productRepository.save(product)
        }

        order.totalPrice = totalPrice
        orderRepository.save(order)

        cartService.clearCart(email)

        notificationService.sendToUser(
            email = email,
            title = "Order Placed Successfully",
            message = "Your order #${order.id} has been placed and is being processed.",
            type = "ORDER"
        )

        return order.toResponse()
    }

    override fun getMyOrders(email: String): List<OrderResponse> {
        val user = userRepository.findByEmail(email)
            .orElseThrow { NotFoundException("User not found") }

        return orderRepository.findAllByUser(user).map { it.toResponse() }
    }

    override fun getOrderById(email: String, orderId: Long): OrderResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { NotFoundException("User not found") }

        val order = orderRepository.findByIdAndUser(orderId, user)
            ?: throw NotFoundException("Order not found")

        return order.toResponse()
    }

    @Transactional
    override fun cancelOrder(email: String, orderId: Long) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { NotFoundException("User not found") }

        val order = orderRepository.findByIdAndUser(orderId, user)
            ?: throw NotFoundException("Order not found")

        if (order.status != OrderStatus.PENDING) {
            throw ConflictException("Only PENDING orders can be cancelled")
        }

        for (item in order.items) {
            item.product.stock += item.quantity
            productRepository.save(item.product)
        }

        order.status = OrderStatus.CANCELLED
        order.updatedAt = LocalDateTime.now()
        orderRepository.save(order)

        notificationService.sendToUser(
            email = email,
            title = "Order Cancelled",
            message = "Your order #${order.id} has been cancelled.",
            type = "ORDER"
        )
    }

    @Transactional
    override fun updateOrderStatus(orderId: Long, request: UpdateOrderStatusRequest): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NotFoundException("Order not found") }

        val newStatus = try {
            OrderStatus.valueOf(request.status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid status. Valid values: ${OrderStatus.entries.joinToString()}")
        }

        val validTransitions = mapOf(
            OrderStatus.PENDING to listOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED to listOf(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED to listOf(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED to emptyList(),
            OrderStatus.CANCELLED to emptyList()
        )

        val allowed = validTransitions[order.status] ?: emptyList()
        if (newStatus !in allowed) {
            throw BadRequestException("Cannot transition from ${order.status} to $newStatus")
        }

        order.status = newStatus
        order.updatedAt = LocalDateTime.now()
        orderRepository.save(order)

        notificationService.sendToUser(
            email = order.user.email,
            title = "Order Status Updated",
            message = "Your order #${order.id} status has been updated to ${newStatus.name}.",
            type = "ORDER"
        )

        return order.toResponse()
    }

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