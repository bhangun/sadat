package tech.kayys.silat.executor.rag.examples;

import java.util.Map;

record IngestResult(
    boolean success,
    int documentsIngested,
    int segmentsCreated,
    long durationMs,
    String message
) {}