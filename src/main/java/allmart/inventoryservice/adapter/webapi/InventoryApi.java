package allmart.inventoryservice.adapter.webapi;

import allmart.inventoryservice.adapter.webapi.dto.InventorySummaryResponse;
import allmart.inventoryservice.application.provided.InventoryManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 판매자 앱용 재고 조회 API
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
}
