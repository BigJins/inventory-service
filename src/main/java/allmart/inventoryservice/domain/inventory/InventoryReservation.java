package allmart.inventoryservice.domain.inventory;

import allmart.inventoryservice.domain.AbstractEntity;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주문 1건의 단일 상품에 대한 재고 예약 내역.
 * 하나의 주문이 N개 상품을 포함하면 N개의 InventoryReservation이 생성된다.
 *
 * tossOrderId로 그룹화하여 confirm/release를 일괄 처리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryReservation extends AbstractEntity {

    private String tossOrderId;

    private Long productId;

    private int quantity;

    private ReservationStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime expiredAt;

    public static InventoryReservation pending(String tossOrderId, Long productId, int quantity) {
        InventoryReservation r = new InventoryReservation();
        r.tossOrderId = tossOrderId;
        r.productId = productId;
        r.quantity = quantity;
        r.status = ReservationStatus.PENDING;
        r.createdAt = LocalDateTime.now();
        r.expiredAt = r.createdAt.plusMinutes(30); // 30분 TTL
        return r;
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void release() {
        this.status = ReservationStatus.RELEASED;
    }
}