
public interface MemoryService {
    void write(MemoryEntry entry);
    List<MemoryEntry> read(MemoryQuery query);
    void delete(String memoryId);
    void consolidate(ConsolidationRequest request);
}