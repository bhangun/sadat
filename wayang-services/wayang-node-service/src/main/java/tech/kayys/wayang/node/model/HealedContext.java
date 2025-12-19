package tech.kayys.wayang.workflow.model;

import java.util.Map;

/**
 * Healed context after self-healing attempt.
 */
@lombok.Data
@lombok.Builder
class HealedContext {
    private final boolean healed;
    private final Map<String, Object> fixedInput;
    private final String repairLog;
    private final String failureReason;

    public static HealedContext success(Map<String, Object> fixedInput, String log) {
        return HealedContext.builder()
                .healed(true)
                .fixedInput(fixedInput)
                .repairLog(log)
                .build();
    }

    public static HealedContext failed(String reason) {
        return HealedContext.builder()
                .healed(false)
                .failureReason(reason)
                .build();
    }
}