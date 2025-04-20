package nomoredelay.server.collaboard.repository

import nomoredelay.server.collaboard.model.Document
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface DocumentRepository : R2dbcRepository<Document, String> {
    fun findByTitle(title: String): Mono<Document>
    fun findAllByCreatedBy(userId: String): Flux<Document>
}
