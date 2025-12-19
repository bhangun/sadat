package tech.kayys.wayang.schema.workflow;

import java.util.Arrays;
import java.util.List;

public class Trigger {
    private String type;
    private String expression;
    private String path;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        List<String> validTypes = Arrays.asList("cron", "webhook", "event", "manual");
        if (!validTypes.contains(type)) {
            throw new IllegalArgumentException("Invalid trigger type: " + type);
        }
        this.type = type;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
