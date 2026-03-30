package allmart.inventoryservice.application.required;

import allmart.inventoryservice.domain.inventory.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends Repository<Inventory, Long> {

    Inventory save(Inventory inventory);

    Optional<Inventory> findByProductId(Long productId);

    List<Inventory> findAll();

    /**
     * 재고 예약 시 비관적 쓰기 락 적용.
     * SELECT ... FOR UPDATE — 동시 예약으로 인한 oversell 방지.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdForUpdate(Long productId);
}