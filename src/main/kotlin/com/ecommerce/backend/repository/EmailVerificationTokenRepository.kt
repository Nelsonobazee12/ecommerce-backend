package com.ecommerce.backend.repository

import com.ecommerce.backend.model.entity.EmailVerificationToken
import com.ecommerce.backend.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationToken, Long> {
    fun findByToken(token: String): Optional<EmailVerificationToken>

    @Modifying
    @Query("UPDATE EmailVerificationToken e SET e.isUsed = true WHERE e.user = :user")
    fun invalidateAllUserTokens(user: User)
}