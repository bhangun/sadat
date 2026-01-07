package tech.kayys.wayang.billing.dto;

import java.util.Map;

public record PaymentRequest(
    String paymentMethodId,
    Map<String, String> metadata
) {}
