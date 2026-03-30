package allmart.inventoryservice.adapter.webapi.dto;

import jakarta.validation.constraints.Min;

public record InventoryAddStockRequest(
        @Min(value = 1, message = "입고 수량은 1 이상이어야 합니다")
        int quantity
) {}
