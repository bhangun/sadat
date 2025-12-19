package tech.kayys.wayang.schema.agent;

import java.util.Map;

/**
 * RAG Result.
 */
class RAGResult {
    private String id;
    private String content;
    private Map<String, Object> metadata;
    private double score;
    private String source;
}