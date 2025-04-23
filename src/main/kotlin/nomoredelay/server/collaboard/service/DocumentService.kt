package nomoredelay.server.collaboard.service

import nomoredelay.server.collaboard.model.Document
import nomoredelay.server.collaboard.repository.DocumentRepository
import nomoredelay.server.collaboard.repository.DocumentTempRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val documentTempRepository: DocumentTempRepository
) {
    // 문서 생성
    fun createDocument(title: String, userId: String): Mono<Document> {
        val document = Document(
            title = title,
            createdBy = userId
        )
        return documentRepository.save(document)
    }
    
    // 문서 조회
    fun getDocument(id: String): Mono<Document> {
        return documentRepository.findById(id)
    }
    
    // 사용자 문서 목록
    fun getUserDocuments(userId: String): Flux<Document> {
        return documentRepository.findAllByCreatedBy(userId)
    }
    
    // YJS 문서 상태 저장 (자동 저장)
    fun saveDocumentState(id: String, yDocState: ByteArray): Mono<Document> {
        return documentRepository.findById(id)
            .flatMap { doc ->
                val updatedDoc = doc.copy(
                    yDocState = yDocState,
                    updatedAt = LocalDateTime.now(),
                    version = doc.version + 1
                )
                documentRepository.save(updatedDoc)
            }
    }
    
    // 임시 저장 (Redis)
    fun saveTempState(id: String, yDocState: ByteArray): Mono<Boolean> {
        return documentTempRepository.saveTemp(id, yDocState)
    }
    
    // 임시 저장 불러오기
    fun getTempState(id: String): Mono<ByteArray> {
        return documentTempRepository.getTemp(id)
    }
    
    // 모든 문서 조회
    fun getAllDocuments(): Flux<Document> {
        return documentRepository.findAll()
    }
    
    // 문서 삭제
    fun deleteDocument(id: String): Mono<Void> {
        return documentRepository.deleteById(id)
    }
    
    // 문서 업데이트
    fun updateDocument(id: String, title: String): Mono<Document> {
        return documentRepository.findById(id)
            .flatMap { doc ->
                val updatedDoc = doc.copy(
                    title = title,
                    updatedAt = LocalDateTime.now()
                )
                documentRepository.save(updatedDoc)
            }
    }
}
