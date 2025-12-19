package tech.kayys.wayang.workflow.model;

import tech.kayys.wayang.workflow.api.model.RunStatus;

/**
 * Query object for filtering runs
 */
public record WorkflowRunQuery(
                String tenantId,
                String workflowId,
                RunStatus status,
                int page,
                int size) {

        public record Result(
                        java.util.List<tech.kayys.wayang.workflow.domain.WorkflowRun> runs,
                        long totalElements,
                        int totalPages,
                        boolean hasNext) {
        }
}