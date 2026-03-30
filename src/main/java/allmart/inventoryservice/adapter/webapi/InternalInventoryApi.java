package allmart.inventoryservice.adapter.webapi;

import allmart.inventoryservice.adapter.webapi.dto.InventoryInitRequest;
import allmart.inventoryservice.adapter.webapi.dto.InventoryReserveRequest;
import allmart.inventoryservice.application.provided.InventoryManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 내부 서비스 전용 API — Gateway에서 /internal/** 경로를 외부 차단한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/inventory")
public class InternalInventoryApi {

    private final InventoryManager inventoryManager;

    /**
     * 상품 등록 시 재고 초기화 (product-service 호출)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void initialize(@RequestBody @Valid InventoryInitRequest request) {
        inventoryManager.initialize(request.productId(), request.quantity());
    }

    /**
     * 주문 생성 시 재고 예약 (order-service 호출)
     */
    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.OK)
    public void reserve(@RequestBody @Valid InventoryReserveRequest request) {
        inventoryManager.reserve(
                request.tossOrderId(),
                request.items().stream()
                        .map(item -> new InventoryManager.ReserveItem(item.productId(), item.quantity()))
                        .toList()
        );
    }

    /**
     * 결제 완료 시 재고 예약 확정 (order-service 호출)
     */
    @PostMapping("/confirm/{tossOrderId}")
    @ResponseStatus(HttpStatus.OK)
    public void confirm(@PathVariable String tossOrderId) {
        inventoryManager.confirm(tossOrderId);
    }

    /**
     * 결제 실패/취소 시 재고 예약 해제 (order-service 호출)
     */
    @PostMapping("/release/{tossOrderId}")
    @ResponseStatus(HttpStatus.OK)
    public void release(@PathVariable String tossOrderId) {
        inventoryManager.release(tossOrderId);
    }
}
