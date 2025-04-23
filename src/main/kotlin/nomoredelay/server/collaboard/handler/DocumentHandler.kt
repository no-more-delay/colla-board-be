package nomoredelay.server.collaboard.handler

import nomoredelay.server.collaboard.model.Document
import nomoredelay.server.collaboard.service.DocumentService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyToMono
import reactor.core.publisher.Mono

@Component
class DocumentHandler(private val documentService: DocumentService) {
    fun getAllDocuments(request: ServerRequest): Mono<ServerResponse> {
        val userId = request.queryParam("userId").orElse(null)
        
        return if (userId != null) {
            documentService.getUserDocuments(userId)
                .collectList()
                .flatMap { documents ->
                    ServerResponse.ok().bodyValue(documents)
                }
        } else {
            documentService.getAllDocuments()
                .collectList()
                .flatMap { documents ->
                    ServerResponse.ok().bodyValue(documents)
                }
        }
    }
    
    fun getDocument(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id")
        return documentService.getDocument(id)
            .flatMap { document ->
                ServerResponse.ok().bodyValue(document)
            }
            .switchIfEmpty(ServerResponse.notFound().build())
    }
    
    fun createDocument(request: ServerRequest): Mono<ServerResponse> {
        return request.bodyToMono(DocumentRequest::class.java)
            .flatMap { req ->
                documentService.createDocument(req.title, req.userId)
            }
            .flatMap { document ->
                ServerResponse.ok().bodyValue(document)
            }
    }
    
    fun updateDocument(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id")
        return request.bodyToMono(DocumentRequest::class.java)
            .flatMap { req ->
                documentService.updateDocument(id, req.title)
            }
            .flatMap { document ->
                ServerResponse.ok().bodyValue(document)
            }
            .switchIfEmpty(ServerResponse.notFound().build())
    }
    
    fun deleteDocument(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id")
        return documentService.deleteDocument(id)
            .then(ServerResponse.noContent().build())
    }
    
    data class DocumentRequest(
        val title: String,
        val userId: String
    )
}
