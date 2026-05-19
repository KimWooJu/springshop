<div align="center">

# 🛒 SpringShop

### Enterprise-Grade E-Commerce Platform built with Java 25 + Spring Boot 3.4

[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![Lines of Code](https://img.shields.io/badge/Lines_of_Code-35%2C700%2B-brightgreen?style=for-the-badge)](.)

<br/>

> **Java 25의 최신 기능을 모두 활용한 엔터프라이즈급 이커머스 백엔드 샘플 프로젝트**  
> *A comprehensive e-commerce backend showcasing every major Java 25 feature in production-grade patterns*

<br/>

```
  ╔═══════════════════════════════════════════════════════╗
  ║   Virtual Threads  ·  Sealed Interfaces  ·  Records   ║
  ║   Pattern Matching ·  Text Blocks        ·  Gatherers  ║
  ║   StructuredTaskScope · Spring Boot 3.4 · 35,700 LOC  ║
  ╚═══════════════════════════════════════════════════════╝
```

</div>

---

## 📋 목차 / Table of Contents

- [🌟 프로젝트 소개](#-프로젝트-소개--about-this-project)
- [🏗️ 아키텍처](#-아키텍처--architecture)
- [⚡ Java 25 핵심 기능](#-java-25-핵심-기능--java-25-key-features)
- [🧩 모듈 구조](#-모듈-구조--module-structure)
- [🔧 기술 스택](#-기술-스택--tech-stack)
- [🚀 빌드 및 실행](#-빌드-및-실행--build--run)
- [📖 API 문서](#-api-문서--api-documentation)
- [📊 프로젝트 규모](#-프로젝트-규모--project-scale)

---

## 🌟 프로젝트 소개 / About This Project

**SpringShop**은 Java 25의 모든 최신 언어 기능과 Spring Boot 3.4의 최신 패턴을 실제 이커머스 도메인에 적용한 대규모 샘플 프로젝트입니다.

코드 분석 도구(SonarQube, PMD, SpotBugs 등)의 Java 25 파싱 능력 검증과  
엔터프라이즈 Java 아키텍처 레퍼런스를 목적으로 설계되었습니다.

**SpringShop** is a large-scale sample project that applies all the latest Java 25 language features and Spring Boot 3.4 patterns to a real-world e-commerce domain.

Designed as a reference for enterprise Java architecture and for validating Java 25 parsing capability of code analysis tools such as SonarQube, PMD, and SpotBugs.

### ✨ 주요 특징 / Highlights

| 특징 | 설명 |
|------|------|
| 🔬 **Java 25 All-In** | Virtual Thread, Sealed Interface, Pattern Matching, Record, Text Block, Stream Gatherer 전부 사용 |
| 🏢 **엔터프라이즈 패턴** | DDD, CQRS 분리, Event-Driven, Repository Pattern, Domain Event |
| 🔐 **완전한 보안** | JWT + Redis Blacklist, Rate Limiting, CORS, Spring Security 6.x |
| 📡 **REST API 완비** | 12개 컨트롤러, 80+ 엔드포인트, SpringDoc OpenAPI 3.0 |
| ⚡ **고성능 설계** | Virtual Thread 기반 비동기, Caffeine L1 + Redis L2 이중 캐시 |
| 📊 **35,700+ Lines** | 242개 Java 파일, 프로덕션 수준의 코드 밀도 |

---

## 🏗️ 아키텍처 / Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT / BROWSER                          │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS
┌────────────────────────────▼────────────────────────────────────┐
│                    springshop-app  (Port 8080)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │ CorrelationId│  │  RateLimit   │  │  RequestLogging      │   │
│  │   Filter     │  │   Filter     │  │  Filter              │   │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘   │
│         └─────────────────┼──────────────────────┘              │
│  ┌──────────────────────── ▼ ─────────────────────────────────┐ │
│  │           JwtAuthenticationFilter (Spring Security)         │ │
│  └──────────────────────── ┬ ─────────────────────────────────┘ │
│  ┌───────────────┐  ┌──────▼────────┐  ┌──────────────────────┐ │
│  │Performance    │  │  Audit        │  │  Locale              │ │
│  │Interceptor    │  │  Interceptor  │  │  Interceptor         │ │
│  └───────────────┘  └───────────────┘  └──────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                    springshop-web                                │
│   UserController  ProductController  OrderController  ...       │
│   (12 Controllers · 80+ Endpoints · OpenAPI 3.0 Docs)           │
│   MapStruct Mappers  ·  Bean Validation  ·  Custom Validators    │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                   springshop-service                             │
│   UserService  ProductService  OrderService  PaymentService     │
│   CartService  CouponService   ReviewService  NotificationService│
│   Event Handlers (OrderEvent, UserEvent)                        │
│   Schedulers  ·  Virtual Thread Executors  ·  @Async            │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                   springshop-domain                              │
│   User · Product · Order · Payment · Cart · Coupon · Review     │
│   Notification · Inventory  (JPA Entities + Repositories)       │
│   Domain Events  ·  Value Objects  ·  Base Audit Entity          │
└──────────┬──────────────────────────────────┬───────────────────┘
           │                                  │
    ┌──────▼──────┐                   ┌───────▼──────┐
    │  H2 / MySQL │                   │    Redis     │
    │  (JPA/JDBC) │                   │  (Cache/JWT  │
    └─────────────┘                   │   Blacklist) │
                                      └──────────────┘
```

### 레이어드 아키텍처 원칙 / Layered Architecture Principles

- **단방향 의존성**: app → web → service → domain → common (역방향 참조 없음)
- **도메인 중심 설계**: 엔티티가 비즈니스 로직을 소유하고 서비스는 조율만 담당
- **이벤트 기반 분리**: `ApplicationEventPublisher`로 도메인 간 결합도를 낮춤
- **Unidirectional dependency**: app → web → service → domain → common (no reverse references)

---

## ⚡ Java 25 핵심 기능 / Java 25 Key Features

### 🧵 Virtual Threads (Project Loom)

```java
// 수천 개의 동시 요청을 경량 Virtual Thread로 처리
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<Double> avgFuture    = executor.submit(() -> calculateAverageRating(productId));
    Future<Long>   countFuture  = executor.submit(() -> countReviews(productId));
    Future<Map<Integer, Long>> distFuture = executor.submit(() -> getRatingDistribution(productId));

    return Map.of(
        "averageRating",      avgFuture.get(),
        "totalCount",         countFuture.get(),
        "ratingDistribution", distFuture.get()
    );
}
```

### 🔒 Sealed Interfaces + Pattern Matching Switch

```java
// 쿠폰 유효성 검증 결과를 sealed interface로 타입 안전하게 표현
sealed interface CouponValidationResult
    permits Valid, Expired, NotApplicable, UsageLimitReached, AlreadyUsed {

    record Valid(BigDecimal discountAmount, BigDecimal finalAmount) implements CouponValidationResult {}
    record Expired(String couponCode, LocalDate expiredAt)          implements CouponValidationResult {}
    record UsageLimitReached(int limit, int current)                implements CouponValidationResult {}
}

// 패턴 매칭 switch로 모든 케이스를 컴파일 타임에 보장
BigDecimal discount = switch (validateCoupon(code, userId, amount)) {
    case Valid v              -> v.discountAmount();
    case Expired e            -> throw new IllegalStateException("만료: " + e.expiredAt());
    case NotApplicable n      -> throw new IllegalStateException(n.reason());
    case UsageLimitReached l  -> throw new IllegalStateException("한도 초과: " + l.current());
    case AlreadyUsed a        -> throw new IllegalStateException("이미 사용됨: " + a.usedAt());
};
```

### 📦 Records as Value Objects

```java
// 불변 도메인 객체를 record로 간결하게 정의
public record TokenPair(
    String accessToken,
    String refreshToken,
    LocalDateTime accessExpiresAt,
    LocalDateTime refreshExpiresAt
) {}

// 중첩 record로 복잡한 이벤트 페이로드 표현
public record OrderPlacedEvent(
    Long orderId, String orderNo, Long userId,
    List<OrderItemSnapshot> items,
    BigDecimal totalAmount,
    LocalDateTime occurredAt
) {
    public record OrderItemSnapshot(
        Long productId, String productName,
        int quantity, BigDecimal price
    ) {}
}
```

### 📝 Text Blocks (HTML Email Templates)

```java
// HTML 이메일 템플릿을 Text Block으로 가독성 높게 작성
String htmlBody = """
        <!DOCTYPE html>
        <html lang="ko">
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <h2 style="color: #333;">안녕하세요, %s님!</h2>
          <p>주문 <strong>#%s</strong>이 성공적으로 접수되었습니다.</p>
          <p style="font-size: 18px; font-weight: bold;">합계: %s원</p>
        </body>
        </html>
        """.formatted(userName, orderNo, totalAmount);
```

### 🔬 StructuredTaskScope (Concurrent Programming)

```java
// StructuredTaskScope로 구조적 병렬 처리
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var stockTask   = scope.fork(() -> fetchCurrentStock(productId));
    var reservedTask = scope.fork(() -> fetchReservedStock(productId));
    var alertTask   = scope.fork(() -> fetchAlertThreshold(productId));

    scope.join().throwIfFailed();

    return InventorySnapshot.of(
        stockTask.get(), reservedTask.get(), alertTask.get()
    );
}
```

### 🎯 API Response — Sealed Interface 패턴

```java
// 모든 API 응답을 하나의 sealed interface로 통합
public sealed interface ApiResponse<T>
        permits ApiResponse.Success, ApiResponse.Failure, ApiResponse.Paginated {

    record Success<T>(T data, String message, LocalDateTime timestamp, String traceId)
            implements ApiResponse<T> {
        public static <T> Success<T> of(T data) { ... }
        public static Success<Void> empty()      { ... }
    }

    record Paginated<T>(List<T> content, long totalElements, int totalPages, ...)
            implements ApiResponse<T> { ... }

    // 클라이언트에서 패턴 매칭으로 분기
    default boolean isSuccess() {
        return switch (this) {
            case Success<T>   s -> true;
            case Failure<T>   f -> false;
            case Paginated<T> p -> true;
        };
    }
}
```

---

## 🧩 모듈 구조 / Module Structure

```
java-spring-samples/
├── pom.xml                          # Parent POM (Spring Boot 3.4.1, Java 25)
│
├── springshop-common/               # 공통 모듈 (4,533 lines)
│   └── src/main/java/com/springshop/common/
│       ├── constant/                # AppConstants, SecurityConstants, CacheConstants ...
│       ├── exception/               # GlobalExceptionHandler, ErrorCode, BusinessException ...
│       ├── model/                   # PageInfo, SearchFilter, SortOption, Gender ...
│       └── util/                    # DateUtils, JsonUtils, MoneyUtils, EncryptionUtils ...
│
├── springshop-domain/               # 도메인 모듈 (10,098 lines)
│   └── src/main/java/com/springshop/domain/
│       ├── base/                    # BaseEntity, BaseAuditEntity
│       ├── user/                    # User, UserProfile, UserAddress + Repositories
│       ├── product/                 # Product, Category, Brand, Tag, ProductImage + Repos
│       ├── order/                   # Order, OrderItem, OrderShipping, OrderStatus
│       ├── payment/                 # Payment, Refund, PaymentTransaction, PaymentMethod
│       ├── cart/                    # Cart, CartItem
│       ├── coupon/                  # Coupon, CouponUsage, DiscountPolicy
│       ├── review/                  # Review, ReviewImage, ReviewStatus
│       ├── inventory/               # Inventory, InventoryLog, StockAlertThreshold
│       └── notification/            # Notification, NotificationTemplate, NotificationType
│
├── springshop-service/              # 서비스 모듈 (9,349 lines)
│   └── src/main/java/com/springshop/service/
│       ├── user/                    # UserService, AuthService, UserAddressService
│       ├── product/                 # ProductService, CategoryService, BrandService ...
│       ├── order/                   # OrderService, OrderProcessingService
│       ├── payment/                 # PaymentService, RefundService, PaymentGatewayAdapter
│       ├── cart/                    # CartService
│       ├── coupon/                  # CouponService (sealed ValidationResult)
│       ├── review/                  # ReviewService (Virtual Thread statistics)
│       ├── notification/            # NotificationService, EmailService (Text Block templates)
│       ├── inventory/               # InventoryService (StructuredTaskScope)
│       ├── event/                   # OrderEventHandler, UserEventHandler (@TransactionalEventListener)
│       └── scheduler/               # 4 Schedulers (Virtual Thread + @Scheduled)
│
├── springshop-web/                  # 웹 모듈 (8,811 lines)
│   └── src/main/java/com/springshop/web/
│       ├── controller/              # 12 REST Controllers (80+ endpoints)
│       ├── dto/
│       │   ├── request/             # 22 Request DTOs (Bean Validation)
│       │   └── response/            # 24 Response DTOs (sealed ApiResponse)
│       ├── mapper/                  # 9 MapStruct Mappers
│       └── validator/               # 5 Custom Constraint Validators
│
└── springshop-app/                  # 애플리케이션 진입점 (2,650 lines)
    └── src/main/java/com/springshop/
        ├── Application.java          # @SpringBootApplication
        ├── config/                  # 9 Configuration classes
        │   ├── SecurityConfig.java   # JWT filter chain, CORS
        │   ├── AsyncConfig.java      # Virtual Thread executor
        │   ├── CacheConfig.java      # Caffeine L1 + Redis L2
        │   ├── SwaggerConfig.java    # OpenAPI 3.0 + Bearer auth
        │   └── ...
        ├── security/                # JwtTokenProvider, JwtAuthFilter, SecurityUtils
        │   └── (sealed ResourceAction for permission model)
        ├── filter/                  # CorrelationId, RateLimit, RequestLogging
        ├── interceptor/             # Performance, Audit, Locale
        └── resources/
            ├── application.yml      # 기본 설정
            ├── application-dev.yml  # 개발 환경
            └── application-prod.yml # 운영 환경
```

---

## 🔧 기술 스택 / Tech Stack

### Core
| 기술 | 버전 | 용도 |
|------|------|------|
| **Java** | 25 | 언어 (Virtual Thread, Pattern Matching, Sealed Interface) |
| **Spring Boot** | 3.4.1 | 애플리케이션 프레임워크 |
| **Maven** | 3.9.x | 빌드 도구 |
| **Spring Security** | 6.x | 인증/인가 (JWT) |
| **Spring Data JPA** | 3.4.x | ORM |

### Infrastructure
| 기술 | 버전 | 용도 |
|------|------|------|
| **H2** | Runtime | 임베디드 DB (개발/테스트) |
| **MySQL** | 8.x | 운영 DB |
| **Redis** | 7.x | 캐시 + JWT 블랙리스트 |
| **Caffeine** | 3.x | 로컬 인메모리 캐시 (L1) |

### Libraries
| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| **JJWT** | 0.12.3 | JWT 발급/검증 |
| **MapStruct** | 1.5.5 | DTO ↔ Entity 매핑 |
| **SpringDoc OpenAPI** | 2.3.0 | Swagger UI |
| **Lombok** | Latest | 보일러플레이트 제거 |
| **Spring Mail** | 3.4.x | 이메일 발송 |

---

## 🚀 빌드 및 실행 / Build & Run

### 사전 요구사항 / Prerequisites

```bash
# Java 25 설치 확인
java --version
# openjdk 25 ...

# Maven 3.9 설치 확인
mvn --version
# Apache Maven 3.9.x ...
```

### 빌드 / Build

```bash
# 전체 멀티 모듈 빌드
git clone https://github.com/KimWooJu/springshop.git
cd springshop

mvn clean package -DskipTests

# 결과: springshop-app/target/springshop-app-1.0.0-SNAPSHOT.jar
```

### 실행 / Run

```bash
# 개발 환경 (H2 인메모리 DB)
java -jar springshop-app/target/springshop-app-1.0.0-SNAPSHOT.jar \
     --spring.profiles.active=dev

# 또는 Maven으로 직접 실행
cd springshop-app
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 접속 / Access

| 서비스 | URL |
|--------|-----|
| **API 서버** | http://localhost:8080 |
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **H2 콘솔** (dev only) | http://localhost:8080/h2-console |
| **Actuator Health** | http://localhost:8080/actuator/health |

### Docker (선택 사항 / Optional)

```bash
# Redis 실행 (캐시 + JWT 블랙리스트)
docker run -d -p 6379:6379 --name springshop-redis redis:7-alpine

# 애플리케이션 실행
java -jar springshop-app/target/springshop-app-1.0.0-SNAPSHOT.jar \
     --spring.data.redis.host=localhost
```

---

## 📖 API 문서 / API Documentation

서버 실행 후 Swagger UI에서 전체 API를 탐색할 수 있습니다.  
Full API documentation is available via Swagger UI after starting the server.

```
http://localhost:8080/swagger-ui.html
```

### API 그룹 / API Groups

| 그룹 | 엔드포인트 | 설명 |
|------|-----------|------|
| 🔐 **Auth** | `/api/v1/auth/**` | 로그인, 토큰 갱신, 비밀번호 재설정 |
| 👤 **Users** | `/api/v1/users/**` | 회원 CRUD, 배송지 관리 |
| 📦 **Products** | `/api/v1/products/**` | 상품 검색, 등록, 이미지 관리 |
| 🗂️ **Categories** | `/api/v1/categories/**` | 카테고리 트리 관리 |
| 📋 **Orders** | `/api/v1/orders/**` | 주문 생성, 상태 관리, 배송 추적 |
| 🛒 **Cart** | `/api/v1/cart/**` | 장바구니 CRUD, 쿠폰 적용 |
| 💳 **Payments** | `/api/v1/payments/**` | 결제 처리, 환불 |
| 📦 **Inventory** | `/api/v1/inventory/**` | 재고 조회 및 관리 |
| ⭐ **Reviews** | `/api/v1/reviews/**` | 리뷰 CRUD, 신고, 도움이 됐어요 |
| 🎟️ **Coupons** | `/api/v1/coupons/**` | 쿠폰 발급, 적용, 관리 |
| 🔔 **Notifications** | `/api/v1/notifications/**` | 알림 조회, 읽음 처리 |
| ⚙️ **Admin** | `/api/v1/admin/**` | 대시보드, 통계, 운영 관리 |

---

## 📊 프로젝트 규모 / Project Scale

```
┌─────────────────────────────────────────────────────────────────┐
│                   📊 Code Statistics                             │
├─────────────────┬──────────────┬───────────────┬───────────────┤
│ Module          │ Files        │ Lines (Java)   │ Highlights    │
├─────────────────┼──────────────┼───────────────┼───────────────┤
│ common          │     36       │     4,533      │ 12 Utils      │
│ domain          │     61       │    10,098      │ 10 Domains    │
│ service         │     47       │     9,349      │ 4 Schedulers  │
│ web             │     77       │     8,811      │ 12 Controllers│
│ app             │     21       │     2,650      │ 9 Configs     │
├─────────────────┼──────────────┼───────────────┼───────────────┤
│ YAML Configs    │      3       │       265      │ 3 Profiles    │
├─────────────────┼──────────────┼───────────────┼───────────────┤
│ 🏆 TOTAL        │    245       │    35,706      │               │
└─────────────────┴──────────────┴───────────────┴───────────────┘
```

### Java 25 기능 활용 현황 / Java 25 Feature Coverage

| 기능 | 사용 위치 | 건수 |
|------|----------|------|
| `record` | DTO, Event, Value Object | 60+ |
| `sealed interface` | ApiResponse, CouponValidationResult, ResourceAction | 8+ |
| Pattern Matching `switch` | Service layer, Mapper, Utils | 30+ |
| Virtual Thread (`Thread.ofVirtual()`) | Service, Scheduler | 10+ |
| `StructuredTaskScope` | InventoryService | 2+ |
| Text Block | EmailService, HTML templates | 15+ |
| Stream Gatherer | Review statistics | 3+ |
| `var` (Local Type Inference) | Throughout | 200+ |
| `@EventListener` / `@TransactionalEventListener` | Event handlers | 8+ |

---

## 🧪 코드 분석 도구 호환성 / Code Analysis Tool Compatibility

이 프로젝트는 다음 코드 분석 도구의 Java 25 지원 검증에 사용할 수 있습니다.

| 도구 | 검증 항목 |
|------|----------|
| **SonarQube** | Java 25 구문 파싱, 코드 스멜, 복잡도 |
| **PMD** | Rule 적용, CPD 중복 코드 탐지 |
| **SpotBugs** | 잠재적 버그 탐지 |
| **Checkstyle** | 코딩 컨벤션 |
| **JaCoCo** | 코드 커버리지 |

---

## 📝 라이선스 / License

이 프로젝트는 MIT 라이선스를 따릅니다.  
This project is licensed under the MIT License.

---

<div align="center">

**Made with ❤️ for the Java community**

*Java 25 · Spring Boot 3.4 · 35,700+ Lines of Production-Grade Code*

[![GitHub Stars](https://img.shields.io/github/stars/KimWooJu/springshop?style=social)](https://github.com/KimWooJu/springshop)

</div>
