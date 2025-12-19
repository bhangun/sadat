package tech.kayys.wayang.sdk.exception;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Thrown when run is not found
 */
public class RunNotFoundException extends WorkflowSDKException {
    public RunNotFoundException(String runId) {
        super("Workflow run not found: " + runId, "RUN_NOT_FOUND");
    }
}
