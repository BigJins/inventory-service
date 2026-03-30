package allmart.inventoryservice.domain.inventory;

public enum ReservationStatus {
    PENDING,    // 예약 중 (결제 대기)
    CONFIRMED,  // 확정 (결제 완료 → 재고 차감)
    RELEASED    // 해제 (결제 실패/취소 → 재고 복구)
}