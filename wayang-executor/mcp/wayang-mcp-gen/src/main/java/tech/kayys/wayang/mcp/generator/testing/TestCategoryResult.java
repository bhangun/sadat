package tech.kayys.wayang.mcp.generator.testing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestCategoryResult {
    private final String category;
    private final long startTime;
    private final long endTime;
    private final int testsRun;
    private final int testsPassed;
    private final int testsFailed;
    private final List<String> failures;
    private final boolean success;
    private final Map<String, Object> metadata;

    private TestCategoryResult(Builder builder) {
        this.category = builder.category;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.testsRun = builder.testsRun;
        this.testsPassed = builder.testsPassed;
        this.testsFailed = builder.testsFailed;
        this.failures = builder.failures;
        this.success = builder.success;
        this.metadata = new HashMap<>(builder.metadata);
    }

    public String getCategory() {
        return category;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getTestsRun() {
        return testsRun;
    }

    public int getTestsPassed() {
        return testsPassed;
    }

    public int getTestsFailed() {
        return testsFailed;
    }

    public List<String> getFailures() {
        return failures;
    }

    public boolean isSuccess() {
        return success;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String category;
        private long startTime;
        private long endTime;
        private int testsRun;
        private int testsPassed;
        private int testsFailed;
        private List<String> failures;
        private boolean success;
        private final Map<String, Object> metadata = new HashMap<>();

        public Builder withCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder withStartTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withEndTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder withTestsRun(int testsRun) {
            this.testsRun = testsRun;
            return this;
        }

        public Builder withTestsPassed(int testsPassed) {
            this.testsPassed = testsPassed;
            return this;
        }

        public Builder withTestsFailed(int testsFailed) {
            this.testsFailed = testsFailed;
            return this;
        }

        public Builder withFailures(List<String> failures) {
            this.failures = failures;
            return this;
        }

        public Builder withSuccess(boolean success) {
            this.success = success;
            return this;
        }

        public TestCategoryResult build() {
            return new TestCategoryResult(this);
        }
    }
}