package tech.kayys.wayang.sdk.dto;

import java.util.Map;

public record EdgeDefinitionDto(
    String id,
    String source,
    String target,
    String type,
    Map<String, Object> configuration
) {}
