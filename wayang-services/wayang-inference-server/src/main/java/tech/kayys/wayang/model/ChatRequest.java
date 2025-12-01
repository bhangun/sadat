package tech.kayys.wayang.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// ChatRequest
public record ChatRequest(
    List<ChatMessage> messages,
    @JsonProperty("max_tokens") Integer maxTokens,
    Float temperature,
    @JsonProperty("top_p") Float topP,
    @JsonProperty("top_k") Integer topK,
    @JsonProperty("min_p") Float minP,
    @JsonProperty("repeat_penalty") Float repeatPenalty,
    @JsonProperty("presence_penalty") Float presencePenalty,
    @JsonProperty("frequency_penalty") Float frequencyPenalty,
    Boolean stream,
    List<String> stop
) {}