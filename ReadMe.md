# 실시간 문서 협업 시스템 백엔드

구글 문서와 유사한 실시간 문서 협업 기능을 제공하는 백엔드 시스템입니다. Spring WebFlux와 WebSocket을 기반으로 하며, YJS를 활용한 실시간 동시 편집 기능을 제공합니다.

## 프로젝트 구조

백엔드 개발은 두 개의 주요 영역으로 나뉩니다:

1. **백엔드 개발자 1**: 문서 관리 및 인증 시스템
2. **백엔드 개발자 2**: 실시간 협업 및 WebSocket 통신

## 개발 환경

- Kotlin 1.9+
- Spring Boot 3.0+
- Spring WebFlux
- R2DBC
- WebSocket

## 패키지 구조

```
com.yourdomain.collabdoc
├── handler              // 함수형 핸들러
│   ├── DocumentHandler.kt
│   └── UserHandler.kt
├── router               // 라우터 함수 정의
│   ├── DocumentRouter.kt
│   └── UserRouter.kt
├── websocket            // WebSocket 관련
│   ├── handler          // WebSocket 핸들러
│   │   └── DocumentWebSocketHandler.kt
│   ├── config           // WebSocket 설정
│   │   └── WebSocketConfig.kt
│   └── message          // 메시지 타입
│       └── DocumentMessage.kt
├── config               // 애플리케이션 설정
│   ├── WebFluxConfig.kt
│   └── SecurityConfig.kt
├── model                // 도메인 모델
│   ├── Document.kt
│   └── User.kt
├── repository           // 데이터 접근
│   ├── DocumentRepository.kt
│   └── UserRepository.kt
└── service              // 비즈니스 로직
    ├── DocumentService.kt
    └── UserService.kt
```

## 백엔드 1: 문서 관리 및 인증 시스템

### 주요 기능
- Spring WebFlux 기반 REST API 개발
- R2DBC를 사용한 문서 CRUD 작업
- 사용자 인증 및 권한 관리 (JWT)
- 문서 버전 관리 및 저장소 설계
- 데이터 모델 및 스키마 정의

## 백엔드 2: 실시간 협업 및 WebSocket 통신

### 주요 기능
- WebSocket 서버 구현 (`/ws/documents/{id}`)
- YJS 업데이트 수신 및 처리 로직
- 클라이언트 간 실시간 동기화 관리
- 사용자 상태 및 커서 정보 브로드캐스팅
- 연결 관리 및 재연결 메커니즘

## WebSocket 데이터 흐름

1. 프론트엔드에서 문서 편집 → YJS가 변경사항 감지
2. YJS가 바이너리 업데이트 생성 → WebSocket으로 서버 전송
3. 서버에서 업데이트 수신 → 다른 클라이언트에 브로드캐스팅
4. 각 클라이언트의 YJS가 업데이트 적용 → 모든 사용자가 동일 문서 상태 유지

## 문서 저장 전략

다음 시점에 문서 상태를 DB에 저장합니다:

1. **주기적 저장**: 30초~1분 간격으로 WebFlux 스케줄러 사용
2. **중요 이벤트 시**: 큰 변경 후 저장
3. **사용자 이벤트**: 마지막 사용자 연결 종료 시 (필수)
4. **최적화**: 변경량이 일정 크기 이상일 때 저장

## WebSocket 서버 구현

WebFlux에서 WebSocket 핸들러 예시:

```kotlin
@Component
class DocumentWebSocketHandler : WebSocketHandler {
    override fun handle(session: WebSocketSession): Mono<Void> {
        // URL에서 문서 ID 추출
        val documentId = extractDocumentId(session)

        // 메시지 수신 및 브로드캐스팅
        val receive = session.receive()
            .doOnNext { message ->
                // YJS 업데이트 처리
                // 다른 클라이언트에 브로드캐스팅
            }
            .then()

        return receive
    }
}
```

## WebSocket 설정

```kotlin
@Configuration
class WebSocketConfig {
    @Bean
    fun webSocketHandlerMapping(handler: DocumentWebSocketHandler): HandlerMapping {
        val map = mapOf("/ws/documents/{id}" to handler)

        val mapping = SimpleUrlHandlerMapping()
        mapping.urlMap = map
        mapping.order = -1
        return mapping
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }
}
```

## 개발자 주의사항

### WebSocket 개발자 (백엔드 2)
1. **연결 관리**: 클라이언트 연결 끊김 감지 및 리소스 정리
2. **확장성**: 다수 동시 연결 처리를 위한 비동기 패턴 적용
3. **보안**: 인증 토큰 검증 및 권한 확인
4. **상태 관리**: 메모리 내 문서 상태 관리 및 주기적 저장
5. **하트비트**: 연결 상태 모니터링을 위한 하트비트 구현

### 문서 저장 개발자 (백엔드 1)
1. **R2DBC 활용**: 비동기 DB 접근 구현
2. **버전 관리**: 문서 이력 및 변경사항 추적
3. **인증 통합**: WebSocket 연결에 인증 적용

## 임시 저장 (Redis)

Redis를 활용한 임시 저장 방식:

```
KEY: "doc:{documentId}:state"  → VALUE: [YJS 바이너리 상태]
KEY: "doc:{documentId}:lastAccess" → VALUE: [타임스탬프]
```

## 핵심 통합 포인트

- 문서 상태는 메모리에서 관리 후 주기적으로 DB에 저장
- 백엔드 1의 인증 시스템이 백엔드 2의 WebSocket 연결에 적용
- 백엔드 2가 처리한 YJS 업데이트를 백엔드 1의 저장소에 전달
- 두 개발자가 같은 Spring 애플리케이션 내에서 다른 부분 담당

## 의존성

```kotlin
// build.gradle.kts
implementation("org.springframework.boot:spring-boot-starter-webflux")
implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("io.jsonwebtoken:jjwt-api:0.11.5")
implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
implementation("org.jetbrains.kotlin:kotlin-reflect")
implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
```

## 참고사항

- WebFlux에는 WebSocket 지원이 내장되어 있으므로 별도 의존성 추가 불필요
- 프론트엔드에서 YJS 처리, 백엔드는 메시지 중계 역할
- Redis는 임시 저장 및 세션 관리에 활용