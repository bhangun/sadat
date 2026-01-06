package tech.kayys.wayang.workflow.kernel;

import java.util.Map;

/**
 * Resource requirements for node execution
 */
public class ResourceRequirements {

    private final ResourceEstimate minimum;
    private final ResourceEstimate recommended;
    private final ResourceEstimate maximum;
    private final Map<String, String> constraints;
    private final boolean guaranteed;

    public ResourceRequirements(ResourceEstimate minimum, ResourceEstimate recommended,
            ResourceEstimate maximum, Map<String, String> constraints,
            boolean guaranteed) {
        this.minimum = minimum;
        this.recommended = recommended;
        this.maximum = maximum;
        this.constraints = Map.copyOf(constraints);
        this.guaranteed = guaranteed;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public ResourceEstimate getMinimum() {
        return minimum;
    }

    public ResourceEstimate getRecommended() {
        return recommended;
    }

    public ResourceEstimate getMaximum() {
        return maximum;
    }

    public Map<String, String> getConstraints() {
        return constraints;
    }

    public boolean isGuaranteed() {
        return guaranteed;
    }

    public boolean canFitIn(ResourceEstimate available) {
        return this.minimum.canFitIn(available);
    }

    public ResourceRequirements scale(double factor) {
        return new ResourceRequirements(
                this.minimum.multiply(factor),
                this.recommended.multiply(factor),
                this.maximum.multiply(factor),
                this.constraints,
                this.guaranteed);
    }

    public static class Builder {
        private ResourceEstimate minimum;
        private ResourceEstimate recommended;
        private ResourceEstimate maximum;
        private Map<String, String> constraints;
        private boolean guaranteed;

        public Builder minimum(ResourceEstimate minimum) {
            this.minimum = minimum;
            return this;
        }

        public Builder recommended(ResourceEstimate recommended) {
            this.recommended = recommended;
            return this;
        }

        public Builder maximum(ResourceEstimate maximum) {
            this.maximum = maximum;
            return this;
        }

        public Builder constraints(Map<String, String> constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder guaranteed(boolean guaranteed) {
            this.guaranteed = guaranteed;
            return this;
        }

        public ResourceRequirements build() {
            return new ResourceRequirements(minimum, recommended, maximum, constraints, guaranteed);
        }
    }
}