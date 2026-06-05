package com.ecommerce.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@ConditionalOnProperty(name = ["rate-limiting.enabled"], havingValue = "true", matchIfMissing = true)
@Component
class RateLimitingFilter : OncePerRequestFilter() {

    private val buckets = ConcurrentHashMap<String, Bucket>()
    private val objectMapper = ObjectMapper()

    private val rateLimitedPaths = listOf(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/auth/verify-email"
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        if (rateLimitedPaths.none { path.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val ipAddress = getClientIp(request)
        val bucket = buckets.computeIfAbsent("$ipAddress:$path") { createBucket() }

        val probe = bucket.tryConsumeAndReturnRemaining(1)

        if (probe.isConsumed) {
            response.addHeader("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
            filterChain.doFilter(request, response)
        } else {
            val waitSeconds = maxOf(1L, probe.nanosToWaitForRefill / 1_000_000_000)
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", waitSeconds.toString())

            val errorResponse = mapOf(
                "success" to false,
                "message" to "Too many requests. Please try again in $waitSeconds seconds.",
                "data" to null
            )

            response.writer.write(objectMapper.writeValueAsString(errorResponse))
        }
    }

    private fun createBucket(): Bucket {
        val limit = Bandwidth.builder()
            .capacity(5)
            .refillGreedy(5, Duration.ofMinutes(1))
            .build()

        return Bucket.builder()
            .addLimit(limit)
            .build()
    }

    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrEmpty()) {
            xForwardedFor.split(",")[0].trim()
        } else {
            request.remoteAddr
        }
    }
}