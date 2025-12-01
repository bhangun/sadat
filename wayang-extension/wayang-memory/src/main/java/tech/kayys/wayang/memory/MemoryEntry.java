@Value
@Builder
public class MemoryEntry {
    String memoryId;
    MemoryType type;
    UUID tenantId;
    String owner;
    String content;
    float[] embedding;
    Map<String, Object> metadata;
    double score;
    double relevance;
    Instant createdAt;
    Duration ttl;
}