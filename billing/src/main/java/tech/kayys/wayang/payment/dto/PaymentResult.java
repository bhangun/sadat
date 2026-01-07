package tech.kayys.wayang.payment.dto;

public record PaymentResult(boolean success, String message, String transactionId) {
}