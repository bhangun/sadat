package tech.kayys.wayang.agent.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.dto.SimilarMessage;
import tech.kayys.wayang.agent.dto.VectorStoreStats;
import tech.kayys.wayang.agent.model.Message;
import tech.kayys.wayang.agent.model.VectorStore;

@ApplicationScoped
public class PineconeVectorStore implements VectorStore {

    private static final Logger LOG = LoggerFactory.getLogger(PineconeVectorStore.class);

    @Inject
    WebClient webClient;

    @Inject
    EmbeddingService embeddingService;

    @ConfigProperty(name = "silat.agent.memory.vector.pinecone.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "silat.agent.memory.vector.pinecone.environment")
    Optional<String> environment;

    @ConfigProperty(name = "silat.agent.memory.vector.pinecone.index-name")
    Optional<String> indexName;

    @Override
    public Uni<String> store(
            String sessionId,
            String tenantId,
            Message message,
            float[] embedding) {

        if (!isConfigured()) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Pinecone not configured"));
        }

        String vectorId = UUID.randomUUID().toString();
        String url = buildUrl("/vectors/upsert");

        JsonObject metadata = new JsonObject()
                .put("sessionId", sessionId)
                .put("tenantId", tenantId)
                .put("role", message.role())
                .put("content", message.content())
                .put("timestamp", message.timestamp().toEpochMilli());

        JsonObject vector = new JsonObject()
                .put("id", vectorId)
                .put("values", new JsonArray(toList(embedding)))
                .put("metadata", metadata);

        JsonObject requestBody = new JsonObject()
                .put("vectors", new JsonArray().add(vector))
                .put("namespace", makeNamespace(tenantId));

        return Uni.createFrom().completionStage(webClient
                .postAbs(url)
                .putHeader("Api-Key", apiKey.get())
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(requestBody)
                .toCompletionStage())
                .map(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Pinecone error: " + response.statusCode());
                    }
                    LOG.debug("Stored vector: {}", vectorId);
                    return vectorId;
                });
    }

    @Override
    public Uni<List<SimilarMessage>> search(
            String sessionId,
            String tenantId,
            float[] queryEmbedding,
            int limit) {

        return searchWithFilter(sessionId, tenantId, queryEmbedding,
                Map.of("sessionId", sessionId), limit);
    }

    @Override
    public Uni<List<SimilarMessage>> searchWithFilter(
            String sessionId,
            String tenantId,
            float[] queryEmbedding,
            Map<String, Object> filters,
            int limit) {

        if (!isConfigured()) {
            return Uni.createFrom().item(List.of());
        }

        String url = buildUrl("/query");

        JsonObject filterObj = new JsonObject();
        filters.forEach(filterObj::put);

        JsonObject requestBody = new JsonObject()
                .put("vector", new JsonArray(toList(queryEmbedding)))
                .put("topK", limit)
                .put("includeMetadata", true)
                .put("namespace", makeNamespace(tenantId))
                .put("filter", filterObj);

        return Uni.createFrom().completionStage(webClient
                .postAbs(url)
                .putHeader("Api-Key", apiKey.get())
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(requestBody)
                .toCompletionStage())
                .map(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Pinecone query error: " + response.statusCode());
                    }

                    JsonObject json = response.bodyAsJsonObject();
                    JsonArray matches = json.getJsonArray("matches");

                    List<SimilarMessage> results = new ArrayList<>();
                    for (int i = 0; i < matches.size(); i++) {
                        JsonObject match = matches.getJsonObject(i);
                        JsonObject metadata = match.getJsonObject("metadata");

                        Message message = new Message(
                                metadata.getString("role"),
                                metadata.getString("content"),
                                null,
                                null,
                                Instant.ofEpochMilli(metadata.getLong("timestamp")));

                        results.add(new SimilarMessage(
                                match.getString("id"),
                                message,
                                match.getDouble("score"),
                                metadata.getMap()));
                    }

                    LOG.debug("Found {} similar messages", results.size());
                    return results;
                });
    }

    @Override
    public Uni<Void> deleteSession(String sessionId, String tenantId) {
        String url = buildUrl("/vectors/delete");

        JsonObject requestBody = new JsonObject()
                .put("filter", new JsonObject()
                        .put("sessionId", sessionId)
                        .put("tenantId", tenantId))
                .put("namespace", makeNamespace(tenantId));

        return Uni.createFrom().completionStage(webClient
                .postAbs(url)
                .putHeader("Api-Key", apiKey.get())
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(requestBody)
                .toCompletionStage())
                .map(response -> null);
    }

    @Override
    public Uni<VectorStoreStats> getStats(String tenantId) {
        String url = buildUrl("/describe_index_stats");

        return Uni.createFrom().completionStage(webClient
                .getAbs(url)
                .putHeader("Api-Key", apiKey.get())
                .send()
                .toCompletionStage())
                .map(response -> {
                    JsonObject json = response.bodyAsJsonObject();
                    return new VectorStoreStats(
                            json.getLong("totalVectorCount"),
                            json.getInteger("dimension"),
                            "pinecone");
                });
    }

    private boolean isConfigured() {
        return apiKey.isPresent() && environment.isPresent() && indexName.isPresent();
    }

    private String buildUrl(String path) {
        return String.format("https://%s-%s.svc.%s.pinecone.io%s",
                indexName.get(), "default", environment.get(), path);
    }

    private String makeNamespace(String tenantId) {
        return "tenant_" + tenantId;
    }

    private List<Double> toList(float[] array) {
        List<Double> list = new ArrayList<>(array.length);
        for (float value : array) {
            list.add((double) value);
        }
        return list;
    }
}
