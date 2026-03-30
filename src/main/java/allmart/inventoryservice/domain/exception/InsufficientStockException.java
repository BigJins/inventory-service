package allmart.inventoryservice.domain.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, int requested, int available) {
        super("재고 부족 — productId=" + productId
                + ", 요청=" + requested
                + ", 가용=" + available);
    }
}