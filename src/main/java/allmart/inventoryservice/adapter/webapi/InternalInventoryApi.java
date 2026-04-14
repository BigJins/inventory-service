package allmart.inventoryservice.adapter.webapi;

import allmart.inventoryservice.adapter.webapi.dto.InventoryInitRequest;
import allmart.inventoryservice.application.provided.InventoryManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 내부 서비스 전용 API — Gateway에서 /internal/** 경로를 외부 차단한다.
 *
 * 모든 재고 상태 전이는 Kafka 이벤트 소비로 처리:
 *   - reserve:  order.created.v1  (OrderCreatedConsumer)
 *   - confirm:  order.paid.v1     (OrderPaidConsumer)
 *   - release(결제실패): payment.result.v1 FAILED  (PaymentResultConsumer)
 *   - release(취소):     order.canceled.v1          (OrderCanceledConsumer)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/inventory")
public class InternalInventoryApi {

    private final InventoryManager inventoryManager;

    /** 상품 등록 시 재고 초기화 (product-service 호출 — 추후 product.registered.v1 Kafka로 전환 예정) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void initialize(@RequestBody @Valid InventoryInitRequest request) {
        inventoryManager.initialize(request.productId(), request.quantity());
    }
}
