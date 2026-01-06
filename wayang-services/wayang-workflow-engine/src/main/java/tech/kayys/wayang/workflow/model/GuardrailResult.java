package tech.kayys.wayang.workflow.model;

import java.util.Map;

/**
 * Result classes.
 */
public class GuardrailResult {
    private final boolean allowed;
    private final String reason;
    private final Map<String, Object> metadata;

    private GuardrailResult(boolean allowed, String reason, Map<String, Object> metadata) {
        this.allowed = allowed;
        this.reason = reason;
        this.metadata = metadata;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getReason() {
        return reason;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static GuardrailResultBuilder builder() {
        return new GuardrailResultBuilder();
    }

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

    public static class GuardrailResultBuilder {
        private boolean allowed;
        private String reason;
        private Map<String, Object> metadata;

        public GuardrailResultBuilder allowed(boolean allowed) {
            this.allowed = allowed;
            return this;
        }

        public GuardrailResultBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public GuardrailResultBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public GuardrailResult build() {
            return new GuardrailResult(allowed, reason, metadata);
        }
    }
}
