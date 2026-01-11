package tech.kayys.silat.executor.camel.ai;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive Large Language Model integration
 */
@ApplicationScoped
public class LLMIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(LLMIntegrationService.class);

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    /**
     * OpenAI GPT integration with streaming
     */
    public Uni<LLMResponse> generateWithOpenAI(
            String prompt,
            OpenAIConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            java.util.concurrent.CompletableFuture<LLMResponse> future = new java.util.concurrent.CompletableFuture<>();

            try {
                String routeId = "openai-generate-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                            .setHeader("Authorization", simple("Bearer " + config.apiKey()))

                            // Build request
                            .process(exchange -> {
                                Map<String, Object> request = Map.of(
                                    "model", config.model(),
                                    "messages", List.of(
                                        Map.of("role", "system", "content", config.systemPrompt()),
                                        Map.of("role", "user", "content", prompt)
                                    ),
                                    "temperature", config.temperature(),
                                    "max_tokens", config.maxTokens(),
                                    "stream", config.stream()
                                );
                                exchange.getIn().setBody(request);
                            })

                            // Call OpenAI API
                            .toD("https://api.openai.com/v1/chat/completions")

                            // Parse response
                            .unmarshal().json()
                            .process(exchange -> {
                                Map<String, Object> response = exchange.getIn().getBody(Map.class);

                                List<Map<String, Object>> choices =
                                    (List<Map<String, Object>>) response.get("choices");

                                if (!choices.isEmpty()) {
                                    Map<String, Object> message =
                                        (Map<String, Object>) choices.get(0).get("message");

                                    String content = (String) message.get("content");
                                    Map<String, Object> usage =
                                        (Map<String, Object>) response.get("usage");

                                    LLMResponse llmResponse = new LLMResponse(
                                        content,
                                        config.model(),
                                        (int) usage.get("prompt_tokens"),
                                        (int) usage.get("completion_tokens"),
                                        (int) usage.get("total_tokens"),
                                        Instant.now()
                                    );

                                    future.complete(llmResponse);
                                } else {
                                    future.completeExceptionally(
                                        new RuntimeException("No response from OpenAI")
                                    );
                                }
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, null);

            } catch (Exception e) {
                LOG.error("OpenAI generation failed", e);
                future.completeExceptionally(e);
            }

            return future;
        });
    }

    /**
     * Anthropic Claude integration
     */
    public Uni<LLMResponse> generateWithClaude(
            String prompt,
            ClaudeConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            java.util.concurrent.CompletableFuture<LLMResponse> future = new java.util.concurrent.CompletableFuture<>();

            try {
                String routeId = "claude-generate-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                            .setHeader("x-api-key", constant(config.apiKey()))
                            .setHeader("anthropic-version", constant("2023-06-01"))

                            // Build request
                            .process(exchange -> {
                                Map<String, Object> request = Map.of(
                                    "model", config.model(),
                                    "messages", List.of(
                                        Map.of("role", "user", "content", prompt)
                                    ),
                                    "max_tokens", config.maxTokens(),
                                    "temperature", config.temperature(),
                                    "system", config.systemPrompt()
                                );
                                exchange.getIn().setBody(request);
                            })

                            // Call Claude API
                            .toD("https://api.anthropic.com/v1/messages")

                            // Parse response
                            .unmarshal().json()
                            .process(exchange -> {
                                Map<String, Object> response = exchange.getIn().getBody(Map.class);

                                List<Map<String, Object>> content =
                                    (List<Map<String, Object>>) response.get("content");

                                String text = (String) content.get(0).get("text");
                                Map<String, Object> usage =
                                    (Map<String, Object>) response.get("usage");

                                LLMResponse llmResponse = new LLMResponse(
                                    text,
                                    config.model(),
                                    (int) usage.get("input_tokens"),
                                    (int) usage.get("output_tokens"),
                                    (int) usage.get("input_tokens") + (int) usage.get("output_tokens"),
                                    Instant.now()
                                );

                                future.complete(llmResponse);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, null);

            } catch (Exception e) {
                LOG.error("Claude generation failed", e);
                future.completeExceptionally(e);
            }

            return future;
        });
    }

    /**
     * AWS Bedrock integration (multi-model support)
     */
    public Uni<LLMResponse> generateWithBedrock(
            String prompt,
            BedrockConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            java.util.concurrent.CompletableFuture<LLMResponse> future = new java.util.concurrent.CompletableFuture<>();

            try {
                String routeId = "bedrock-generate-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))

                            // AWS Bedrock via Camel AWS2 component
                            .process(exchange -> {
                                Map<String, Object> request = buildBedrockRequest(
                                    prompt, config
                                );
                                exchange.getIn().setBody(request);
                            })

                            .toD("aws2-bedrock-runtime://" + config.modelId() +
                                 "?region=" + config.region() +
                                 "&operation=invokeModel")

                            .unmarshal().json()
                            .process(exchange -> {
                                Map<String, Object> response = exchange.getIn().getBody(Map.class);
                                LLMResponse llmResponse = parseBedrockResponse(
                                    response, config.modelId()
                                );
                                future.complete(llmResponse);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, null);

            } catch (Exception e) {
                LOG.error("Bedrock generation failed", e);
                future.completeExceptionally(e);
            }

            return future;
        });
    }

    /**
     * Azure OpenAI integration
     */
    public Uni<LLMResponse> generateWithAzureOpenAI(
            String prompt,
            AzureOpenAIConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            java.util.concurrent.CompletableFuture<LLMResponse> future = new java.util.concurrent.CompletableFuture<>();

            try {
                String routeId = "azure-openai-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                            .setHeader("api-key", constant(config.apiKey()))

                            .process(exchange -> {
                                Map<String, Object> request = Map.of(
                                    "messages", List.of(
                                        Map.of("role", "user", "content", prompt)
                                    ),
                                    "temperature", config.temperature(),
                                    "max_tokens", config.maxTokens()
                                );
                                exchange.getIn().setBody(request);
                            })

                            .toD("https://" + config.resourceName() +
                                 ".openai.azure.com/openai/deployments/" +
                                 config.deploymentName() +
                                 "/chat/completions?api-version=2024-02-01")

                            .unmarshal().json()
                            .process(exchange -> {
                                Map<String, Object> response = exchange.getIn().getBody(Map.class);
                                LLMResponse llmResponse = parseAzureOpenAIResponse(response);
                                future.complete(llmResponse);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, null);

            } catch (Exception e) {
                future.completeExceptionally(e);
            }

            return future;
        });
    }

    private Map<String, Object> buildBedrockRequest(String prompt, BedrockConfig config) {
        // Different models have different request formats
        return switch (config.modelId()) {
            case String s when s.startsWith("anthropic.claude") -> Map.of(
                "prompt", "\n\nHuman: " + prompt + "\n\nAssistant:",
                "max_tokens_to_sample", config.maxTokens(),
                "temperature", config.temperature()
            );
            case String s when s.startsWith("amazon.titan") -> Map.of(
                "inputText", prompt,
                "textGenerationConfig", Map.of(
                    "maxTokenCount", config.maxTokens(),
                    "temperature", config.temperature()
                )
            );
            default -> Map.of("prompt", prompt);
        };
    }

    private LLMResponse parseBedrockResponse(Map<String, Object> response, String modelId) {
        String content = "";
        int tokens = 0;

        if (modelId.startsWith("anthropic.claude")) {
            content = (String) response.get("completion");
        } else if (modelId.startsWith("amazon.titan")) {
            List<Map<String, Object>> results =
                (List<Map<String, Object>>) response.get("results");
            content = (String) results.get(0).get("outputText");
        }

        return new LLMResponse(content, modelId, tokens, tokens, tokens * 2, Instant.now());
    }

    private LLMResponse parseAzureOpenAIResponse(Map<String, Object> response) {
        List<Map<String, Object>> choices =
            (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message =
            (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        Map<String, Object> usage = (Map<String, Object>) response.get("usage");

        return new LLMResponse(
            content,
            "azure-openai",
            (int) usage.get("prompt_tokens"),
            (int) usage.get("completion_tokens"),
            (int) usage.get("total_tokens"),
            Instant.now()
        );
    }
}