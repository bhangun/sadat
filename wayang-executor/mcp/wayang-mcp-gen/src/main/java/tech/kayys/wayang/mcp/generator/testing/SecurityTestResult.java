package tech.kayys.wayang.mcp.generator.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SecurityTestResult {
    private final String testName;
    private final String category;
    private final boolean success;
    private final List<String> securityIssues;
    private final String failureReason;

    private SecurityTestResult(Builder builder) {
        this.testName = builder.testName;
        this.category = builder.category;
        this.success = builder.success;
        this.securityIssues = Collections.unmodifiableList(new ArrayList<>(builder.securityIssues));
        this.failureReason = builder.failureReason;
    }

    public String getTestName() {
        return testName;
    }

    public String getCategory() {
        return category;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean hasSecurityIssues() {
        return !securityIssues.isEmpty();
    }

    public List<String> getSecurityIssues() {
        return securityIssues;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String testName;
        private String category;
        private boolean success;
        private final List<String> securityIssues = new ArrayList<>();
        private String failureReason;

        public Builder withTestName(String testName) {
            this.testName = testName;
            return this;
        }

        public Builder withCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder withSuccess(boolean success) {
            this.success = success;
            return this;
        }

        public Builder withFailureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder addSecurityIssue(String issue) {
            this.securityIssues.add(issue);
            return this;
        }

        public SecurityTestResult build() {
            return new SecurityTestResult(this);
        }
    }
}