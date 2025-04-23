package nomoredelay.server.collaboard.websocket.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import nomoredelay.server.collaboard.service.DocumentService
import nomoredelay.server.collaboard.websocket.message.DocumentMessage
import nomoredelay.server.collaboard.websocket.message.SyncMessage
import nomoredelay.server.collaboard.websocket.message.UserInfoMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class DocumentWebSocketHandler(
    private val documentService: DocumentService,
    private val objectMapper: ObjectMapper
) : WebSocketHandler {
    private val logger = LoggerFactory.getLogger(DocumentWebSocketHandler::class.java)
    
    // 문서별 연결 관리
    private val documentSessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()
    
    // 임시 저장 스케줄러 맵
    private val autoSaveSchedulers = ConcurrentHashMap<String, Disposable>()
    
    override fun handle(session: WebSocketSession): Mono<Void> {
        // 문서 ID 파싱 (예: /ws/document/123 형태의 URL에서 123 추출)
        val docId = extractDocumentId(session)
        logger.info("웹소켓 연결 시도: 세션 ID=${session.id}, 문서 ID=$docId, 핸드셰이크 URI=${session.handshakeInfo.uri}")
        
        // 세션 관리
        registerSession(docId, session)
        
        // 자동 저장 스케줄러 설정
        setupAutoSave(docId)
        
        // 수신 메시지 처리
        val input = session.receive()
            .doOnNext { message ->
                logger.info("메시지 수신: 세션 ID=${session.id}, 문서 ID=$docId, 타입=${message.type}")
                try {
                    processMessage(docId, session, message)
                } catch (e: Exception) {
                    logger.error("메시지 처리 중 오류: ${e.message}", e)
                }
            }
            .doOnError { error ->
                logger.error("메시지 수신 오류: ${error.message}", error)
            }
            .doFinally { signal ->
                logger.info("웹소켓 연결 종료: 세션 ID=${session.id}, 문서 ID=$docId, 신호=$signal")
                unregisterSession(docId, session)
            }
            .then()
        
        logger.info("웹소켓 연결 설정 완료: 세션 ID=${session.id}, 문서 ID=$docId")
        return input
    }
    
    private fun extractDocumentId(session: WebSocketSession): String {
        val path = session.handshakeInfo.uri.path
        logger.debug("URI 경로: $path")
        val docId = path.substring(path.lastIndexOf('/') + 1)
        logger.debug("추출된 문서 ID: $docId")
        return docId
    }
    
    private fun registerSession(docId: String, session: WebSocketSession) {
        documentSessions.computeIfAbsent(docId) { ConcurrentHashMap.newKeySet() }.add(session)
        logger.info("세션 등록: 문서 ID=$docId, 세션 ID=${session.id}, 활성 세션 수=${documentSessions[docId]?.size}")
    }
    
    private fun unregisterSession(docId: String, session: WebSocketSession) {
        documentSessions[docId]?.remove(session)
        logger.info("세션 해제: 문서 ID=$docId, 세션 ID=${session.id}, 남은 세션 수=${documentSessions[docId]?.size ?: 0}")
        
        if (documentSessions[docId]?.isEmpty() == true) {
            documentSessions.remove(docId)
            // 스케줄러 취소
            autoSaveSchedulers[docId]?.dispose()
            autoSaveSchedulers.remove(docId)
            logger.info("문서의 모든 세션 종료: 문서 ID=$docId")
        }
    }
    
    private fun processMessage(docId: String, senderSession: WebSocketSession, message: WebSocketMessage) {
        try {
            when (message.type) {
                WebSocketMessage.Type.TEXT -> {
                    val text = message.payloadAsText
                    logger.debug("텍스트 메시지 수신: 문서 ID=$docId, 내용=$text")
                    
                    val docMessage = objectMapper.readValue<DocumentMessage>(text)
                    
                    when (docMessage) {
                        is SyncMessage -> {
                            logger.debug("Sync 메시지 처리: 문서 ID=$docId, Origin=${docMessage.origin}")
                            documentService.saveTempState(docId, docMessage.update)
                                .subscribe()
                            
                            // 다른 세션들에 직접 브로드캐스팅
                            broadcastToOtherSessions(docId, senderSession, text)
                        }
                        is UserInfoMessage -> {
                            logger.debug("사용자 정보 메시지: 문서 ID=$docId, 사용자=${docMessage.name}")
                            // 모든 세션에 사용자 정보 브로드캐스팅 (보낸 사람 포함)
                            broadcastToAllSessions(docId, text)
                        }
                        else -> {
                            // 나머지 메시지 타입도 브로드캐스팅
                            broadcastToOtherSessions(docId, senderSession, text)
                        }
                    }
                }
                WebSocketMessage.Type.BINARY -> {
                    logger.debug("바이너리 메시지 수신: 문서 ID=$docId, 크기=${message.payload.readableByteCount()}")
                    
                    // 바이너리 데이터 복사
                    val bytes = ByteArray(message.payload.readableByteCount())
                    message.payload.read(bytes)
                    
                    // 임시 저장
                    documentService.saveTempState(docId, bytes)
                        .subscribe()
                    
                    // 바이너리 메시지 그대로 브로드캐스팅하지 않음
                    // YJS 프로토콜 메시지는 텍스트로 변환해서 처리
                }
                else -> {
                    logger.warn("지원하지 않는 메시지 타입: ${message.type}")
                }
            }
        } catch (e: Exception) {
            logger.error("메시지 처리 오류: ${e.message}", e)
        }
    }
    
    // 다른 세션들에게 메시지 전송 (브로드캐스팅)
    private fun broadcastToOtherSessions(docId: String, senderSession: WebSocketSession, text: String) {
        val sessions = documentSessions[docId] ?: return
        
        sessions.forEach { session ->
            // 메시지를 보낸 세션이 아닌 다른 세션들에게만 전송
            if (session.id != senderSession.id && session.isOpen) {
                try {
                    logger.debug("메시지 전송: 대상 세션=${session.id}, 문서 ID=$docId")
                    val newMessage = session.textMessage(text)
                    session.send(Mono.just(newMessage)).subscribe()
                } catch (e: Exception) {
                    logger.error("메시지 브로드캐스팅 오류: ${e.message}", e)
                }
            }
        }
        
        logger.info("메시지 브로드캐스팅 완료: 문서=$docId, 전송 세션 수=${sessions.size - 1}")
    }
    
    // 모든 세션에게 메시지 전송 (보낸 사람 포함)
    private fun broadcastToAllSessions(docId: String, text: String) {
        val sessions = documentSessions[docId] ?: return
        
        sessions.forEach { session ->
            if (session.isOpen) {
                try {
                    logger.debug("메시지 전송: 대상 세션=${session.id}, 문서 ID=$docId")
                    val newMessage = session.textMessage(text)
                    session.send(Mono.just(newMessage)).subscribe()
                } catch (e: Exception) {
                    logger.error("메시지 브로드캐스팅 오류: ${e.message}", e)
                }
            }
        }
        
        logger.info("메시지 브로드캐스팅 완료(전체): 문서=$docId, 전송 세션 수=${sessions.size}")
    }
    
    private fun setupAutoSave(docId: String) {
        if (!autoSaveSchedulers.containsKey(docId)) {
            logger.info("자동 저장 스케줄러 설정: 문서 ID=$docId")
            
            // 1분마다 자동 저장
            val scheduler = Flux.interval(Duration.ofMinutes(1))
                .doOnNext {
                    logger.debug("자동 저장 실행: 문서 ID=$docId")
                    documentService.getTempState(docId)
                        .flatMap { state ->
                            logger.debug("임시 상태 조회 성공: 문서 ID=$docId, 크기=${state.size}")
                            documentService.saveDocumentState(docId, state)
                        }
                        .doOnError { error ->
                            logger.error("자동 저장 오류: ${error.message}", error)
                        }
                        .subscribe()
                }
                .publish()
                .connect()
            
            autoSaveSchedulers[docId] = scheduler
        }
    }
}
