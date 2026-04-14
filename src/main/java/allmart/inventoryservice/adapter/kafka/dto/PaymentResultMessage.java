package allmart.inventoryservice.adapter.kafka.dto;

/** payment.result.v1 Kafka 메시지 — pay-service가 Debezium CDC를 통해 발행 */
public record PaymentResultMessage(
        String tossOrderId,
        long amount,
        String paymentKey,
        String result
) {
    /** FAILED 또는 PAYMENT_FAILED이면 결제 실패 */
    public boolean isFailed() {
        return result != null &&
                (result.equalsIgnoreCase("FAILED") || result.equalsIgnoreCase("PAYMENT_FAILED"));
    }
}
