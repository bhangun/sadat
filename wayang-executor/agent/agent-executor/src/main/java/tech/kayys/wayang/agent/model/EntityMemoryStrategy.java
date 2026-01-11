package tech.kayys.wayang.agent.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Entity Memory Strategy
 * Tracks and maintains entity information
 */
@ApplicationScoped
public class EntityMemoryStrategy implements MemoryStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(EntityMemoryStrategy.class);

    @Inject
    EntityExtractor entityExtractor;

    @Override
    public List<Message> process(List<Message> messages, Integer windowSize) {
        LOG.debug("Processing messages with entity memory strategy");
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        // Extract entities from conversation
        Map<String, String> entities = entityExtractor.extractEntities(messages);

        // Create context message with entity information
        if (!entities.isEmpty()) {
            String entityContext = "Known entities: " +
                    entities.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining(", "));

            List<Message> result = new ArrayList<>();
            result.add(Message.system(entityContext));

            // Add recent messages
            int window = windowSize != null ? windowSize : 10;
            int startIndex = Math.max(0, messages.size() - window);
            result.addAll(messages.subList(startIndex, messages.size()));

            return result;
        }

        // Fallback to buffer strategy
        int window = windowSize != null ? windowSize : 10;
        int startIndex = Math.max(0, messages.size() - window);
        return new ArrayList<>(messages.subList(startIndex, messages.size()));
    }

    @Override
    public String getType() {
        return "entity";
    }
}
