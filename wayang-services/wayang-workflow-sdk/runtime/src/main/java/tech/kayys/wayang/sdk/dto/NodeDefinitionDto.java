package tech.kayys.wayang.sdk.dto;

import java.util.Map;

public record NodeDefinitionDto(
    String id,
    String type,
    String name,
    Map<String, Object> configuration
) {}
