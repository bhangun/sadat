package tech.kayys.wayang.agent.dto;

public record VectorStoreStats(
        long totalVectors,
        long dimensionality,
        String indexType) {
}