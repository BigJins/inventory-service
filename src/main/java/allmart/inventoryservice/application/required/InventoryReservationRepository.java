package allmart.inventoryservice.application.required;

import allmart.inventoryservice.domain.inventory.InventoryReservation;
import allmart.inventoryservice.domain.inventory.ReservationStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryReservationRepository extends Repository<InventoryReservation, Long> {

    InventoryReservation save(InventoryReservation reservation);

    List<InventoryReservation> findByTossOrderIdAndStatus(String tossOrderId, ReservationStatus status);

    List<InventoryReservation> findByStatus(ReservationStatus status);

    // TTL 만료된 PENDING 예약 조회 (스케줄러 사용)
    @Query("SELECT r FROM InventoryReservation r WHERE r.status = :status AND r.expiredAt < :now")
    List<InventoryReservation> findExpired(ReservationStatus status, LocalDateTime now);
}