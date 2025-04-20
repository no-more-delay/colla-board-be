package nomoredelay.server.collaboard.router

import nomoredelay.server.collaboard.handler.DocumentHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class DocumentRouter {
    @Bean
    fun documentRoutes(handler: DocumentHandler): RouterFunction<ServerResponse> = router {
        "/api/documents".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                GET("", handler::getAllDocuments)
                POST("", handler::createDocument)
                GET("/{id}", handler::getDocument)
                PUT("/{id}", handler::updateDocument)
                DELETE("/{id}", handler::deleteDocument)
            }
        }
    }
}
