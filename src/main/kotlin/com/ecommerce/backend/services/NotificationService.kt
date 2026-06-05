package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.request.SendNotificationRequest
import com.ecommerce.backend.model.dto.response.NotificationResponse
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface NotificationService {
    fun subscribe(email: String): SseEmitter
    fun sendNotification(request: SendNotificationRequest): NotificationResponse
    fun getMyNotifications(email: String): List<NotificationResponse>
    fun getUnreadCount(email: String): Long
    fun markAllAsRead(email: String)
    fun sendToUser(email: String, title: String, message: String, type: String)
}