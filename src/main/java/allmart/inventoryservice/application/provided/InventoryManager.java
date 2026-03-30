package allmart.inventoryservice.application.provided;

import allmart.inventoryservice.domain.inventory.Inventory;
import java.util.List;

/**
 * 재고 관리 포트 — adapter가 호출하는 유스케이스 인터페이스
 */
public interface InventoryManager {

    /**
     * 전체 재고 목록 조회.
     */
    List<Inventory> findAll();

    /**
     * 상품 재고를 초기화한다 (product-service가 상품 등록 후 호출).
     */
    void initialize(Long productId, int quantity);

    /**
     * 주문 내 전체 상품의 재고를 원자적으로 예약한다.
     * 하나라도 재고 부족 시 전체 롤백.
     *
     * @param tossOrderId 주문 식별자 (결제 결과와 매핑)
     * @param items       예약할 상품 목록
     */
    void reserve(String tossOrderId, List<ReserveItem> items);

    /**
     * 결제 완료 — 예약을 확정한다 (reservedQuantity 차감).
     */
    void confirm(String tossOrderId);

    /**
     * 결제 실패/취소 — 예약을 해제한다 (availableQuantity 복구).
     */
    void release(String tossOrderId);

    record ReserveItem(Long productId, int quantity) {}
}
