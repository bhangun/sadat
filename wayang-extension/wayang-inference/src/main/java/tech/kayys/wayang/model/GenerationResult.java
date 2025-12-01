package tech.kayys.wayang.model;


public record GenerationResult(
    String text,
    int tokensGenerated,
    int promptTokens,
    long timeMs,
    String finishReason
) {}