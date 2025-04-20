package nomoredelay.server.collaboard.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class StaticResourceConfig {
    @Bean
    fun staticResourceRouter(): RouterFunction<ServerResponse> = router {
        // HTML 파일을 /test와 /test2 경로로 제공
        GET("/test") {
            ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .bodyValue(ClassPathResource("test2.html"))
        }
    }
}
