package tech.kayys.wayang.billing.dto;

public record ReviewRequest(
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(5)
    int rating,
    String title,
    String comment
) {}