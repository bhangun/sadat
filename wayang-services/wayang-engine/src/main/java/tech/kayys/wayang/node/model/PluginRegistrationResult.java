package tech.kayys.wayang.node.model;

import java.util.List;

import tech.kayys.wayang.node.domain.NodeMetadata;

/**
 * Plugin registration result.
 */
@lombok.Data
@lombok.Builder
class PluginRegistrationResult {
    private String status;
    private List<String> errors;
    private NodeMetadata metadata;
    private List<String> resolvedDependencies;

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    public static PluginRegistrationResult failed(String error) {
        return PluginRegistrationResult.builder()
                .status("FAILED")
                .errors(List.of(error))
                .build();
    }

    public static PluginRegistrationResult failed(List<String> errors) {
        return PluginRegistrationResult.builder()
                .status("FAILED")
                .errors(errors)
                .build();
    }

    public static PluginRegistrationResult scanPassed() {
        return PluginRegistrationResult.builder()
                .status("SUCCESS")
                .errors(List.of())
                .build();
    }

    public static PluginRegistrationResult error(Throwable th) {
        return PluginRegistrationResult.builder()
                .status("ERROR")
                .errors(List.of(th.getMessage()))
                .build();
    }

    public PluginRegistrationResult withDependencies(List<String> deps) {
        this.resolvedDependencies = deps;
        return this;
    }

    public PluginRegistrationResult withMetadata(NodeMetadata metadata) {
        this.metadata = metadata;
        return this;
    }
}
