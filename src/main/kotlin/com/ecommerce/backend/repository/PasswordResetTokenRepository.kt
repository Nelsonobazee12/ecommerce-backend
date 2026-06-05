package com.ecommerce.backend.repository

import com.ecommerce.backend.model.entity.PasswordResetToken
import com.ecommerce.backend.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByToken(token: String): Optional<PasswordResetToken>

    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true WHERE p.user = :user")
    fun invalidateAllUserTokens(user: User)
}