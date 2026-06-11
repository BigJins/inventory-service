# inventory-service

**allmart** 이커머스 플랫폼의 재고 도메인 마이크로서비스입니다.
재고 예약/확정/해제를 담당하며, Choreography Saga에서 보상 트랜잭션의 트리거 역할을 합니다.

## 관련 서비스

| 서비스 | 역할 | GitHub |
|--------|------|--------|
| **inventory-service** | 재고 예약 / 확정 / 해제 | 현재 레포 |
| order-service | 주문 생성 / 상태 관리 / CQRS | [BigJins/order-service](https://github.com/BigJins/order-service) |
| delivery-service | 배송 상태 머신 | [BigJins/delivery-service](https://github.com/BigJins/delivery-service) |
| auth-service | RS256 JWT 발급 / JWKS | [BigJins/auth-service](https://github.com/BigJins/auth-service) |
| apigateway-service | JWT 검증 / Rate Limiting | [BigJins/apigateway-service](https://github.com/BigJins/apigateway-service) |
| product-service | 상품 / 카테고리 | [BigJins/product-service](https://github.com/BigJins/product-service) |

## 기술 스택

- **Java 21** + Spring Boot 4
- Spring Data JPA + MySQL 8
- **Redis** — 재고 수량 원자 연산 (DECRBY/INCRBY)
- Apache Kafka — `order.created.v1`, `payment.result.v1`, `order.canceled.v1` 소비
- Debezium CDC — `outbox_event` → `order.reserve.failed.v1` 발행

## 주요 구현 포인트

### 재고 상태 흐름

```
order.created.v1 소비 → 예약 (AVAILABLE → RESERVED)
payment.result.v1 DONE → 확정 (RESERVED → DEDUCTED)
payment.result.v1 FAILED / order.canceled.v1 → 해제 (RESERVED → AVAILABLE, 멱등)
재고 부족 → order.reserve.failed.v1 발행 → order-service가 주문 자동 취소 (Saga 보상)
```

### 비관적 락 → Redis 원자 연산 전환 (부하테스트 실측 근거)

**문제**: 800 RPS 부하에서 `SELECT ... FOR UPDATE`가 동일 인기 상품에 몰리며 행 락 직렬화 → 락 대기 → 전체 요청 타임아웃

**해결**:

```
변경 전: SELECT FOR UPDATE → 도메인 검증 → save
변경 후: Redis DECRBY inv:stock:{productId} (원자 연산, 락 없음)
        remaining < 0 → INCRBY 롤백 + InsufficientStockException
        성공 시 예약 레코드 DB 저장
```

**안전장치**:

- 서비스 시작 시 DB → Redis 재고 동기화 (PENDING 예약분 차감)
- Redis 키 미존재/장애 시 **DB fallback** — Redis 다운이 재고 서비스 전체 중단으로 번지지 않음

### 멱등 소비

모든 이벤트에 포함된 고유 식별자(orderId)로 상태 체크 후 중복 시 skip — Kafka 재전달/Outbox 재발행 상황에서도 재고가 이중 차감되지 않음.

## 테스트

```bash
./gradlew test
```

CI: GitHub Actions — 빌드 + 테스트 + SpotBugs 정적분석 ([.github/workflows](.github/workflows))
CD: master 머지 시 ECR 푸시 → k3s `kubectl set image` 롤링 배포
