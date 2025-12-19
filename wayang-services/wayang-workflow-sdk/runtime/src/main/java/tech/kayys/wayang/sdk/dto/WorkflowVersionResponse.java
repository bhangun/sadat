package tech.kayys.wayang.sdk.dto;

import java.time.Instant;

public record WorkflowVersionResponse(
    String version,
    Instant createdAt,
    String createdBy
) {}
