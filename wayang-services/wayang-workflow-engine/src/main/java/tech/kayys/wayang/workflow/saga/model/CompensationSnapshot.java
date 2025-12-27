package tech.kayys.wayang.workflow.saga.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compensation snapshot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompensationSnapshot {
    private boolean required;
    private List<String> nodesToCompensate;
    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED
}