package com.ecommerce.backend.controller

import com.ecommerce.backend.config.RateLimitingFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
@Profile("test")
class TestRateLimiterConfig {

    /**
     * Replace the real RateLimitingFilter with a no-op version that doesn't limit anything
     */
    @Bean
    @Primary
    fun rateLimitingFilter(): RateLimitingFilter {
        return NoOpRateLimitingFilter()
    }
}

/**
 * A no-op filter that bypasses all rate limiting
 */
class NoOpRateLimitingFilter : RateLimitingFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Simply proceed without any rate limiting
        filterChain.doFilter(request, response)
    }
}