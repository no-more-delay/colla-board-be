package nomoredelay.server.collaboard.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("documents")
data class Document(
    @Id
    val id: String = UUID.randomUUID().toString(),
    
    val title: String,
    
    // YJS 문서 상태 저장 (바이너리 데이터)
    @Column("y_doc_state")
    val yDocState: ByteArray? = null,
    
    // 메타데이터
    val createdBy: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    // 버전 관리
    val version: Long = 0,
    
    // 협업 정보
    @Column("active_users")
    val activeUsers: String = "[]" // JSON 배열 형태로 저장
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Document
        
        if (id != other.id) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
}
