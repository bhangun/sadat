package tech.kayys.wayang.agent.model;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * LLM-based conversation summarizer
 */
@ApplicationScoped
public class LLMSummarizer {

    private static final Logger LOG = LoggerFactory.getLogger(LLMSummarizer.class);

    public String summarize(List<Message> messages) {
        LOG.debug("Summarizing conversation");
        // In real implementation, call LLM to generate summary
        // For now, simple concatenation
        return messages.stream()
                .filter(m -> !m.isSystem())
                .map(m -> m.role() + ": " + truncate(m.content(), 100))
                .collect(Collectors.joining("; "));
    }

    private String truncate(String text, int maxLength) {
        LOG.debug("Truncating text: {}", text);
        if (text == null)
            return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
