package tech.kayys.wayang.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CompletionRequest(
    String prompt,
    @JsonProperty("max_tokens") Integer maxTokens,
    Float temperature,
    @JsonProperty("top_p") Float topP,
    @JsonProperty("top_k") Integer topK,
    Boolean stream,
    List<String> stop
) {}