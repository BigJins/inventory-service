package allmart.inventoryservice.adapter.kafka.dto;

import java.util.List;

/** order.created.v1 Kafka 메시지 — 재고 예약에 필요한 필드만 포함 */
public record OrderCreatedMessage(
        String orderId,
        String tossOrderId,
        List<OrderLineDto> orderLines
) {
    public record OrderLineDto(String productId, int quantity) {}
}
