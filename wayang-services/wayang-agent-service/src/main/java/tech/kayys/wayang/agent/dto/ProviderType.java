package tech.kayys.wayang.agent.dto;

public enum ProviderType {
    CLOUD, // Hosted by provider (OpenAI, Anthropic)
    SELF_HOSTED, // Self-hosted infrastructure
    LOCAL, // Local machine (Ollama)
    EDGE // Edge devices
}
