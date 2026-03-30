package allmart.inventoryservice.adapter.webapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryReserveItem(
        @NotNull Long productId,
        @Min(1) int quantity
) {}
