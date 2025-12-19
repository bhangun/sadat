package tech.kayys.wayang.orchestration.strategy;

/* 
import io.quarkus.ai.agent.runtime.model.*;
import io.quarkus.ai.agent.runtime.context.ExecutionContext; */
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.model.AgentDefinition;
import tech.kayys.wayang.agent.model.OrchestrationPattern;
import tech.kayys.wayang.orchestration.engine.OrchestrationEngine;
import tech.kayys.wayang.workflow.model.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * Base strategy interface
 */
public interface OrchestrationStrategy {
    Uni<StrategyResult> execute(AgentDefinition agent, Map<String, Object> input,
            OrchestrationPattern pattern, ExecutionContext context);

    OrchestrationPattern.PatternType getSupportedPattern();

    class StrategyResult {
        private final boolean success;
        private final Map<String, Object> output;
        private final List<OrchestrationEngine.OrchestrationStep> steps;
        private final Map<String, Object> metadata;

        public StrategyResult(boolean success, Map<String, Object> output,
                List<OrchestrationEngine.OrchestrationStep> steps,
                Map<String, Object> metadata) {
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

        public List<OrchestrationEngine.OrchestrationStep> getSteps() {
            return steps;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
}