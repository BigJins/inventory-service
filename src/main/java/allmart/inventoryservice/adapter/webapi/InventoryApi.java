package allmart.inventoryservice.adapter.webapi;

import allmart.inventoryservice.adapter.webapi.dto.InventoryAddStockRequest;
import allmart.inventoryservice.adapter.webapi.dto.InventorySummaryResponse;
import allmart.inventoryservice.application.provided.InventoryManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 판매자 앱용 재고 조회/관리 API
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryApi {

    private final InventoryManager inventoryManager;

    @GetMapping
    public List<InventorySummaryResponse> findAll() {
        return inventoryManager.findAll().stream()
                .map(InventorySummaryResponse::of)
                .toList();
    }

    /**
     * 판매자 입고 — 가용 재고 추가 (MEMBER 전용, Gateway에서 인가)
     */
    @PatchMapping("/{productId}/stock")
    public void addStock(@PathVariable Long productId,
                         @RequestBody @Valid InventoryAddStockRequest request) {
        inventoryManager.addStock(productId, request.quantity());
    }
}
