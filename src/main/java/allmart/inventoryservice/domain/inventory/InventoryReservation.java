package allmart.inventoryservice.domain.inventory;

import allmart.inventoryservice.domain.AbstractEntity;
import jakarta.persistence.*;
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
@Table(
        name = "inventory_reservations",
        indexes = @Index(name = "idx_reservations_toss_order_id", columnList = "toss_order_id")
)
public class InventoryReservation extends AbstractEntity {

    @Column(name = "toss_order_id", nullable = false, length = 64)
    private String tossOrderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at", nullable = false)
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