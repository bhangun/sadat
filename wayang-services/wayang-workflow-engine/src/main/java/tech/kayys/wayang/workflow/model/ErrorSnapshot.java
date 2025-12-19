package tech.kayys.wayang.workflow.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error snapshot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ErrorSnapshot {
    private String type;
    private String message;
    private Map<String, Object> details;
    private String stackTrace;
    private boolean retryable;
    private String suggestedAction;
}
