package tech.kayys.silat.executor.camel.ai;

record ClaudeConfig(
    String apiKey,
    String model,
    String systemPrompt,
    double temperature,
    int maxTokens
) {}