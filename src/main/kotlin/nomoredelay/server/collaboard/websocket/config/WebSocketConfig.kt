package nomoredelay.server.collaboard.websocket.config

import nomoredelay.server.collaboard.websocket.handler.DocumentWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy
import java.util.HashMap

@Configuration
class WebSocketConfig {
    @Bean
    fun webSocketHandlerMapping(documentWebSocketHandler: DocumentWebSocketHandler): HandlerMapping {
        val map = HashMap<String, Any>()
        
        // 문서별 웹소켓 엔드포인트
        map["/ws/document/{docId}"] = documentWebSocketHandler
        
        val order = -1 // 어노테이션 컨트롤러보다 높은 우선순위
        val mapping = SimpleUrlHandlerMapping(map, order)
        
        // SimpleUrlHandlerMapping은 Spring 5에서는 properties 속성에 직접 접근할 수 없음
        // 대신 setCorsConfigurations 메서드 사용 (Spring WebFlux에서 CORS는 전역 설정으로 처리하는 것이 더 좋음)
        
        println("웹소켓 핸들러 매핑 설정 완료: $map")
        
        return mapping
    }
    
    @Bean
    fun handlerAdapter(webSocketService: WebSocketService): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter(webSocketService)
    }
    
    @Bean
    fun webSocketService(): WebSocketService {
        return HandshakeWebSocketService(ReactorNettyRequestUpgradeStrategy())
    }
}
