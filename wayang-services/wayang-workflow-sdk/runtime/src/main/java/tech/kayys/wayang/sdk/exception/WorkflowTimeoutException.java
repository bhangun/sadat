package tech.kayys.wayang.sdk.exception;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Thrown when operation times out
 */
public class WorkflowTimeoutException extends WorkflowSDKException {
    public WorkflowTimeoutException(String runId, long timeoutMs) {
        super(
            String.format("Workflow execution timed out after %dms", timeoutMs),
            "WORKFLOW_TIMEOUT",
            Map.of("runId", runId, "timeoutMs", timeoutMs)
        );
    }
}
