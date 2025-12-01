
/**
 * Memory service - manages agent memory
 */
@ApplicationScoped
public class MemoryService {
    private final EpisodicMemory episodicMemory;
    private final SemanticMemory semanticMemory;
    private final ProceduralMemory proceduralMemory;
    private final MemoryScorer scorer;
    private final MemoryConsolidator consolidator;
    
    /**
     * Store episodic memory
     */
    public void storeEpisode(Episode episode) {
        episodicMemory.store(episode);
    }
    
    /**
     * Query semantic memory
     */
    public List<MemoryEntry> querySemanticMemory(String query, int topK) {
        float[] queryEmbedding = embeddingService.embed(query);
        List<MemoryEntry> candidates = semanticMemory.search(queryEmbedding, topK * 2);
        
        // Score and re-rank
        List<ScoredMemory> scored = candidates.stream()
                .map(entry -> scorer.score(entry, query))
                .sorted(Comparator.comparing(ScoredMemory::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
        
        return scored.stream()
                .map(ScoredMemory::getEntry)
                .collect(Collectors.toList());
    }
    
    /**
     * Consolidate episodic memories into semantic memory
     */
    public void consolidate() {
        List<Episode> episodes = episodicMemory.getRecentEpisodes();
        consolidator.consolidate(episodes, semanticMemory);
    }
}