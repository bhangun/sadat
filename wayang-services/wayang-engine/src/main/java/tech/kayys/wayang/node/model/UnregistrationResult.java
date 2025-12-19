package tech.kayys.wayang.node.model;

/**
 * Unregistration result.
 */
@lombok.Data
@lombok.Builder
class UnregistrationResult {
    private String status;
    private String message;

    public static UnregistrationResult success() {
        return UnregistrationResult.builder()
                .status("SUCCESS")
                .build();
    }

    public static UnregistrationResult failed(String message) {
        return UnregistrationResult.builder()
                .status("FAILED")
                .message(message)
                .build();
    }
}
