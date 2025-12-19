package tech.kayys.wayang.orchestration.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.OrchestrationPattern;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy registry
 */
@ApplicationScoped
public class OrchestrationStrategyRegistry {

    private final Map<OrchestrationPattern.PatternType, OrchestrationStrategy> strategies = new HashMap<>();

    @Inject
    public OrchestrationStrategyRegistry(Instance<OrchestrationStrategy> strategyInstances) {
        strategyInstances.forEach(strategy -> strategies.put(strategy.getSupportedPattern(), strategy));
    }

    public OrchestrationStrategy getStrategy(OrchestrationPattern.PatternType type) {
        OrchestrationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for pattern: " + type);
        }
        return strategy;
    }
}
