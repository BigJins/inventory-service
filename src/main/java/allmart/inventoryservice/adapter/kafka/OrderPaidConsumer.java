package allmart.inventoryservice.adapter.kafka;

import allmart.inventoryservice.adapter.kafka.dto.OrderPaidMessage;
import allmart.inventoryservice.application.provided.InventoryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * order.paid.v1 Kafka 컨슈머.
 * 후불 결제(CASH_ON_DELIVERY, CARD_ON_DELIVERY): PENDING → CONFIRMED.
 * 카드 결제: payment.result.v1에서 이미 처리됐으면 멱등 no-op.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryManager inventoryManager;

    @KafkaListener(
            topics = "${kafka.topics.order-paid}",
            groupId = "${kafka.consumer.group-id:inventory-service}"
    )
    public void onMessage(String value) {
        OrderPaidMessage msg = parseMessage(value);
        log.info("order.paid 수신 → 재고 확정(멱등): orderId={}, tossOrderId={}", msg.orderId(), msg.tossOrderId());
        inventoryManager.confirm(msg.tossOrderId());
    }

    /** Debezium EventRouter envelope 대응 */
    private OrderPaidMessage parseMessage(String value) {
        try {
            var root = objectMapper.readTree(value);
            String payload = root.has("payload") ? root.get("payload").asText() : value;
            return objectMapper.readValue(payload, OrderPaidMessage.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("order.paid 파싱 실패: " + value, e);
        }
    }
}
