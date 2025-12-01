
// Knowledge Graph Models
@Value
@Builder
public class Triple {
    String id;
    Entity subject;
    String predicate;
    Entity object;
    double confidence;
    String source;
    UUID documentId;
    Instant createdAt;
}