package tech.kayys.wayang.agent.model;

import java.util.List;

/**
 * Base interface for memory processing strategies
 */
public interface MemoryStrategy {

    /**
     * Process raw messages according to strategy
     */
    List<Message> process(List<Message> messages, Integer windowSize);

    /**
     * Get strategy type
     */
    String getType();
}
