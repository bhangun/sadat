package tech.kayys.wayang.agent.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Summary Memory Strategy
 * Maintains summaries of older conversations
 */
@ApplicationScoped
public class SummaryMemoryStrategy implements MemoryStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(SummaryMemoryStrategy.class);

    @Inject
    LLMSummarizer summarizer;

    @Override
    public List<Message> process(List<Message> messages, Integer windowSize) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        int window = windowSize != null ? windowSize : 10;

        if (messages.size() <= window) {
            return new ArrayList<>(messages);
        }

        // Keep recent messages, summarize older ones
        List<Message> older = messages.subList(0, messages.size() - window);
        List<Message> recent = messages.subList(messages.size() - window, messages.size());

        String summary = summarizer.summarize(older);

        List<Message> result = new ArrayList<>();
        result.add(Message.system("Previous conversation summary: " + summary));
        result.addAll(recent);

        LOG.debug("Summary memory: {} -> {} messages (with summary)",
                messages.size(), result.size());

        return result;
    }

    @Override
    public String getType() {
        return "summary";
    }
}
