package allmart.inventoryservice.domain;

import allmart.inventoryservice.domain.exception.InsufficientStockException;
import allmart.inventoryservice.domain.inventory.Inventory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class InventoryTest {

    @Test
    @DisplayName("초기화 시 availableQuantity가 설정된다")
    void initialize_setsAvailableQuantity() {
        Inventory inv = Inventory.initialize(1L, 100);
        assertThat(inv.getAvailableQuantity()).isEqualTo(100);
        assertThat(inv.getReservedQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("음수 초기 재고는 예외가 발생한다")
    void initialize_negativeQuantity_throws() {
        assertThatThrownBy(() -> Inventory.initialize(1L, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("예약 시 availableQuantity가 줄고 reservedQuantity가 늘어난다")
    void reserve_decreasesAvailableIncreasesReserved() {
        Inventory inv = Inventory.initialize(1L, 10);
        inv.reserve(3);
        assertThat(inv.getAvailableQuantity()).isEqualTo(7);
        assertThat(inv.getReservedQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("재고 부족 시 InsufficientStockException이 발생한다")
    void reserve_insufficientStock_throws() {
        Inventory inv = Inventory.initialize(1L, 5);
        assertThatThrownBy(() -> inv.reserve(6))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("확정 시 reservedQuantity가 줄어든다")
    void confirm_decreasesReservedQuantity() {
        Inventory inv = Inventory.initialize(1L, 10);
        inv.reserve(3);
        inv.confirm(3);
        assertThat(inv.getReservedQuantity()).isEqualTo(0);
        assertThat(inv.getAvailableQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("해제 시 reservedQuantity가 줄고 availableQuantity가 복구된다")
    void release_restoresAvailableQuantity() {
        Inventory inv = Inventory.initialize(1L, 10);
        inv.reserve(4);
        inv.release(4);
        assertThat(inv.getReservedQuantity()).isEqualTo(0);
        assertThat(inv.getAvailableQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("재고 추가 시 availableQuantity가 늘어난다")
    void addStock_increasesAvailableQuantity() {
        Inventory inv = Inventory.initialize(1L, 10);
        inv.addStock(5);
        assertThat(inv.getAvailableQuantity()).isEqualTo(15);
    }
}
