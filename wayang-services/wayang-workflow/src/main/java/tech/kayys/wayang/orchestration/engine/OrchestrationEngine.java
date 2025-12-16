package tech.kayys.wayang.orchestration.engine;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.AgentDefinition;
import tech.kayys.wayang.agent.model.OrchestrationPattern;
import tech.kayys.wayang.orchestration.strategy.OrchestrationStrategy;
import tech.kayys.wayang.orchestration.strategy.OrchestrationStrategyRegistry;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

/**
 * Advanced Orchestration Engine supporting multiple agentic patterns
 */
@ApplicationScoped
public class OrchestrationEngine {

    private static final Logger LOG = Logger.getLogger(OrchestrationEngine.class);

    @Inject
    WorkflowRuntimeEngine workflowEngine;

    @Inject
    OrchestrationStrategyRegistry strategyRegistry;

    /**
     * Execute agent with orchestration pattern
     */
    public Uni<OrchestrationResult> execute(
            AgentDefinition agent,
            Map<String, Object> input,
            OrchestrationPattern pattern) {

        LOG.infof("Executing agent with pattern: %s", pattern.getType());

        OrchestrationStrategy strategy = strategyRegistry.getStrategy(pattern.getType());
        ExecutionContext context = new ExecutionContext();
        context.setInput(input);

        return strategy.execute(agent, input, pattern, context)
                .map(result -> new OrchestrationResult(
                        result.isSuccess(),
                        result.getOutput(),
                        result.getSteps(),
                        result.getMetadata()));
    }

    /**
     * Orchestration result
     */
    public static class OrchestrationResult {
        private final boolean success;
        private final Map<String, Object> output;
        private final List<OrchestrationStep> steps;
        private final Map<String, Object> metadata;

        public OrchestrationResult(boolean success, Map<String, Object> output,
                List<OrchestrationStep> steps, Map<String, Object> metadata) {
            this.success = success;
            this.output = output;
            this.steps = steps;
            this.metadata = metadata;
        }

        public boolean isSuccess() {
            return success;
        }

        public Map<String, Object> getOutput() {
            return output;
        }

        public List<OrchestrationStep> getSteps() {
            return steps;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    /**
     * Orchestration step for tracking
     */
    public static class OrchestrationStep {
        private final String agentId;
        private final String agentName;
        private final long timestamp;
        private final Map<String, Object> input;
        private final Map<String, Object> output;
        private final String status;
        private final long duration;

        public OrchestrationStep(String agentId, String agentName, long timestamp,
                Map<String, Object> input, Map<String, Object> output,
                String status, long duration) {
            this.agentId = agentId;
            this.agentName = agentName;
            this.timestamp = timestamp;
            this.input = input;
            this.output = output;
            this.status = status;
            this.duration = duration;
        }

        // Getters
        public String getAgentId() {
            return agentId;
        }

        public String getAgentName() {
            return agentName;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Map<String, Object> getInput() {
            return input;
        }

        public Map<String, Object> getOutput() {
            return output;
        }

        public String getStatus() {
            return status;
        }

        public long getDuration() {
            return duration;
        }
    }
}
