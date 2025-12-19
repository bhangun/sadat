package tech.kayys.wayang.workflow.model.saga;

import java.util.List;

/**
 * Compensation Strategy
 */
public record CompensationStrategy(
        CompensationType type,
        List<String> nodesToCompensate) {

    public static final CompensationStrategy BACKWARD = null;
}