package tech.kayys.wayang.workflow.saga.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Saga execution snapshot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaExecutionSnapshot {
    private String sagaId;
    private List<String> completedSteps;
    private List<String> compensatedSteps;
    private String status; // NONE, IN_PROGRESS, COMPLETED, FAILED
}
