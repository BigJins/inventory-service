package allmart.inventoryservice.adapter.kafka;

import allmart.inventoryservice.adapter.kafka.dto.OrderReserveFailedPayload;
import allmart.inventoryservice.application.required.InventoryOutboxPublisher;
import allmart.inventoryservice.application.required.OutboxRepository;
import allmart.inventoryservice.domain.event.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * InventoryOutboxPublisher 구현체.
 * reserve 실패 시 별도 트랜잭션으로 Outbox 저장 (reserve 트랜잭션 롤백과 독립적으로 커밋).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class InventoryOutboxPublisherAdapter implements InventoryOutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publishReserveFailed(String tossOrderId, String orderId) {
        save("order.reserve.failed.v1", orderId, new OrderReserveFailedPayload(tossOrderId, orderId));
    }

    private void save(String topic, String aggregateId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxRepository.save(OutboxEvent.create(topic, "order", aggregateId, json));
        } catch (Exception e) {
            throw new IllegalStateException(topic + " 이벤트 직렬화 실패: " + e.getMessage(), e);
        }
    }
}
