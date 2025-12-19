package tech.kayys.wayang.memory.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.model.MemoryBackend;
import tech.kayys.wayang.model.MemoryConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class InMemoryBackend implements MemoryBackend {

    private final Map<String, List<MemoryEntry>> storage = new ConcurrentHashMap<>();

    @Override
    public Uni<Void> store(String sessionId, MemoryEntry entry) {
        storage.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(entry);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<List<MemoryEntry>> retrieve(String sessionId, int limit) {
        List<MemoryEntry> entries = storage.getOrDefault(sessionId, List.of());
        return Uni.createFrom().item(
                entries.stream()
                        .sorted(Comparator.comparingLong(MemoryEntry::getTimestamp).reversed())
                        .limit(limit)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<List<MemoryEntry>> search(String sessionId, String query, int limit) {
        List<MemoryEntry> entries = storage.getOrDefault(sessionId, List.of());
        return Uni.createFrom().item(
                entries.stream()
                        .filter(e -> e.getContent().toLowerCase().contains(query.toLowerCase()))
                        .limit(limit)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<Void> clear(String sessionId) {
        storage.remove(sessionId);
        return Uni.createFrom().voidItem();
    }

    @Override
    public tech.kayys.wayang.memory.model.StorageBackend getSupportedBackend() {
        return MemoryConfig.StorageBackend.IN_MEMORY;
    }
}
