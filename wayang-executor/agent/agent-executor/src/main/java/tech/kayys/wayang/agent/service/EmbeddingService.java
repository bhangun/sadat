package tech.kayys.wayang.agent.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * VECTOR DATABASE INTEGRATION
 * ============================================================================
 * 
 * Semantic memory using vector embeddings for context-aware retrieval.
 * 
 * Supported vector databases:
 * - Pinecone
 * - Weaviate
 * - Qdrant
 * - Chroma
 * - PostgreSQL with pgvector
 * 
 * Features:
 * - Automatic embedding generation
 * - Semantic similarity search
 * - Metadata filtering
 * - Hybrid search (vector + keyword)
 */

// ==================== EMBEDDING SERVICE ====================

/**
 * Service for generating embeddings from text
 */
@ApplicationScoped
public class EmbeddingService {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddingService.class);

    @Inject
    WebClient webClient;

    @ConfigProperty(name = "silat.agent.embeddings.provider", defaultValue = "openai")
    String provider;

    @ConfigProperty(name = "silat.agent.embeddings.model", defaultValue = "text-embedding-ada-002")
    String model;

    @ConfigProperty(name = "silat.agent.llm.openai.api-key")
    String openaiApiKey;

    /**
     * Generate embedding vector for text
     */
    public Uni<float[]> generateEmbedding(String text) {
        LOG.debug("Generating embedding for text: {} chars", text.length());

        return switch (provider.toLowerCase()) {
            case "openai" -> generateOpenAIEmbedding(text);
            case "huggingface" -> generateHuggingFaceEmbedding(text);
            default -> Uni.createFrom().failure(
                    new IllegalArgumentException("Unsupported embedding provider: " + provider));
        };
    }

    /**
     * Generate embeddings for multiple texts (batch)
     */
    public Uni<List<float[]>> generateEmbeddings(List<String> texts) {
        LOG.debug("Generating embeddings for {} texts", texts.size());

        // Batch API call for efficiency
        return switch (provider.toLowerCase()) {
            case "openai" -> generateOpenAIEmbeddingsBatch(texts);
            default -> {
                // Fallback to sequential calls
                List<Uni<float[]>> unis = texts.stream()
                        .map(this::generateEmbedding)
                        .collect(Collectors.toList());

                yield Uni.combine().all().unis(unis).with(
                        results -> results.stream()
                                .map(r -> (float[]) r)
                                .collect(Collectors.toList()));
            }
        };
    }

    private Uni<float[]> generateOpenAIEmbedding(String text) {
        JsonObject requestBody = new JsonObject()
                .put("input", text)
                .put("model", model);

        return webClient
                .postAbs("https://api.openai.com/v1/embeddings")
                .putHeader("Authorization", "Bearer " + openaiApiKey)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(requestBody)
                .map(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Embedding API error: " + response.statusCode());
                    }

                    JsonObject json = response.bodyAsJsonObject();
                    JsonArray data = json.getJsonArray("data");
                    JsonObject embeddingObj = data.getJsonObject(0);
                    JsonArray embedding = embeddingObj.getJsonArray("embedding");

                    float[] vector = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        vector[i] = embedding.getDouble(i).floatValue();
                    }

                    LOG.trace("Generated embedding: dimension={}", vector.length);
                    return vector;
                });
    }

    private Uni<List<float[]>> generateOpenAIEmbeddingsBatch(List<String> texts) {
        JsonObject requestBody = new JsonObject()
                .put("input", new JsonArray(texts))
                .put("model", model);

        return webClient
                .postAbs("https://api.openai.com/v1/embeddings")
                .putHeader("Authorization", "Bearer " + openaiApiKey)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(requestBody)
                .map(response -> {
                    JsonObject json = response.bodyAsJsonObject();
                    JsonArray data = json.getJsonArray("data");

                    List<float[]> embeddings = new ArrayList<>();
                    for (int i = 0; i < data.size(); i++) {
                        JsonObject embeddingObj = data.getJsonObject(i);
                        JsonArray embedding = embeddingObj.getJsonArray("embedding");

                        float[] vector = new float[embedding.size()];
                        for (int j = 0; j < embedding.size(); j++) {
                            vector[j] = embedding.getDouble(j).floatValue();
                        }
                        embeddings.add(vector);
                    }

                    return embeddings;
                });
    }

    private Uni<float[]> generateHuggingFaceEmbedding(String text) {
        // Implementation for HuggingFace embeddings
        return Uni.createFrom().failure(
                new UnsupportedOperationException("HuggingFace embeddings not yet implemented"));
    }
}
