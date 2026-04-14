package allmart.inventoryservice.adapter.kafka;

import allmart.inventoryservice.adapter.kafka.dto.OrderCreatedMessage;
import allmart.inventoryservice.application.provided.InventoryManager;
import allmart.inventoryservice.application.required.InventoryOutboxPublisher;
import allmart.inventoryservice.domain.exception.InsufficientStockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * order.created.v1 Kafka 컨슈머.
 * 주문 생성 → 재고 예약 (AVAILABLE → RESERVED).
 * 재고 부족 시 order.reserve.failed.v1 Outbox 저장 → CDC → order-service 주문 자동 취소.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryManager inventoryManager;
    private final InventoryOutboxPublisher outboxPublisher;

    @KafkaListener(
            topics = "${kafka.topics.order-created}",
            groupId = "${kafka.consumer.group-id:inventory-service}"
    )
    public void onMessage(String value) {
        OrderCreatedMessage msg = parseMessage(value);
        List<InventoryManager.ReserveItem> items = msg.orderLines().stream()
                .map(l -> new InventoryManager.ReserveItem(Long.parseLong(l.productId()), l.quantity()))
                .toList();
        try {
            inventoryManager.reserve(msg.tossOrderId(), items);
            log.info("재고 예약 완료: orderId={}, tossOrderId={}", msg.orderId(), msg.tossOrderId());
        } catch (InsufficientStockException e) {
            log.warn("재고 부족 → 주문 취소 이벤트 발행: orderId={}, tossOrderId={}", msg.orderId(), msg.tossOrderId());
            outboxPublisher.publishReserveFailed(msg.tossOrderId(), msg.orderId());
        }
    }

    /** Debezium EventRouter envelope 대응 */
    private OrderCreatedMessage parseMessage(String value) {
        try {
            var root = objectMapper.readTree(value);
            String payload = root.has("payload") ? root.get("payload").asText() : value;
            return objectMapper.readValue(payload, OrderCreatedMessage.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("order.created 파싱 실패: " + value, e);
        }
    }
}
