package nomoredelay.server.collaboard.websocket.config

import nomoredelay.server.collaboard.websocket.handler.DocumentWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping

// TODO: Configure WebSocket endpoints
//  - Define WebSocket handlers mapping
//  - Setup path configurations
//  - Configure connection upgrade
@Configuration
class WebSocketConfig {
    @Bean
    fun registerWebSocketHandlers(documentWebSocketHandler: DocumentWebSocketHandler): HandlerMapping {
        // https://docs.spring.io/spring-framework/reference/web/webflux-websocket.html#when-to-use-websockets
        val map = mapOf("/ws/document" to DocumentWebSocketHandler())
        val order = -1 // before annotated controllers

        return SimpleUrlHandlerMapping(map, order)
    }
}

// TODO: Implement security integration
//  - Add authentication handling
//  - Implement authorization checks
//  - Add CSRF protection

// TODO: Configure timeouts and limits
//  - Set connection timeout
//  - Configure message size limits
//  - Set idle timeout

// TODO: Add monitoring and metrics
//  - Connection metrics
//  - Performance monitoring
//  - Error rate tracking
