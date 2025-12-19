package tech.kayys.wayang.sdk.dto;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Paginated list of workflow runs
 */
public record WorkflowRunListResponse(
    List<WorkflowRunResponse> runs,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {
    public boolean isEmpty() {
        return runs.isEmpty();
    }
}
