package tech.kayys.wayang.schema.node;

import java.util.List;

public class PropertyValidation {
    private Number min;
    private Number max;
    private String pattern;
    private List<Object> enumValues;
    private String celExpression;

    // Getters and setters
    public Number getMin() {
        return min;
    }

    public void setMin(Number min) {
        this.min = min;
    }

    public Number getMax() {
        return max;
    }

    public void setMax(Number max) {
        this.max = max;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<Object> getEnum() {
        return enumValues;
    }

    public void setEnum(List<Object> enumValues) {
        this.enumValues = enumValues;
    }

    public String getCelExpression() {
        return celExpression;
    }

    public void setCelExpression(String celExpression) {
        this.celExpression = celExpression;
    }
}