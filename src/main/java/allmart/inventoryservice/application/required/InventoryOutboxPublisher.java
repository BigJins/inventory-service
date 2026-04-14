package allmart.inventoryservice.application.required;

/** 재고 처리 결과 Outbox 발행 포트 */
public interface InventoryOutboxPublisher {

    /** 재고 부족으로 예약 실패 → order.reserve.failed.v1 Outbox 저장 */
    void publishReserveFailed(String tossOrderId, String orderId);
}
