package com.ecommerce.backend.service.impl

import com.ecommerce.backend.exception.ConflictException
import com.ecommerce.backend.exception.NotFoundException
import com.ecommerce.backend.model.dto.request.AddToCartRequest
import com.ecommerce.backend.model.dto.request.UpdateCartRequest
import com.ecommerce.backend.model.dto.response.CartItemResponse
import com.ecommerce.backend.model.dto.response.CartResponse
import com.ecommerce.backend.model.redis.CartItem
import com.ecommerce.backend.repository.ProductRepository
import com.ecommerce.backend.service.CartService
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@Service
class CartServiceImpl(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val productRepository: ProductRepository
) : CartService {

    companion object {
        private const val CART_PREFIX = "cart:"
        private const val CART_TTL = 7L
    }

    private fun getCartKey(email: String) = "$CART_PREFIX$email"

    override fun addToCart(email: String, request: AddToCartRequest): CartResponse {
        val product = productRepository.findActiveById(request.productId)
            ?: throw NotFoundException("Product not found")

        if (product.stock < request.quantity) {
            throw ConflictException("Insufficient stock. Available: ${product.stock}")
        }

        val cartKey = getCartKey(email)
        val cartItems = getCartItems(email).toMutableMap()

        val existingItem = cartItems[request.productId.toString()]
        if (existingItem != null) {
            val newQuantity = existingItem.quantity + request.quantity
            if (product.stock < newQuantity) {
                throw ConflictException("Insufficient stock. Available: ${product.stock}")
            }
            existingItem.quantity = newQuantity
            cartItems[request.productId.toString()] = existingItem
        } else {
            cartItems[request.productId.toString()] = CartItem(
                productId = product.id,
                productName = product.name,
                productImage = product.imageUrl,
                price = product.price,
                quantity = request.quantity
            )
        }

        saveCartItems(cartKey, cartItems)
        return buildCartResponse(cartItems.values.toList())
    }

    override fun getCart(email: String): CartResponse {
        val cartItems = getCartItems(email)
        return buildCartResponse(cartItems.values.toList())
    }

    override fun updateCart(email: String, request: UpdateCartRequest): CartResponse {
        val product = productRepository.findActiveById(request.productId)
            ?: throw NotFoundException("Product not found")

        if (product.stock < request.quantity) {
            throw ConflictException("Insufficient stock. Available: ${product.stock}")
        }

        val cartKey = getCartKey(email)
        val cartItems = getCartItems(email).toMutableMap()

        val existingItem = cartItems[request.productId.toString()]
            ?: throw NotFoundException("Item not found in cart")

        existingItem.quantity = request.quantity
        cartItems[request.productId.toString()] = existingItem

        saveCartItems(cartKey, cartItems)
        return buildCartResponse(cartItems.values.toList())
    }

    override fun removeFromCart(email: String, productId: Long): CartResponse {
        val cartKey = getCartKey(email)
        val cartItems = getCartItems(email).toMutableMap()

        cartItems.remove(productId.toString())
            ?: throw NotFoundException("Item not found in cart")

        saveCartItems(cartKey, cartItems)
        return buildCartResponse(cartItems.values.toList())
    }

    override fun clearCart(email: String) {
        redisTemplate.delete(getCartKey(email))
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCartItems(email: String): Map<String, CartItem> {
        val cartKey = getCartKey(email)
        val entries = redisTemplate.opsForHash<String, CartItem>().entries(cartKey)
        return entries as Map<String, CartItem>
    }

    private fun saveCartItems(cartKey: String, cartItems: Map<String, CartItem>) {
        redisTemplate.delete(cartKey)
        if (cartItems.isNotEmpty()) {
            redisTemplate.opsForHash<String, CartItem>().putAll(cartKey, cartItems)
            redisTemplate.expire(cartKey, CART_TTL, TimeUnit.DAYS)
        }
    }

    private fun buildCartResponse(items: List<CartItem>): CartResponse {
        val cartItems = items.map {
            CartItemResponse(
                productId = it.productId,
                productName = it.productName,
                productImage = it.productImage,
                price = it.price,
                quantity = it.quantity,
                subtotal = it.price.multiply(BigDecimal(it.quantity))
            )
        }

        val totalPrice = cartItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subtotal) }
        val totalItems = cartItems.sumOf { it.quantity }

        return CartResponse(
            items = cartItems,
            totalItems = totalItems,
            totalPrice = totalPrice
        )
    }
}