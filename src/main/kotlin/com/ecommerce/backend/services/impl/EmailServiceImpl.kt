package com.ecommerce.backend.service.impl

import com.ecommerce.backend.service.EmailService
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailServiceImpl(
    private val mailSender: JavaMailSender
) : EmailService {

    override fun sendVerificationEmail(email: String, firstName: String, token: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)

        helper.setTo(email)
        helper.setSubject("Verify your email address")
        helper.setText(
            """
            <html>
                <body>
                    <h2>Hi $firstName,</h2>
                    <p>Thanks for registering. Please verify your email by clicking the link below:</p>
                    <a href="http://localhost:8080/api/auth/verify-email?token=$token">
                        Verify Email
                    </a>
                    <p>This link expires in 24 hours.</p>
                </body>
            </html>
            """.trimIndent(),
            true
        )

        mailSender.send(message)
    }

    override fun sendPasswordResetEmail(email: String, firstName: String, token: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)

        helper.setTo(email)
        helper.setSubject("Reset your password")
        helper.setText(
            """
        <html>
            <body>
                <h2>Hi $firstName,</h2>
                <p>We received a request to reset your password.</p>
                <p>Click the link below to reset it. This link expires in 15 minutes:</p>
                <a href="http://localhost:8080/api/auth/reset-password-page?token=$token">
                    Reset Password
                </a>
                <p>If you didn't request this, ignore this email.</p>
            </body>
        </html>
        """.trimIndent(),
            true
        )

        mailSender.send(message)
    }
}