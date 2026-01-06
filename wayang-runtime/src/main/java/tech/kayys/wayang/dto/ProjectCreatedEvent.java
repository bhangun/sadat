package tech.kayys.wayang.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectCreatedEvent(
        UUID projectId,
        String projectName,
        String createdBy,
        Instant timestamp) {
}
