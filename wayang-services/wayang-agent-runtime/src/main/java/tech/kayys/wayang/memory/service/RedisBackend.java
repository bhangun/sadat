package tech.kayys.wayang.memory.service;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.model.MemoryBackend;
import tech.kayys.wayang.model.MemoryBackend.MemoryEntry;
import tech.kayys.wayang.model.MemoryConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class RedisBackend implements MemoryBackend {

    @Inject
    ReactiveRedisDataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    private ReactiveListCommands<String, String> listCommands;

    @jakarta.annotation.PostConstruct
    void init() {
        listCommands = dataSource.list(String.class, String.class);
    }

    @Override
    public Uni<Void> store(String sessionId, MemoryEntry entry) {
        String key = "memory:" + sessionId;

        return Uni.createFrom().item(() -> {
            try {
                return objectMapper.writeValueAsString(entry);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize memory entry", e);
            }
        })
                .chain(json -> listCommands.rpush(key, json))
                .replaceWithVoid();
    }

    @Override
    public Uni<List<MemoryEntry>> retrieve(String sessionId, int limit) {
        String key = "memory:" + sessionId;

        return listCommands.lrange(key, -limit, -1)
                .map(jsonList -> jsonList.stream()
                        .map(json -> {
                            try {
                                return objectMapper.readValue(json, MemoryEntry.class);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to deserialize memory entry", e);
                            }
                        })
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<List<MemoryEntry>> search(String sessionId, String query, int limit) {
        // Simple implementation - retrieve all and filter
        // For production, consider using Redis Search
        return retrieve(sessionId, 1000)
                .map(entries -> entries.stream()
                        .filter(e -> e.getContent().toLowerCase().contains(query.toLowerCase()))
                        .limit(limit)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<Void> clear(String sessionId) {
        String key = "memory:" + sessionId;
        return dataSource.key().del(key).replaceWithVoid();
    }

    @Override
    public tech.kayys.wayang.memory.model.StorageBackend getSupportedBackend() {
        return MemoryConfig.StorageBackend.REDIS;
    }
}