package tech.kayys.wayang.nodes.services;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class PromptShaper {
    public Prompt shape(String prompt, int maxTokens, Map<String, Object> metadata) {
        return new Prompt();
    }
}
