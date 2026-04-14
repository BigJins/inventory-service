package allmart.inventoryservice.adapter.kafka.dto;

/** order.paid.v1 Kafka 메시지 — inventory confirm에 필요한 필드만 포함 */
public record OrderPaidMessage(
        Long orderId,
        String tossOrderId
) {}
