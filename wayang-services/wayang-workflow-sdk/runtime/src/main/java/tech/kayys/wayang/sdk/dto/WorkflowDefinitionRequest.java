package tech.kayys.wayang.sdk.dto;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Workflow definition request
 */
public record WorkflowDefinitionRequest(
    String id,
    String name,
    String description,
    String version,
    String tenantId,
    List<NodeDefinitionDto> nodes,
    List<EdgeDefinitionDto> edges,
    Map<String, Object> metadata
) {}
