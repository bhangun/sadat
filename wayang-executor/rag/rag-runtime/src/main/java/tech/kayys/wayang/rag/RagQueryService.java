package tech.kayys.silat.executor.rag.examples;

import dev.langchain4j.data.segment.TextSegment;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.client.SilatClient;
import tech.kayys.silat.executor.rag.domain.*;

import java.time.Instant;
import java.util.*;

/**
 * High-level service for executing RAG queries via Silat
 */
@ApplicationScoped
public class RagQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(RagQueryService.class);

    @Inject
    SilatClient silatClient;

    /**
     * Execute simple RAG query
     */
    public Uni<RagResponse> query(
            String tenantId,
            String query,
            String collectionName) {

        return executeRagWorkflow(
            tenantId,
            query,
            RagMode.STANDARD,
            SearchStrategy.HYBRID,
            RetrievalConfig.defaults(),
            GenerationConfig.defaults(),
            List.of(collectionName),
            Map.of()
        );
    }

    /**
     * Execute advanced RAG query with full configuration
     */
    public Uni<RagResponse> advancedQuery(RagQueryRequest request) {

        return executeRagWorkflow(
            request.tenantId(),
            request.query(),
            request.ragMode(),
            request.searchStrategy(),
            request.retrievalConfig(),
            request.generationConfig(),
            request.collections(),
            request.filters()
        );
    }

    /**
     * Execute multi-turn conversational RAG
     */
    public Uni<RagResponse> conversationalQuery(
            String tenantId,
            String query,
            String sessionId,
            List<ConversationTurn> history) {

        // Build context from history
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sessionId", sessionId);
        metadata.put("conversationHistory", history);

        // Enhance query with conversation context
        String enhancedQuery = enhanceQueryWithHistory(query, history);

        return executeRagWorkflow(
            tenantId,
            enhancedQuery,
            RagMode.STANDARD,
            SearchStrategy.HYBRID,
            RetrievalConfig.defaults(),
            GenerationConfig.defaults(),
            List.of(),
            metadata
        );
    }

    private Uni<RagResponse> executeRagWorkflow(
            String tenantId,
            String query,
            RagMode mode,
            SearchStrategy strategy,
            RetrievalConfig retrievalConfig,
            GenerationConfig generationConfig,
            List<String> collections,
            Map<String, Object> filters) {

        LOG.info("Executing RAG workflow for tenant: {}, query: {}", tenantId, query);

        // Build workflow input
        Map<String, Object> input = new HashMap<>();
        input.put("query", query);
        input.put("tenantId", tenantId);
        input.put("ragMode", mode.name());
        input.put("searchStrategy", strategy.name());
        input.put("retrievalConfig", serializeRetrievalConfig(retrievalConfig));
        input.put("generationConfig", serializeGenerationConfig(generationConfig));
        input.put("collections", collections);
        input.put("filters", filters);

        // Execute workflow
        return silatClient.runs()
            .create("rag-langchain4j-workflow")
            .input("query", query)
            .input("tenantId", tenantId)
            .input("ragMode", mode.name())
            .input("searchStrategy", strategy.name())
            .input("retrievalConfig", serializeRetrievalConfig(retrievalConfig))
            .input("generationConfig", serializeGenerationConfig(generationConfig))
            .input("collections", collections)
            .input("filters", filters)
            .executeAndStart()
            .map(run -> {
                // Poll for completion
                return pollForCompletion(run.runId());
            })
            .flatMap(runId -> getRagResponse(runId));
    }

    private String pollForCompletion(String runId) {
        // Simplified - in production use reactive polling
        return runId;
    }

    private Uni<RagResponse> getRagResponse(String runId) {
        return silatClient.runs()
            .get(runId)
            .map(run -> {
                // Extract RAG response from run output
                Map<String, Object> output = (Map<String, Object>) run;
                return parseRagResponse(output);
            });
    }

    private RagResponse parseRagResponse(Map<String, Object> output) {
        // Parse response from workflow output
        String answer = (String) output.get("answer");

        return new RagResponse(
            "", // query
            answer,
            List.of(),
            List.of(),
            null,
            null,
            Instant.now(),
            Map.of(),
            List.of(),
            Optional.empty()
        );
    }

    private String enhanceQueryWithHistory(
            String query,
            List<ConversationTurn> history) {

        if (history.isEmpty()) {
            return query;
        }

        StringBuilder enhanced = new StringBuilder();
        enhanced.append("Previous conversation:\n");

        for (ConversationTurn turn : history) {
            enhanced.append("User: ").append(turn.userMessage()).append("\n");
            enhanced.append("Assistant: ").append(turn.assistantMessage()).append("\n");
        }

        enhanced.append("\nCurrent question: ").append(query);

        return enhanced.toString();
    }

    private Map<String, Object> serializeRetrievalConfig(RetrievalConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("topK", config.topK());
        map.put("minSimilarity", config.minSimilarity());
        map.put("maxChunkSize", config.maxChunkSize());
        map.put("chunkOverlap", config.chunkOverlap());
        map.put("enableReranking", config.enableReranking());
        map.put("enableHybridSearch", config.enableHybridSearch());
        map.put("hybridAlpha", config.hybridAlpha());
        return map;
    }

    private Map<String, Object> serializeGenerationConfig(GenerationConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("provider", config.provider());
        map.put("model", config.model());
        map.put("temperature", config.temperature());
        map.put("maxTokens", config.maxTokens());
        map.put("systemPrompt", config.systemPrompt());
        return map;
    }
}