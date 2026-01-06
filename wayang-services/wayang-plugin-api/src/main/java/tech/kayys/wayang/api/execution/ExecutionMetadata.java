package tech.kayys.wayang.api.execution;

public class ExecutionMetadata {
    private final String userId;

    public ExecutionMetadata() {
        this.userId = null;
    }

    public ExecutionMetadata(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
