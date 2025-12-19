package tech.kayys.wayang.sdk.exception;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Thrown when workflow is not found
 */
public class WorkflowNotFoundException extends WorkflowSDKException {
    public WorkflowNotFoundException(String workflowId) {
        super("Workflow not found: " + workflowId, "WORKFLOW_NOT_FOUND");
    }
}

