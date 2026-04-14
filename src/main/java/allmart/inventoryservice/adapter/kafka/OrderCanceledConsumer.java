package allmart.inventoryservice.adapter.kafka;

import allmart.inventoryservice.adapter.kafka.dto.OrderCanceledMessage;
import allmart.inventoryservice.application.provided.InventoryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * order.canceled.v1 Kafka 컨슈머.
 * 주문 취소 → 재고 해제 (PENDING → RELEASED, Redis 복구).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCanceledConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryManager inventoryManager;

    @KafkaListener(
            topics = "${kafka.topics.order-canceled}",
            groupId = "${kafka.consumer.group-id:inventory-service}"
    )
    public void onMessage(String value) {
        OrderCanceledMessage msg = parseMessage(value);
        log.info("order.canceled 수신 → 재고 해제: orderId={}, tossOrderId={}", msg.orderId(), msg.tossOrderId());
        inventoryManager.release(msg.tossOrderId());
    }

    /** Debezium EventRouter envelope 대응 */
    private OrderCanceledMessage parseMessage(String value) {
        try {
            var root = objectMapper.readTree(value);
            String payload = root.has("payload") ? root.get("payload").asText() : value;
            return objectMapper.readValue(payload, OrderCanceledMessage.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("order.canceled 파싱 실패: " + value, e);
        }
    }
}
