package tech.kayys.wayang.common.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorPayload(
    @NotNull ErrorType type,
    @NotBlank String message,
    Map<String, Object> details,
    @NotNull Boolean retryable,
    @NotBlank String originNode,
    String originRunId,
    @Min(0) Integer attempt,
    @Min(0) Integer maxAttempts,
    @NotNull Instant timestamp,
    SuggestedAction suggestedAction,
    String provenanceRef
) {
    public enum ErrorType {
        TOOL_ERROR, LLM_ERROR, NETWORK_ERROR, 
        VALIDATION_ERROR, TIMEOUT, UNKNOWN_ERROR
    }
    
    public enum SuggestedAction {
        RETRY, FALLBACK, ESCALATE, 
        HUMAN_REVIEW, ABORT, AUTO_FIX
    }
}
