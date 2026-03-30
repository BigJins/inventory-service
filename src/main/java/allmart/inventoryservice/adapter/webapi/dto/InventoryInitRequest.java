package allmart.inventoryservice.adapter.webapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryInitRequest(
        @NotNull Long productId,
        @Min(0) int quantity
) {}
