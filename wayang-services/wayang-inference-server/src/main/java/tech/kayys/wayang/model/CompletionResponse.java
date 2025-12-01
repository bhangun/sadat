package tech.kayys.wayang.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CompletionResponse(
    String id,
    String object,
    long created,
    String model,
    Choice choice,
    Usage usage
) {
    public record Choice(
        int index,
        String text,
        @JsonProperty("finish_reason") String finishReason
    ) {}
    
    public record Usage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("completion_tokens") int completionTokens,
        @JsonProperty("total_tokens") int totalTokens
    ) {}
}
