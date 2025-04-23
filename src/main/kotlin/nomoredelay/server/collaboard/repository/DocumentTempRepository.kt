package nomoredelay.server.collaboard.repository

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Duration

@Repository
class DocumentTempRepository(
    private val redisTemplate: ReactiveRedisTemplate<String, ByteArray>
) {
    companion object {
        private const val KEY_PREFIX = "doc:temp:"
        private val EXPIRATION = Duration.ofMinutes(10) // 10분 후 만료
    }
    
    fun saveTemp(docId: String, yDocState: ByteArray): Mono<Boolean> {
        val key = "$KEY_PREFIX$docId"
        return redisTemplate.opsForValue().set(key, yDocState, EXPIRATION)
    }
    
    fun getTemp(docId: String): Mono<ByteArray> {
        val key = "$KEY_PREFIX$docId"
        return redisTemplate.opsForValue().get(key)
    }
    
    fun deleteTemp(docId: String): Mono<Boolean> {
        val key = "$KEY_PREFIX$docId"
        return redisTemplate.opsForValue().delete(key)
    }
}
