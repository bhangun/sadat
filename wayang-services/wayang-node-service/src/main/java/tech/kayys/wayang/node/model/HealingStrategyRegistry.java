package tech.kayys.wayang.workflow.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Registry for healing strategies.
 */
@ApplicationScoped
class HealingStrategyRegistry {

    private final List<HealingStrategy> strategies = new ArrayList<>();

    public void register(HealingStrategy strategy) {
        strategies.add(strategy);
    }

    public HealingStrategy findStrategy(String errorMessage) {
        return strategies.stream()
                .filter(s -> s.canHandle(errorMessage))
                .findFirst()
                .orElse(null);
    }
}
