package allmart.inventoryservice.domain.inventory;

import allmart.inventoryservice.domain.AbstractEntity;
import allmart.inventoryservice.domain.exception.InsufficientStockException;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 Aggregate Root.
 * 상품 1개당 1개의 Inventory 레코드가 존재한다.
 *
 * availableQuantity: 실제 주문 가능한 수량
 * reservedQuantity:  결제 대기 중 예약된 수량 (확정 전까지 가용 수량에서 제외)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends AbstractEntity {

    private Long productId;

    private int availableQuantity;

    private int reservedQuantity;

    private LocalDateTime updatedAt;

    public static Inventory initialize(Long productId, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("초기 재고는 0 이상이어야 합니다: " + quantity);

        Inventory inv = new Inventory();
        inv.productId = productId;
        inv.availableQuantity = quantity;
        inv.reservedQuantity = 0;
        inv.updatedAt = LocalDateTime.now();
        return inv;
    }

    /**
     * 재고 예약 — availableQuantity를 줄이고 reservedQuantity를 늘린다.
     * 비관적 락이 적용된 findByProductIdForUpdate 조회 후 호출해야 한다.
     */
    public void reserve(int quantity) {
        if (availableQuantity < quantity) {
            throw new InsufficientStockException(productId, quantity, availableQuantity);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 재고 확정 — 결제 완료. reservedQuantity를 줄인다 (실제 차감).
     */
    public void confirm(int quantity) {
        reservedQuantity -= quantity;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 재고 해제 — 결제 실패/취소. reservedQuantity를 줄이고 availableQuantity를 복구.
     */
    public void release(int quantity) {
        reservedQuantity -= quantity;
        availableQuantity += quantity;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 재고 수량 추가 (관리자 입고).
     */
    public void addStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("추가 수량은 1 이상이어야 합니다: " + quantity);
        availableQuantity += quantity;
        updatedAt = LocalDateTime.now();
    }
}