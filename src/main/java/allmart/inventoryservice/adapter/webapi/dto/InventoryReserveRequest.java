package allmart.inventoryservice.adapter.webapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InventoryReserveRequest(
        @NotBlank String tossOrderId,
        @NotEmpty @Valid List<InventoryReserveItem> items
) {
    public InventoryReserveRequest {
        items = List.copyOf(items);
    }
}
