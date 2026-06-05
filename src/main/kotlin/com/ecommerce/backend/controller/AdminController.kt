package com.ecommerce.backend.controller

import com.ecommerce.backend.model.dto.request.UpdateUserRoleRequest
import com.ecommerce.backend.model.dto.request.UpdateUserStatusRequest
import com.ecommerce.backend.model.dto.response.*
import com.ecommerce.backend.service.AdminService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminService: AdminService
) {

    @GetMapping("/dashboard")
    fun getDashboard(): ResponseEntity<ApiResponse<DashboardResponse>> {
        val dashboard = adminService.getDashboard()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Dashboard retrieved successfully", data = dashboard)
        )
    }

    @GetMapping("/users")
    fun getAllUsers(): ResponseEntity<ApiResponse<List<UserProfileResponse>>> {
        val users = adminService.getAllUsers()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Users retrieved successfully", data = users)
        )
    }

    @GetMapping("/users/{id}")
    fun getUserById(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<UserProfileResponse>> {
        val user = adminService.getUserById(id)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "User retrieved successfully", data = user)
        )
    }

    @PutMapping("/users/{id}/role")
    fun updateUserRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserRoleRequest
    ): ResponseEntity<ApiResponse<UserProfileResponse>> {
        val user = adminService.updateUserRole(id, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "User role updated successfully", data = user)
        )
    }

    @PutMapping("/users/{id}/status")
    fun updateUserStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserStatusRequest
    ): ResponseEntity<ApiResponse<UserProfileResponse>> {
        val user = adminService.updateUserStatus(id, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "User status updated successfully", data = user)
        )
    }

    @GetMapping("/orders")
    fun getAllOrders(): ResponseEntity<ApiResponse<List<OrderResponse>>> {
        val orders = adminService.getAllOrders()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Orders retrieved successfully", data = orders)
        )
    }
}