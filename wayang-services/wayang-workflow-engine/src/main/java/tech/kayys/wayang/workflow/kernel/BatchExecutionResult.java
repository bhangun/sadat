package tech.kayys.wayang.workflow.kernel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Result of batch node execution
 */
public class BatchExecutionResult {

    private final String batchId;
    private final List<IndividualResult> results;
    private final BatchStatus overallStatus;
    private final Map<String, Object> batchMetadata;

    public BatchExecutionResult(String batchId, List<IndividualResult> results,
            BatchStatus overallStatus, Map<String, Object> batchMetadata) {
        this.batchId = batchId;
        this.results = List.copyOf(results);
        this.overallStatus = overallStatus;
        this.batchMetadata = Map.copyOf(batchMetadata);
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public String getBatchId() {
        return batchId;
    }

    public List<IndividualResult> getResults() {
        return results;
    }

    public BatchStatus getOverallStatus() {
        return overallStatus;
    }

    public Map<String, Object> getBatchMetadata() {
        return batchMetadata;
    }

    public List<NodeExecutionResult> getSuccessfulResults() {
        return results.stream()
                .filter(r -> r.getResult().getStatus() == NodeExecutionStatus.SUCCESS)
                .map(IndividualResult::getResult)
                .collect(Collectors.toList());
    }

    public List<FailedResult> getFailedResults() {
        return results.stream()
                .filter(r -> r.getResult().getStatus() == NodeExecutionStatus.FAILED)
                .map(r -> new FailedResult(r.getRequestId(), r.getResult(), r.getError()))
                .collect(Collectors.toList());
    }

    public List<IndividualResult> getPendingResults() {
        return results.stream()
                .filter(r -> r.getResult().getStatus() == NodeExecutionStatus.PENDING)
                .collect(Collectors.toList());
    }

    public enum BatchStatus {
        COMPLETED, // All nodes executed successfully
        PARTIAL_SUCCESS, // Some succeeded, some failed
        FAILED, // All nodes failed
        CANCELLED // Batch was cancelled
    }

    public static class IndividualResult {
        private final String requestId;
        private final NodeExecutionResult result;
        private final Throwable error;
        private final long executionTimeMs;

        public IndividualResult(String requestId, NodeExecutionResult result,
                Throwable error, long executionTimeMs) {
            this.requestId = requestId;
            this.result = result;
            this.error = error;
            this.executionTimeMs = executionTimeMs;
        }

        public String getRequestId() {
            return requestId;
        }

        public NodeExecutionResult getResult() {
            return result;
        }

        public Throwable getError() {
            return error;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public boolean isSuccess() {
            return error == null && result != null &&
                    result.getStatus() == NodeExecutionStatus.SUCCESS;
        }
    }

    public static class FailedResult {
        private final String requestId;
        private final NodeExecutionResult result;
        private final Throwable error;

        public FailedResult(String requestId, NodeExecutionResult result, Throwable error) {
            this.requestId = requestId;
            this.result = result;
            this.error = error;
        }

        public String getRequestId() {
            return requestId;
        }

        public NodeExecutionResult getResult() {
            return result;
        }

        public Throwable getError() {
            return error;
        }
    }

    public static class Builder {
        private String batchId;
        private List<IndividualResult> results;
        private BatchStatus overallStatus;
        private Map<String, Object> batchMetadata;

        public Builder batchId(String batchId) {
            this.batchId = batchId;
            return this;
        }

        public Builder results(List<IndividualResult> results) {
            this.results = results;
            return this;
        }

        public Builder overallStatus(BatchStatus overallStatus) {
            this.overallStatus = overallStatus;
            return this;
        }

        public Builder batchMetadata(Map<String, Object> batchMetadata) {
            this.batchMetadata = batchMetadata;
            return this;
        }

        public BatchExecutionResult build() {
            return new BatchExecutionResult(batchId, results, overallStatus, batchMetadata);
        }
    }
}
