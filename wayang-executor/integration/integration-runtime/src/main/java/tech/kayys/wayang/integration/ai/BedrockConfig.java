package tech.kayys.silat.executor.camel.ai;

record BedrockConfig(
    String modelId,
    String region,
    double temperature,
    int maxTokens
) {}