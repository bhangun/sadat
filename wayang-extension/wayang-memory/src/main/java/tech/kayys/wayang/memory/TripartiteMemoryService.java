
@ApplicationScoped
public class TripartiteMemoryService implements MemoryService {
    @Inject EpisodicMemoryStore episodicStore;
    @Inject SemanticMemoryStore semanticStore;
    @Inject ProceduralMemoryStore proceduralStore;
    @Inject MemoryScorer memoryScorer;
    @Inject MemoryConsolidator memoryConsolidator;
    
    @Override
    public void write(MemoryEntry entry) {
        // Score memory
        double score = memoryScorer.score(entry);
        entry = entry.withScore(score);
        
        // Route to appropriate store
        switch (entry.getType()) {
            case EPISODIC:
                episodicStore.store(entry);
                break;
            case SEMANTIC:
                semanticStore.store(entry);
                break;
            case PROCEDURAL:
                proceduralStore.store(entry);
                break;
        }
    }
    
    @Override
    public List<MemoryEntry> read(MemoryQuery query) {
        List<MemoryEntry> results = new ArrayList<>();
        
        // Query each store
        if (query.getTypes().contains(MemoryType.EPISODIC)) {
            results.addAll(episodicStore.query(query));
        }
        if (query.getTypes().contains(MemoryType.SEMANTIC)) {
            results.addAll(semanticStore.query(query));
        }
        if (query.getTypes().contains(MemoryType.PROCEDURAL)) {
            results.addAll(proceduralStore.query(query));
        }
        
        // Score and sort
        return results.stream()
            .map(entry -> entry.withRelevance(
                memoryScorer.scoreRelevance(entry, query)
            ))
            .sorted(Comparator.comparing(MemoryEntry::getRelevance).reversed())
            .limit(query.getTopK())
            .collect(Collectors.toList());
    }
}