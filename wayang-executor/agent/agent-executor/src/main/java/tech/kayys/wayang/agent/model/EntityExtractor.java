package tech.kayys.wayang.agent.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Entity extraction service
 */
@ApplicationScoped
public class EntityExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(EntityExtractor.class);

    public Map<String, String> extractEntities(List<Message> messages) {
        LOG.debug("Extracting entities from messages");
        // In real implementation, use NER or LLM to extract entities
        // For now, simple placeholder
        Map<String, String> entities = new HashMap<>();

        // Example: extract email addresses, names, etc.
        for (Message msg : messages) {
            if (msg.content() != null) {
                // Simple email detection
                if (msg.content().contains("@")) {
                    String[] words = msg.content().split("\\s+");
                    for (String word : words) {
                        if (word.contains("@")) {
                            entities.put("email", word);
                        }
                    }
                }
            }
        }

        return entities;
    }
}
