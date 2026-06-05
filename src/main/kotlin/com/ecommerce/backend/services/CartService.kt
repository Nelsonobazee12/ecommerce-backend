package com.ecommerce.backend.service

import com.ecommerce.backend.model.dto.request.AddToCartRequest
import com.ecommerce.backend.model.dto.request.UpdateCartRequest
import com.ecommerce.backend.model.dto.response.CartResponse

interface CartService {
    fun addToCart(email: String, request: AddToCartRequest): CartResponse
    fun getCart(email: String): CartResponse
    fun updateCart(email: String, request: UpdateCartRequest): CartResponse
    fun removeFromCart(email: String, productId: Long): CartResponse
    fun clearCart(email: String)
}