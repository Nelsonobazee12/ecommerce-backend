package com.ecommerce.backend.repository

import com.ecommerce.backend.model.entity.Notification
import com.ecommerce.backend.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUserOrderByCreatedAtDesc(user: User): List<Notification>
    fun countByUserAndIsReadFalse(user: User): Long

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user")
    fun markAllAsRead(user: User)
}