package allmart.inventoryservice.adapter.kafka;

import allmart.inventoryservice.adapter.kafka.dto.PaymentResultMessage;
import allmart.inventoryservice.application.provided.InventoryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * payment.result.v1 Kafka 컨슈머.
 * DONE → 재고 확정 (PENDING → CONFIRMED).
 * FAILED → 재고 해제 (PENDING → RELEASED, Redis 복구).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryManager inventoryManager;

    @KafkaListener(
            topics = "${kafka.topics.payment-result}",
            groupId = "${kafka.consumer.group-id:inventory-service}"
    )
    public void onMessage(String value) {
        PaymentResultMessage msg = parseMessage(value);
        log.info("payment.result 수신: tossOrderId={}, status={}", msg.tossOrderId(), msg.isFailed() ? "FAILED" : "DONE");

        if (msg.isFailed()) {
            inventoryManager.release(msg.tossOrderId());
        } else {
            inventoryManager.confirm(msg.tossOrderId());
        }
    }

    /** Debezium EventRouter envelope 대응 — payload 필드가 있으면 내부 JSON 파싱 */
    private PaymentResultMessage parseMessage(String value) {
        try {
            var root = objectMapper.readTree(value);
            String payload = root.has("payload") ? root.get("payload").asText() : value;
            return objectMapper.readValue(payload, PaymentResultMessage.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("payment.result 파싱 실패: " + value, e);
        }
    }
}