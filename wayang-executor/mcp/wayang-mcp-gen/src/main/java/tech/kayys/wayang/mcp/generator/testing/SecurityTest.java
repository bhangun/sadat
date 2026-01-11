package tech.kayys.wayang.mcp.generator.testing;

import tech.kayys.wayang.mcp.model.ApiOperation;

public class SecurityTest {
    private final String name;
    private final String description;
    private final String category;
    private final String testCode;
    private final ApiOperation operation;

    private SecurityTest(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.category = builder.category;
        this.testCode = builder.testCode;
        this.operation = builder.operation;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getTestCode() {
        return testCode;
    }

    public ApiOperation getOperation() {
        return operation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private String category;
        private String testCode;
        private ApiOperation operation;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder withTestCode(String testCode) {
            this.testCode = testCode;
            return this;
        }

        public Builder withOperation(ApiOperation operation) {
            this.operation = operation;
            return this;
        }

        public SecurityTest build() {
            return new SecurityTest(this);
        }
    }
}