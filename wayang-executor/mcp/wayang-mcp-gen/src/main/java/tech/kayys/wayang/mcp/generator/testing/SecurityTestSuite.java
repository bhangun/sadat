package tech.kayys.wayang.mcp.generator.testing;

import tech.kayys.wayang.mcp.model.ApiOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SecurityTestSuite {
    private final String name;
    private final String description;
    private final List<SecurityTest> tests;

    private SecurityTestSuite(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.tests = Collections.unmodifiableList(new ArrayList<>(builder.tests));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<SecurityTest> getTests() {
        return tests;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private final List<SecurityTest> tests = new ArrayList<>();

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder addTest(SecurityTest test) {
            this.tests.add(test);
            return this;
        }

        public SecurityTestSuite build() {
            return new SecurityTestSuite(this);
        }
    }
}