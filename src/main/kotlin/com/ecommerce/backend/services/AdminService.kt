package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.request.UpdateUserRoleRequest
import com.ecommerce.backend.model.dto.request.UpdateUserStatusRequest
import com.ecommerce.backend.model.dto.response.DashboardResponse
import com.ecommerce.backend.model.dto.response.OrderResponse
import com.ecommerce.backend.model.dto.response.UserProfileResponse

interface AdminService {
    fun getDashboard(): DashboardResponse
    fun getAllUsers(): List<UserProfileResponse>
    fun getUserById(id: Long): UserProfileResponse
    fun updateUserRole(id: Long, request: UpdateUserRoleRequest): UserProfileResponse
    fun updateUserStatus(id: Long, request: UpdateUserStatusRequest): UserProfileResponse
    fun getAllOrders(): List<OrderResponse>
}