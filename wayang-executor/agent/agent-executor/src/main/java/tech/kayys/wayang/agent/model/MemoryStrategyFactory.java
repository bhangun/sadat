package tech.kayys.wayang.agent.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MemoryStrategyFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryStrategyFactory.class);

    private final Map<String, MemoryStrategy> strategies = new ConcurrentHashMap<>();

    @Inject
    BufferMemoryStrategy bufferStrategy;

    @Inject
    SummaryMemoryStrategy summaryStrategy;

    @Inject
    VectorMemoryStrategy vectorStrategy;

    @Inject
    EntityMemoryStrategy entityStrategy;

    @jakarta.annotation.PostConstruct
    void init() {
        registerStrategy(bufferStrategy);
        registerStrategy(summaryStrategy);
        registerStrategy(vectorStrategy);
        registerStrategy(entityStrategy);

        LOG.info("Registered {} memory strategies", strategies.size());
    }

    public void registerStrategy(MemoryStrategy strategy) {
        strategies.put(strategy.getType(), strategy);
        LOG.debug("Registered memory strategy: {}", strategy.getType());
    }

    public Uni<MemoryStrategy> getStrategy(String type) {
        MemoryStrategy strategy = strategies.getOrDefault(type, bufferStrategy);
        return Uni.createFrom().item(strategy);
    }
}
