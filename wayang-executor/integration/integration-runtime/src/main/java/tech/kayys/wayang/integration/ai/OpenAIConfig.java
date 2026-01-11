package tech.kayys.silat.executor.camel.ai;

record OpenAIConfig(
    String apiKey,
    String model,
    String systemPrompt,
    double temperature,
    int maxTokens,
    boolean stream
) {}