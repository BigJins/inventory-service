package allmart.inventoryservice.adapter.scheduler;

import allmart.inventoryservice.application.required.InventoryRepository;
import allmart.inventoryservice.application.required.InventoryReservationRepository;
import allmart.inventoryservice.domain.inventory.Inventory;
import allmart.inventoryservice.domain.inventory.InventoryReservation;
import allmart.inventoryservice.domain.inventory.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재고 예약 TTL 만료 스케줄러
 *
 * 주문 생성 후 30분 이내 결제가 완료되지 않으면 PENDING 예약을 자동 해제.
 * → 오래된 예약이 재고를 영구적으로 잠그는 문제 방지
 *
 * 실행 주기: 5분마다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTtlScheduler {

    private final InventoryReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;

    @Scheduled(fixedDelay = 300_000) // 5분
    @Transactional
    public void releaseExpiredReservations() {
        List<InventoryReservation> expired =
                reservationRepository.findExpired(ReservationStatus.PENDING, LocalDateTime.now());

        if (expired.isEmpty()) return;

        log.info("TTL 만료 예약 해제 시작: {}건", expired.size());

        for (InventoryReservation reservation : expired) {
            inventoryRepository.findByProductId(reservation.getProductId()).ifPresent(inventory -> {
                inventory.release(reservation.getQuantity());
                log.info("TTL 만료 예약 해제: tossOrderId={}, productId={}, qty={}",
                        reservation.getTossOrderId(), reservation.getProductId(), reservation.getQuantity());
            });
            reservation.release();
        }

        log.info("TTL 만료 예약 해제 완료: {}건", expired.size());
    }
}
