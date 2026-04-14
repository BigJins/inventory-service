package allmart.inventoryservice.domain.event;

import allmart.inventoryservice.domain.AbstractEntity;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outbox 패턴 이벤트 엔티티.
 * 재고 처리 결과를 동일 트랜잭션에 저장 → Debezium CDC가 감지 후 Kafka로 릴레이.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends AbstractEntity {

    private String eventType;
    private String aggregateId;
    private String aggregateType;
    private String payload;
    private LocalDateTime createdAt;

    public static OutboxEvent create(
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload
    ) {
        OutboxEvent event = new OutboxEvent();
        event.eventType     = eventType;
        event.aggregateType = aggregateType;
        event.aggregateId   = aggregateId;
        event.payload       = payload;
        event.createdAt     = LocalDateTime.now();
        return event;
    }
}
