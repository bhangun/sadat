package tech.kayys.silat.executor.rag.examples;

import java.util.Map;

record DocumentSource(
    SourceType type,
    String path,
    String content,
    Map<String, String> metadata
) {}