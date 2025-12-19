package tech.kayys.wayang.schema.execution;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExecutionContext {
    private String body;
    private Map<String, String> headers;
    private Map<String, String> properties;
    private String exchangePattern = "InOut";

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getExchangePattern() {
        return exchangePattern;
    }

    public void setExchangePattern(String exchangePattern) {
        List<String> validPatterns = Arrays.asList("InOnly", "InOut");
        if (!validPatterns.contains(exchangePattern)) {
            throw new IllegalArgumentException("Invalid exchange pattern: " + exchangePattern);
        }
        this.exchangePattern = exchangePattern;
    }
}
