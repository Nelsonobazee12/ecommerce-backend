package com.ecommerce.backend.repository

import com.ecommerce.backend.model.entity.Order
import com.ecommerce.backend.model.entity.User
import com.ecommerce.backend.model.enums.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal

interface OrderRepository : JpaRepository<Order, Long> {

    @Query("""
        SELECT o FROM Order o
        JOIN FETCH o.items i
        JOIN FETCH i.product
        WHERE o.user = :user
        ORDER BY o.createdAt DESC
    """)
    fun findAllByUser(user: User): List<Order>

    @Query("""
        SELECT o FROM Order o
        JOIN FETCH o.items i
        JOIN FETCH i.product
        WHERE o.id = :id AND o.user = :user
    """)
    fun findByIdAndUser(id: Long, user: User): Order?

    @Query("""
        SELECT o FROM Order o
        JOIN FETCH o.items i
        JOIN FETCH i.product
        ORDER BY o.createdAt DESC
    """)
    fun findAllOrders(): List<Order>

    fun countByStatus(status: OrderStatus): Long

    @Query("""
        SELECT COALESCE(SUM(o.totalPrice), 0)
        FROM Order o
        WHERE o.status != com.ecommerce.backend.model.enums.OrderStatus.CANCELLED
    """)
    fun getTotalRevenue(): BigDecimal

    @Query("""
        SELECT i.product.id, i.product.name, SUM(i.quantity), SUM(i.subtotal)
        FROM OrderItem i
        WHERE i.order.status != com.ecommerce.backend.model.enums.OrderStatus.CANCELLED
        GROUP BY i.product.id, i.product.name
        ORDER BY SUM(i.quantity) DESC
    """)
    fun getTopProducts(): List<Array<Any>>
}