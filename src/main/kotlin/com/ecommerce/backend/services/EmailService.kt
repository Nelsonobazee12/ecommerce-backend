package com.ecommerce.backend.service

interface EmailService {
    fun sendVerificationEmail(email: String, firstName: String, token: String)
    fun sendPasswordResetEmail(email: String, firstName: String, token: String)
}