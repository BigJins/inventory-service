package allmart.inventoryservice.application;

import allmart.inventoryservice.application.provided.InventoryManager;
import allmart.inventoryservice.application.required.InventoryRepository;
import allmart.inventoryservice.application.required.InventoryReservationRepository;
import allmart.inventoryservice.domain.exception.InsufficientStockException;
import allmart.inventoryservice.domain.inventory.Inventory;
import allmart.inventoryservice.domain.inventory.InventoryReservation;
import allmart.inventoryservice.domain.inventory.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService implements InventoryManager {

    private static final String STOCK_KEY = "inv:stock:";

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;

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
        redisTemplate.opsForValue().set(STOCK_KEY + productId, String.valueOf(quantity));
        log.info("재고 초기화 완료: productId={}, quantity={}", productId, quantity);
    }

    /**
     * Redis DECRBY로 원자적 재고 차감 — SELECT FOR UPDATE 락 제거.
     * Redis 키 없으면 DB에서 읽어 초기화 후 차감.
     * 하나라도 재고 부족이면 이미 차감한 항목을 INCRBY로 롤백 후 예외.
     */
    @Override
    @Transactional
    public void reserve(String tossOrderId, List<ReserveItem> items) {
        List<ReserveItem> decremented = new ArrayList<>();
        try {
            for (ReserveItem item : items) {
                String key = STOCK_KEY + item.productId();
                ensureRedisKeyExists(key, item.productId());
                Long remaining = redisTemplate.opsForValue().decrement(key, item.quantity());
                // DECRBY가 이미 실행됐으므로 실패해도 해당 항목을 롤백 대상에 포함
                decremented.add(item);
                if (remaining == null || remaining < 0) {
                    int available = remaining == null ? 0 : (int) (remaining + item.quantity());
                    throw new InsufficientStockException(item.productId(), item.quantity(), available);
                }
                reservationRepository.save(
                        InventoryReservation.pending(tossOrderId, item.productId(), item.quantity())
                );
            }
        } catch (InsufficientStockException e) {
            log.warn("재고 부족 - 예약 롤백: tossOrderId={}, 롤백 항목 수={}", tossOrderId, decremented.size());
            for (ReserveItem d : decremented) {
                redisTemplate.opsForValue().increment(STOCK_KEY + d.productId(), d.quantity());
            }
            throw e;
        }
        log.info("재고 예약 완료: tossOrderId={}, 항목 수={}", tossOrderId, items.size());
    }

    /**
     * Redis 키가 없으면 DB에서 가용 재고를 읽어 초기화.
     * (서비스 재시작 시 Redis 초기화가 실패한 경우 fallback)
     */
    private void ensureRedisKeyExists(String key, Long productId) {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            inventoryRepository.findByProductId(productId).ifPresent(inv -> {
                int pendingReserved = reservationRepository.findByStatus(ReservationStatus.PENDING).stream()
                        .filter(r -> r.getProductId().equals(productId))
                        .mapToInt(InventoryReservation::getQuantity)
                        .sum();
                int available = inv.getAvailableQuantity() - pendingReserved;
                redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(available));
                log.info("Redis 키 fallback 초기화: productId={}, available={}", productId, available);
            });
        }
    }

    /**
     * 결제 완료 — PENDING 예약을 CONFIRMED로 전환 (Redis는 reserve 시 이미 차감됨).
     */
    @Override
    @Transactional
    public void confirm(String tossOrderId) {
        List<InventoryReservation> pending =
                reservationRepository.findByTossOrderIdAndStatus(tossOrderId, ReservationStatus.PENDING);
        for (InventoryReservation reservation : pending) {
            reservation.confirm();
        }
        log.info("재고 확정 완료: tossOrderId={}, 확정 항목 수={}", tossOrderId, pending.size());
    }

    /**
     * 결제 실패/취소 — PENDING 예약을 RELEASED로 전환하고 Redis 재고 복구.
     */
    @Override
    @Transactional
    public void release(String tossOrderId) {
        List<InventoryReservation> pending =
                reservationRepository.findByTossOrderIdAndStatus(tossOrderId, ReservationStatus.PENDING);
        for (InventoryReservation reservation : pending) {
            redisTemplate.opsForValue().increment(STOCK_KEY + reservation.getProductId(), reservation.getQuantity());
            reservation.release();
        }
        log.warn("재고 예약 해제 (결제 실패/취소): tossOrderId={}, 해제 항목 수={}", tossOrderId, pending.size());
    }

    @Override
    @Transactional
    public void addStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> inventoryRepository.save(Inventory.initialize(productId, 0)));
        inventory.addStock(quantity);
        redisTemplate.opsForValue().increment(STOCK_KEY + productId, quantity);
        log.info("재고 추가: productId={}, 추가량={}, 이후 DB 재고={}", productId, quantity, inventory.getAvailableQuantity());
    }
}