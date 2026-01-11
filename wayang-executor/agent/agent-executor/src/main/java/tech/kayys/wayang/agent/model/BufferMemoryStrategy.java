package tech.kayys.wayang.agent.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Buffer Memory Strategy
 * Simple FIFO window of recent messages
 */
@ApplicationScoped
public class BufferMemoryStrategy implements MemoryStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(BufferMemoryStrategy.class);

    @Override
    public List<Message> process(List<Message> messages, Integer windowSize) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        int window = windowSize != null ? windowSize : 10;
        int startIndex = Math.max(0, messages.size() - window);

        List<Message> result = messages.subList(startIndex, messages.size());
        LOG.debug("Buffer memory: {} -> {} messages", messages.size(), result.size());

        return new ArrayList<>(result);
    }

    @Override
    public String getType() {
        return "buffer";
    }
}