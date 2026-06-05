package com.ecommerce.backend.service.impl

import com.ecommerce.backend.model.dto.request.SendNotificationRequest
import com.ecommerce.backend.model.dto.response.NotificationResponse
import com.ecommerce.backend.model.entity.Notification
import com.ecommerce.backend.repository.NotificationRepository
import com.ecommerce.backend.repository.UserRepository
import com.ecommerce.backend.service.NotificationService
import com.ecommerce.backend.service.SseEmitterRegistry
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.Executors

@Service
class NotificationServiceImpl(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val sseEmitterRegistry: SseEmitterRegistry,
    private val objectMapper: ObjectMapper
) : NotificationService {

    private val executor = Executors.newSingleThreadScheduledExecutor()

    override fun subscribe(email: String): SseEmitter {
        val emitter = SseEmitter(Long.MAX_VALUE)

        sseEmitterRegistry.addEmitter(email, emitter)

        emitter.onCompletion {
            sseEmitterRegistry.removeEmitter(email, emitter)
        }

        emitter.onTimeout {
            sseEmitterRegistry.removeEmitter(email, emitter)
            emitter.complete()
        }

        emitter.onError {
            sseEmitterRegistry.removeEmitter(email, emitter)
        }

        // Send initial connection event
        executor.execute {
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("connected")
                        .data("Connected to notification stream")
                )
            } catch (e: Exception) {
                sseEmitterRegistry.removeEmitter(email, emitter)
            }
        }

        return emitter
    }

    @Transactional
    override fun sendNotification(request: SendNotificationRequest): NotificationResponse {
        val user = userRepository.findById(request.userId)
            .orElseThrow { RuntimeException("User not found") }

        val notification = Notification(
            user = user,
            title = request.title,
            message = request.message,
            type = request.type
        )

        notificationRepository.save(notification)

        // Push via SSE
        val response = notification.toResponse()
        sseEmitterRegistry.sendToUser(
            user.email,
            "notification",
            objectMapper.writeValueAsString(response)
        )

        return response
    }

    override fun getMyNotifications(email: String): List<NotificationResponse> {
        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("User not found") }

        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
            .map { it.toResponse() }
    }

    override fun getUnreadCount(email: String): Long {
        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("User not found") }

        return notificationRepository.countByUserAndIsReadFalse(user)
    }

    @Transactional
    override fun markAllAsRead(email: String) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("User not found") }

        notificationRepository.markAllAsRead(user)
    }

    override fun sendToUser(email: String, title: String, message: String, type: String) {
        val user = userRepository.findByEmail(email)
            .orElse(null) ?: return

        val notification = Notification(
            user = user,
            title = title,
            message = message,
            type = type
        )

        notificationRepository.save(notification)

        val response = notification.toResponse()
        sseEmitterRegistry.sendToUser(
            email,
            "notification",
            objectMapper.writeValueAsString(response)
        )
    }

    private fun Notification.toResponse() = NotificationResponse(
        id = id,
        title = title,
        message = message,
        isRead = isRead,
        type = type,
        createdAt = createdAt
    )
}