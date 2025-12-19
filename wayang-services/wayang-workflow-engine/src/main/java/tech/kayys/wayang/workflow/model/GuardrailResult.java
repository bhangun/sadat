package tech.kayys.wayang.workflow.model;

import java.util.Map;

/**
 * Result classes.
 */
@lombok.Data
@lombok.Builder
public class GuardrailResult {
    private final boolean allowed;
    private final String reason;
    private final Map<String, Object> metadata;

    public static GuardrailResult allow() {
        return GuardrailResult.builder()
                .allowed(true)
                .build();
    }

    public static GuardrailResult block(String reason) {
        return GuardrailResult.builder()
                .allowed(false)
                .reason(reason)
                .build();
    }
}
