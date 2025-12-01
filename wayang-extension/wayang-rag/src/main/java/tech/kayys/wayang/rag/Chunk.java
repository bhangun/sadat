@Value
@Builder
public class Chunk {
    String chunkId;
    String documentId;
    String text;
    int tokenCount;
    Map<String, Object> metadata;
    ChunkPosition position;
}