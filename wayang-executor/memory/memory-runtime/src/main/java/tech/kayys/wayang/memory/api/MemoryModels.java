package tech.kayys.silat.executor.memory.api;

import tech.kayys.silat.core.domain.MemoryType;
import java.util.Map;
import java.util.List;

record StoreRequest(
    String namespace,
    String content,
    MemoryType type,
    Double importance,
    Map<String, Object> metadata
) {}

record StoreResponse(
    boolean success,
    String memoryId,
    String message
) {}

record SearchRequest(
    String namespace,
    String query,
    Integer limit,
    Double minSimilarity
) {}

record SearchResponse(
    boolean success,
    List<MemoryResult> results,
    int count
) {}

record MemoryResult(
    String id,
    String content,
    double score,
    String type,
    double importance
) {}

record ContextRequest(
    String namespace,
    String query,
    Integer maxMemories,
    String systemPrompt,
    String taskInstructions,
    Boolean includeMetadata
) {}

record ContextResponse(
    boolean success,
    String prompt,
    int totalTokens,
    double utilization,
    int sectionCount
) {}

record StatsResponse(
    boolean success,
    long totalMemories,
    long episodicCount,
    long semanticCount,
    long proceduralCount,
    long workingCount,
    double avgImportance
) {}

record ExampleResponse(
    boolean success,
    String message,
    int examplesCount
) {}