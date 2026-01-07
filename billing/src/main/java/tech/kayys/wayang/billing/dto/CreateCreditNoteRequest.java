package tech.kayys.wayang.billing.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record CreateCreditNoteRequest(
    @NotNull BigDecimal amount,
    @NotNull String reason
) {}
