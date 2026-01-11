package tech.kayys.wayang.agent.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Vector Memory Strategy
 * Uses semantic similarity for retrieval
 */
@ApplicationScoped
public class VectorMemoryStrategy implements MemoryStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(VectorMemoryStrategy.class);

    @Inject
    VectorSearchService vectorSearch;

    @Override
    public List<Message> process(List<Message> messages, Integer windowSize) {
        LOG.debug("Processing messages with vector memory strategy");
        // Vector memory typically used with search, not direct loading
        // Return recent messages by default
        int window = windowSize != null ? windowSize : 10;
        int startIndex = Math.max(0, messages.size() - window);

        return new ArrayList<>(messages.subList(startIndex, messages.size()));
    }

    @Override
    public String getType() {
        return "vector";
    }
}
