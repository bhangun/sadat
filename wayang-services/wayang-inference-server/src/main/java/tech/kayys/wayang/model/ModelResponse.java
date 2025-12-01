package tech.kayys.wayang.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModelResponse(
    String id,
    String object,
    long created,
    @JsonProperty("owned_by") String ownedBy,
    ModelInfo info
) {}