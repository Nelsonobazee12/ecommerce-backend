package com.ecommerce.backend.service

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
class SseEmitterRegistry {

    private val emitters = ConcurrentHashMap<String, MutableList<SseEmitter>>()

    fun addEmitter(email: String, emitter: SseEmitter) {
        emitters.getOrPut(email) { mutableListOf() }.add(emitter)
    }

    fun removeEmitter(email: String, emitter: SseEmitter) {
        emitters[email]?.remove(emitter)
    }

    fun getEmitters(email: String): List<SseEmitter> {
        return emitters[email] ?: emptyList()
    }

    fun sendToUser(email: String, eventName: String, data: Any) {
        val userEmitters = emitters[email] ?: return
        val deadEmitters = mutableListOf<SseEmitter>()

        userEmitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name(eventName)
                        .data(data)
                )
            } catch (e: Exception) {
                deadEmitters.add(emitter)
            }
        }

        deadEmitters.forEach { userEmitters.remove(it) }
    }
}