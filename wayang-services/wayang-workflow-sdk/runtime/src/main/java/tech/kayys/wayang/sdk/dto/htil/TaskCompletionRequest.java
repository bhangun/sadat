package tech.kayys.wayang.sdk.dto.htil;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Task completion request
 */
public record TaskCompletionRequest(
    TaskAction action,
    Map<String, Object> data,
    String notes,
    String completedBy
) {}
