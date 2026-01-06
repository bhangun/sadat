package tech.kayys.wayang.project.dto;

import java.time.Instant;

import tech.kayys.wayang.project.domain.AIAgent;
import tech.kayys.wayang.project.service.AgentMemory;
import tech.kayys.wayang.project.service.LLMProvider;

/**
 * Active agent runtime
 */
public class ActiveAgent {
    public final AIAgent agent;
    public final LLMProvider llmProvider;
    public final AgentMemory memory;
    public final Instant activatedAt;

    public ActiveAgent(AIAgent agent, LLMProvider llmProvider, AgentMemory memory) {
        this.agent = agent;
        this.llmProvider = llmProvider;
        this.memory = memory;
        this.activatedAt = Instant.now();
    }
}
