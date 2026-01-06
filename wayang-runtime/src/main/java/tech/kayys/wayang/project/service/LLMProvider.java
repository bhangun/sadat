package tech.kayys.wayang.project.service;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.project.dto.LLMConfig;

/**
 * LLM Provider interface
 */
public interface LLMProvider {
    Uni<String> complete(String prompt, LLMConfig config);
}
