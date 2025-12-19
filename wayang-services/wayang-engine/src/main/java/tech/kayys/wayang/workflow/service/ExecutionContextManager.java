package tech.kayys.wayang.workflow.service;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.model.ExecutionContext;

@ApplicationScoped
public class ExecutionContextManager {
    public ExecutionContext createContext() {
        return new ExecutionContext();
    }

    public ExecutionContext getContext(String id) {
        return new ExecutionContext();
    }
}
