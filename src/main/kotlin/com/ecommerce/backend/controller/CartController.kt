package com.ecommerce.backend.controller

import com.ecommerce.backend.model.dto.request.AddToCartRequest
import com.ecommerce.backend.model.dto.request.UpdateCartRequest
import com.ecommerce.backend.model.dto.response.ApiResponse
import com.ecommerce.backend.model.dto.response.CartResponse
import com.ecommerce.backend.service.CartService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cart")
class CartController(
    private val cartService: CartService
) {

    @PostMapping("/add")
    fun addToCart(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: AddToCartRequest
    ): ResponseEntity<ApiResponse<CartResponse>> {
        val cart = cartService.addToCart(userDetails.username, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Item added to cart", data = cart)
        )
    }

    @GetMapping
    fun getCart(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<CartResponse>> {
        val cart = cartService.getCart(userDetails.username)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Cart retrieved successfully", data = cart)
        )
    }

    @PutMapping("/update")
    fun updateCart(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: UpdateCartRequest
    ): ResponseEntity<ApiResponse<CartResponse>> {
        val cart = cartService.updateCart(userDetails.username, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Cart updated successfully", data = cart)
        )
    }

    @DeleteMapping("/remove/{productId}")
    fun removeFromCart(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable productId: Long
    ): ResponseEntity<ApiResponse<CartResponse>> {
        val cart = cartService.removeFromCart(userDetails.username, productId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Item removed from cart", data = cart)
        )
    }

    @DeleteMapping("/clear")
    fun clearCart(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Nothing>> {
        cartService.clearCart(userDetails.username)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Cart cleared successfully")
        )
    }
}