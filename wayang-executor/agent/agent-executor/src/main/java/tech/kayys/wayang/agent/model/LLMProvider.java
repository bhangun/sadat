package tech.kayys.wayang.agent.model;

import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * LLM PROVIDER SYSTEM
 * ============================================================================
 * 
 * Multi-provider LLM integration supporting:
 * - OpenAI (GPT-4, GPT-3.5)
 * - Anthropic (Claude)
 * - Azure OpenAI
 * - Google (Gemini)
 * - Local models (Ollama, LM Studio)
 * - Custom providers
 * 
 * Features:
 * - Provider abstraction layer
 * - Function/tool calling support
 * - Streaming responses
 * - Token usage tracking
 * - Automatic retries
 * - Rate limiting
 * - Cost tracking
 * 
 * Architecture:
 * ┌───────────────────────────────────────────────────────────┐
 * │              LLM Provider Registry                        │
 * ├───────────────────────────────────────────────────────────┤
 * │  ┌─────────┐  ┌─────────┐  ┌───────┐  ┌──────────────┐ │
 * │  │ OpenAI  │  │ Claude  │  │ Azure │  │   Custom     │ │
 * │  │Provider │  │Provider │  │OpenAI │  │   Provider   │ │
 * │  └─────────┘  └─────────┘  └───────┘  └──────────────┘ │
 * │       │            │            │             │          │
 * │  ┌────▼────────────▼────────────▼─────────────▼──────┐  │
 * │  │         Provider Abstraction Layer               │  │
 * │  │  (Rate Limiting, Retries, Metrics)               │  │
 * │  └──────────────────────────────────────────────────┘  │
 * └───────────────────────────────────────────────────────────┘
 */

// ==================== LLM PROVIDER INTERFACE ====================

/**
 * Base interface for LLM providers
 */
public interface LLMProvider {

    /**
     * Get provider name
     */
    String name();

    /**
     * Complete a chat conversation
     */
    Uni<LLMResponse> complete(LLMRequest request);

    /**
     * Stream completion (for real-time responses)
     */
    io.smallrye.mutiny.Multi<String> stream(LLMRequest request);

    /**
     * Check if provider supports function calling
     */
    boolean supportsFunctionCalling();

    /**
     * Get supported models
     */
    List<String> supportedModels();

    /**
     * Check if model is available
     */
    boolean supportsModel(String model);

    /**
     * Get provider configuration
     */
    Map<String, Object> getConfig();
}
