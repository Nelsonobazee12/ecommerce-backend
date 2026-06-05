package com.ecommerce.backend.exception

import org.springframework.http.HttpStatus

sealed class AppException(message: String) : RuntimeException(message) {
    abstract val status: HttpStatus
}

class NotFoundException(message: String) : AppException(message) {
    override val status = HttpStatus.NOT_FOUND
}

class ConflictException(message: String) : AppException(message) {
    override val status = HttpStatus.CONFLICT
}

class AuthException(message: String) : AppException(message) {
    override val status = HttpStatus.UNAUTHORIZED
}

class BadRequestException(message: String) : AppException(message) {
    override val status = HttpStatus.BAD_REQUEST
}