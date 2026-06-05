package com.ecommerce.backend

import com.ecommerce.backend.model.entity.User
import com.ecommerce.backend.model.enums.Role
import com.ecommerce.backend.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class BaseTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    fun createTestUser(
        email: String = "test@example.com",
        password: String = "password123",
        role: Role = Role.CUSTOMER,
        isEmailVerified: Boolean = true,
        isEnabled: Boolean = true
    ): User {
        val user = User(
            firstName = "Test",
            lastName = "User",
            email = email,
            password = passwordEncoder.encode(password),
            role = role,
            isEmailVerified = isEmailVerified,
            isEnabled = isEnabled
        )
        return userRepository.save(user)
    }

    fun createAdminUser(
        email: String = "admin@example.com",
        password: String = "password123"
    ): User {
        return createTestUser(email = email, password = password, role = Role.ADMIN)
    }
}