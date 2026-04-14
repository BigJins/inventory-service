package allmart.inventoryservice.adapter.kafka.dto;

/** order.reserve.failed.v1 Kafka 이벤트 페이로드 — order-service가 소비하여 주문 자동 취소 */
public record OrderReserveFailedPayload(
        String tossOrderId,
        String orderId
) {}
