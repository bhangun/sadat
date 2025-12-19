package tech.kayys.wayang.sdk.exception;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Thrown when operation is invalid for current run state
 */
public class InvalidRunStateException extends WorkflowSDKException {
    public InvalidRunStateException(String runId, String currentState, String requiredState) {
        super(
            String.format("Invalid state for operation. Current: %s, Required: %s", 
                currentState, requiredState),
            "INVALID_RUN_STATE",
            Map.of("runId", runId, "currentState", currentState, "requiredState", requiredState)
        );
    }
}
