package nomoredelay.server.collaboard.router

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class TestRouter {
    @Bean
    fun testRoutes(): RouterFunction<ServerResponse> = router {
        GET("/test") { 
            ServerResponse.permanentRedirect(java.net.URI.create("/static/test.html")).build()
        }
    }
}
