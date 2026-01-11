package tech.kayys.silat.executor.camel.ai;

record AzureOpenAIConfig(
    String resourceName,
    String deploymentName,
    String apiKey,
    double temperature,
    int maxTokens
) {}