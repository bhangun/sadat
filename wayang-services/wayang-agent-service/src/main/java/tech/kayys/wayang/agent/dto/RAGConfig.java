package tech.kayys.wayang.agent.dto;

public record RAGConfig(
        boolean enabled,
        String vectorStore,
        int topK,
        double similarityThreshold) {
}
