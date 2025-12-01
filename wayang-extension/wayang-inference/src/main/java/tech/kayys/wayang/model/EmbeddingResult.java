package tech.kayys.wayang.model;

import java.util.List;

public record EmbeddingResult(
    List<float[]> embeddings,
    int dimensions,
    long timeMs
) {}
