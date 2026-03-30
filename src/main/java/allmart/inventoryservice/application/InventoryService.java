package allmart.inventoryservice.application;

import allmart.inventoryservice.application.provided.InventoryManager;
import allmart.inventoryservice.application.required.InventoryRepository;
import allmart.inventoryservice.application.required.InventoryReservationRepository;
import allmart.inventoryservice.domain.inventory.Inventory;
import allmart.inventoryservice.domain.inventory.InventoryReservation;
import allmart.inventoryservice.domain.inventory.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService implements InventoryManager {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> findAll() {
        return inventoryRepository.findAll();
    }

    @Override
    @Transactional
    public void initialize(Long productId, int quantity) {
        inventoryRepository.findByProductId(productId).ifPresent(existing -> {
            throw new IllegalStateException("이미 재고가 초기화된 상품입니다: " + productId);
        });
        inventoryRepository.save(Inventory.initialize(productId, quantity));
    }

    /**
     * 전체 상품 예약을 단일 트랜잭션으로 처리.
     * 비관적 락(SELECT FOR UPDATE)으로 동시 예약 시 oversell 방지.
     * 하나라도 재고 부족이면 예외 발생 → 트랜잭션 롤백 → 전체 취소.
     */
    @Override
    @Transactional
    public void reserve(String tossOrderId, List<ReserveItem> items) {
        for (ReserveItem item : items) {
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("재고 정보 없음: productId=" + item.productId()));

            inventory.reserve(item.quantity());

            reservationRepository.save(
                    InventoryReservation.pending(tossOrderId, item.productId(), item.quantity())
            );
        }
    }

    /**
     * 결제 완료 — PENDING 예약을 CONFIRMED로 전환.
     * 멱등: 이미 CONFIRMED인 경우 skip.
     */
    @Override
    @Transactional
    public void confirm(String tossOrderId) {
        List<InventoryReservation> pending =
                reservationRepository.findByTossOrderIdAndStatus(tossOrderId, ReservationStatus.PENDING);

        for (InventoryReservation reservation : pending) {
            Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new IllegalStateException("재고 정보 없음: productId=" + reservation.getProductId()));

            inventory.confirm(reservation.getQuantity());
            reservation.confirm();
        }
    }

    /**
     * 결제 실패/취소 — PENDING 예약을 RELEASED로 전환하고 가용 재고 복구.
     * 멱등: 이미 RELEASED인 경우 skip.
     */
    @Override
    @Transactional
    public void release(String tossOrderId) {
        List<InventoryReservation> pending =
                reservationRepository.findByTossOrderIdAndStatus(tossOrderId, ReservationStatus.PENDING);

        for (InventoryReservation reservation : pending) {
            Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new IllegalStateException("재고 정보 없음: productId=" + reservation.getProductId()));

            inventory.release(reservation.getQuantity());
            reservation.release();
        }
    }

    @Override
    @Transactional
    public void addStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("재고 정보 없음: productId=" + productId));
        inventory.addStock(quantity);
    }
}
