package tech.kayys.wayang.sdk.dto;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;





/**
 * Workflow definition response
 */
public record WorkflowDefinitionResponse(
    String id,
    String name,
    String version,
    String status,
    Instant createdAt,
    Instant updatedAt,
    String createdBy
) {}
