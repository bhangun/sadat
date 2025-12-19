package tech.kayys.wayang.sdk.dto;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Request to cancel a workflow
 */
public record CancelWorkflowRequest(
    String reason
) {}
