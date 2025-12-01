@Value
@Builder
public class Document {
    String id;
    String content;
    DocumentType type;
    Map<String, Object> metadata;
    Instant createdAt;
}