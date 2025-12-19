package tech.kayys.wayang.workflow.model;

@lombok.Data
@lombok.Builder
class InjectionResult {
    private final boolean injectionDetected;
    private final String reason;
}
