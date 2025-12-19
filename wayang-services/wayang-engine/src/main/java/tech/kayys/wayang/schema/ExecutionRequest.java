package tech.kayys.wayang.schema;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ExecutionRequest - Request to execute workflow
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionRequest {

    private Map<String, Object> inputs = new HashMap<>();
    private Map<String, Object> context = new HashMap<>();
    private boolean async = true;
    private String callbackUrl;

    // Getters and setters...
    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
}
