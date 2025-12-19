package tech.kayys.wayang.llm.model;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.model.LLMConfig;
import tech.kayys.wayang.workflow.model.ExecutionContext;

/**
 * LLM Provider interface
 */
public interface LLMProvider {
    Uni<String> complete(LLMConfig config, String prompt, ExecutionContext context);

    LLMConfig.Provider getSupportedProvider();
}