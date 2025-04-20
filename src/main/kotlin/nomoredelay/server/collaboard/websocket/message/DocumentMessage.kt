package nomoredelay.server.collaboard.websocket.message

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SyncMessage::class, name = "sync"),
    JsonSubTypes.Type(value = AwarenessMessage::class, name = "awareness"),
    JsonSubTypes.Type(value = UserInfoMessage::class, name = "user_info"),
    JsonSubTypes.Type(value = ErrorMessage::class, name = "error")
)
sealed class DocumentMessage

// YJS 동기화 메시지
data class SyncMessage(
    val docId: String,
    val update: ByteArray, // YJS 업데이트 바이너리 데이터
    val origin: String? = null // 업데이트 출처 (사용자 ID)
) : DocumentMessage() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as SyncMessage
        
        if (docId != other.docId) return false
        if (!update.contentEquals(other.update)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = docId.hashCode()
        result = 31 * result + update.contentHashCode()
        return result
    }
}

// 사용자 상태 메시지 (커서 위치 등)
data class AwarenessMessage(
    val docId: String,
    val clientId: String,
    val awareness: ByteArray // YJS awareness 바이너리 데이터
) : DocumentMessage() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as AwarenessMessage
        
        if (docId != other.docId) return false
        if (clientId != other.clientId) return false
        if (!awareness.contentEquals(other.awareness)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = docId.hashCode()
        result = 31 * result + clientId.hashCode()
        result = 31 * result + awareness.contentHashCode()
        return result
    }
}

// 사용자 정보 메시지
data class UserInfoMessage(
    val docId: String,
    val userId: String,
    val name: String,
    val color: String
) : DocumentMessage()

// 오류 메시지
data class ErrorMessage(
    val message: String,
    val code: String
) : DocumentMessage()
