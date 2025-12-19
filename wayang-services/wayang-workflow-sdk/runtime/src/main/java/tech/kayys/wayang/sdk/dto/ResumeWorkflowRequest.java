package tech.kayys.wayang.sdk.dto;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Request to resume a waiting workflow
 */
public record ResumeWorkflowRequest(
    String humanTaskId,
    Map<String, Object> resumeData
) {}
