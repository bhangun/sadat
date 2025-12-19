package tech.kayys.wayang.schema.node;

import java.util.Arrays;
import java.util.List;

public class ControlFlow {
    private String type;
    private String expression;
    private String aggregationStrategy;
    private Boolean parallel = false;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        List<String> validTypes = Arrays.asList("choice", "split", "aggregate", "multicast",
                "loop", "throttle", "filter");
        if (!validTypes.contains(type)) {
            throw new IllegalArgumentException("Invalid control flow type: " + type);
        }
        this.type = type;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getAggregationStrategy() {
        return aggregationStrategy;
    }

    public void setAggregationStrategy(String aggregationStrategy) {
        this.aggregationStrategy = aggregationStrategy;
    }

    public Boolean getParallel() {
        return parallel;
    }

    public void setParallel(Boolean parallel) {
        this.parallel = parallel;
    }
}
