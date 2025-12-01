package tech.kayys.wayang.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StreamChunk(
    String id,
    String object,
    long created,
    String model,
    Delta delta,
    @JsonProperty("finish_reason") String finishReason
) {
    public StreamChunk(String id2, String string, long created2, ModelInfo modelInfo, Delta delta2,
            String finishReason2) {
        //TODO Auto-generated constructor stub
    }

    public record Delta(String role, String content) {}
}