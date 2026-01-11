package tech.kayys.silat.executor.rag.examples;

import tech.kayys.silat.executor.rag.domain.*;

import java.util.List;
import java.util.Map;

record RagQueryRequest(
    String tenantId,
    String query,
    RagMode ragMode,
    SearchStrategy searchStrategy,
    RetrievalConfig retrievalConfig,
    GenerationConfig generationConfig,
    List<String> collections,
    Map<String, Object> filters
) {}