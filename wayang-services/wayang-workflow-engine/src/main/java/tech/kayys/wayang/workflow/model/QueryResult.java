package tech.kayys.wayang.workflow.model;

import java.util.List;

import tech.kayys.wayang.workflow.domain.WorkflowRun;

/**
 * Query result wrapper
 */
record QueryResult(
        List<WorkflowRun> runs,
        long totalElements,
        int totalPages) {
}