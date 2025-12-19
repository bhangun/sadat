package tech.kayys.wayang.agent.dto;

import java.time.Instant;
import java.util.List;

/**
 * Template detail with additional info
 */
public record AgentTemplateDetail(
        String id,
        String name,
        String description,
        String category,
        AgentType agentType,
        List<String> useCases,
        LLMConfig llmConfig,
        List<String> tools,
        RAGConfig ragConfig,
        MemoryConfig memoryConfig,
        List<TemplateParameter> parameters,
        String icon,
        boolean featured,
        String author,
        String version,
        Instant createdAt,
        List<TemplateExample> examples,
        long usageCount,
        double rating) {
}
