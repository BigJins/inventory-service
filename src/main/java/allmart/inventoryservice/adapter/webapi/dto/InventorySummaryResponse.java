package allmart.inventoryservice.adapter.webapi.dto;

import allmart.inventoryservice.domain.inventory.Inventory;

public record InventorySummaryResponse(
        Long productId,
        int availableQuantity,
        int reservedQuantity
) {
    public static InventorySummaryResponse of(Inventory inv) {
        return new InventorySummaryResponse(
                inv.getProductId(),
                inv.getAvailableQuantity(),
                inv.getReservedQuantity()
        );
    }
}
