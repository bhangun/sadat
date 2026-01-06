package tech.kayys.wayang.agent.dto;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Agent Execution Request
 */
public record AgentExecutionRequest(
    String requestId,
    String taskDescription,
    Map<String, Object> context,
    Set<String> requiredCapabilities,
    ExecutionConstraints constraints,
    Map<String, Object> metadata,
    Instant submittedAt
) {
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String requestId = UUID.randomUUID().toString();
        private String taskDescription;
        private Map<String, Object> context = new HashMap<>();
        private Set<String> requiredCapabilities = new HashSet<>();
        private ExecutionConstraints constraints = ExecutionConstraints.createDefault();
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder taskDescription(String desc) {
            this.taskDescription = desc;
            return this;
        }
        
        public Builder context(String key, Object value) {
            this.context.put(key, value);
            return this;
        }
        
        public Builder requiredCapability(String capability) {
            this.requiredCapabilities.add(capability);
            return this;
        }
        
        public Builder constraints(ExecutionConstraints constraints) {
            this.constraints = constraints;
            return this;
        }
        
        public AgentExecutionRequest build() {
            return new AgentExecutionRequest(
                requestId,
                taskDescription,
                context,
                requiredCapabilities,
                constraints,
                metadata,
                Instant.now()
            );
        }
    }
}