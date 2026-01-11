package tech.kayys.silat.executor.memory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.core.domain.*;
import tech.kayys.silat.core.engine.NodeExecutionResult;
import tech.kayys.silat.core.engine.NodeExecutionTask;
import tech.kayys.silat.executor.AbstractWorkflowExecutor;
import tech.kayys.silat.executor.Executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Specialized executor for querying and retrieving memories.
 * Provides advanced query capabilities:
 * - Semantic search
 * - Temporal queries
 * - Metadata filtering
 * - Cross-namespace search
 */
@Executor(
    executorType = "memory-query",
    communicationType = tech.kayys.silat.core.scheduler.CommunicationType.GRPC,
    maxConcurrentTasks = 20
)
@ApplicationScoped
public class MemoryQueryExecutor extends AbstractWorkflowExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryQueryExecutor.class);

    @Inject
    VectorMemoryStore memoryStore;

    @Inject
    EmbeddingServiceFactory embeddingFactory;

    @Inject
    ContextEngineeringService contextService;

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Map<String, Object> context = task.context();

        String query = (String) context.get("query");
        String namespace = (String) context.getOrDefault("namespace", "default");
        int limit = (int) context.getOrDefault("limit", 10);

        LOG.info("Executing memory query: '{}' in namespace: {}", query, namespace);

        ContextConfig config = ContextConfig.builder()
            .maxMemories(limit)
            .includeMetadata(true)
            .build();

        return contextService.buildContext(query, namespace, config)
            .map(engineeredContext -> {
                List<Map<String, Object>> results = new ArrayList<>();

                for (ContextSection section : engineeredContext.getSections()) {
                    if (section.getType().startsWith("memory_")) {
                        results.add(Map.of(
                            "content", section.getContent(),
                            "tokens", section.getTokenCount(),
                            "relevance", section.getRelevanceScore()
                        ));
                    }
                }

                Map<String, Object> output = Map.of(
                    "query", query,
                    "resultsCount", results.size(),
                    "results", results,
                    "contextTokens", engineeredContext.getTotalTokens()
                );

                return NodeExecutionResult.success(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    output,
                    task.token()
                );
            });
    }
}