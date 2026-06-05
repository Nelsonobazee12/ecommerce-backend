package com.ecommerce.backend.controller

import com.ecommerce.backend.model.dto.request.SendNotificationRequest
import com.ecommerce.backend.model.dto.response.ApiResponse
import com.ecommerce.backend.model.dto.response.NotificationResponse
import com.ecommerce.backend.service.NotificationService
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping("/subscribe", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(
        @AuthenticationPrincipal userDetails: UserDetails
    ): SseEmitter {
        return notificationService.subscribe(userDetails.username)
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    fun sendNotification(
        @Valid @RequestBody request: SendNotificationRequest
    ): ResponseEntity<ApiResponse<NotificationResponse>> {
        val notification = notificationService.sendNotification(request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Notification sent", data = notification)
        )
    }

    @GetMapping
    fun getMyNotifications(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<List<NotificationResponse>>> {
        val notifications = notificationService.getMyNotifications(userDetails.username)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Notifications retrieved", data = notifications)
        )
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Long>> {
        val count = notificationService.getUnreadCount(userDetails.username)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Unread count retrieved", data = count)
        )
    }

    @PutMapping("/mark-all-read")
    fun markAllAsRead(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Nothing>> {
        notificationService.markAllAsRead(userDetails.username)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "All notifications marked as read")
        )
    }
}