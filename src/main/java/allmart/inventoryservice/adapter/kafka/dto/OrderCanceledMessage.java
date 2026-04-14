package allmart.inventoryservice.adapter.kafka.dto;

/** order.canceled.v1 Kafka 메시지 — inventory release에 필요한 필드만 포함 */
public record OrderCanceledMessage(
        Long orderId,
        String tossOrderId
) {}
