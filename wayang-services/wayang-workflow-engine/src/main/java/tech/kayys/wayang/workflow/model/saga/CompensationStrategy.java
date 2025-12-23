package tech.kayys.wayang.workflow.model.saga;

import java.util.Collections;
import java.util.List;

/**
 * Compensation Strategy
 */
public record CompensationStrategy(
        CompensationType type,
        List<String> nodesToCompensate) {

    public static final CompensationStrategy BACKWARD = new CompensationStrategy(
            CompensationType.FULL_ROLLBACK,
            Collections.emptyList());
}