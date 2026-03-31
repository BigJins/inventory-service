package allmart.inventoryservice.config;

import allmart.inventoryservice.application.required.InventoryRepository;
import allmart.inventoryservice.application.required.InventoryReservationRepository;
import allmart.inventoryservice.domain.inventory.InventoryReservation;
import allmart.inventoryservice.domain.inventory.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 시작 시 DB availableQuantity → Redis 동기화.
 * Redis 재시작 또는 inventory-service 재배포 시 Redis 카운터가 초기화되는 문제 방지.
 *
 * 계산: Redis 재고 = DB.availableQuantity - PENDING 예약 합계
 * (reserve 시 Redis만 차감하고 DB availableQuantity는 변경하지 않으므로)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStockInitializer implements ApplicationRunner {

    private static final String STOCK_KEY = "inv:stock:";

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<allmart.inventoryservice.domain.inventory.Inventory> inventories = inventoryRepository.findAll();
        if (inventories.isEmpty()) {
            log.info("재고 데이터 없음 — Redis 초기화 skip");
            return;
        }

        // PENDING 예약 수량 합산 (productId → 예약 수량 합계)
        Map<Long, Integer> pendingQty = reservationRepository
                .findByStatus(ReservationStatus.PENDING)
                .stream()
                .collect(Collectors.groupingBy(
                        InventoryReservation::getProductId,
                        Collectors.summingInt(InventoryReservation::getQuantity)
                ));

        for (allmart.inventoryservice.domain.inventory.Inventory inv : inventories) {
            int reserved = pendingQty.getOrDefault(inv.getProductId(), 0);
            int available = inv.getAvailableQuantity() - reserved;
            redisTemplate.opsForValue().set(STOCK_KEY + inv.getProductId(), String.valueOf(available));
            log.info("Redis 재고 초기화: productId={}, available={}", inv.getProductId(), available);
        }

        log.info("Redis 재고 초기화 완료: {}개 상품", inventories.size());
    }
}
