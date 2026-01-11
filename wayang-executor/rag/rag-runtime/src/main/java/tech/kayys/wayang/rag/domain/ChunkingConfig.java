package tech.kayys.silat.executor.rag.domain;

public class ChunkingConfig {
    private final ChunkingStrategy strategy;
    private final int chunkSize;
    private final int chunkOverlap;

    public ChunkingConfig(ChunkingStrategy strategy, int chunkSize, int chunkOverlap) {
        this.strategy = strategy;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public static ChunkingConfig defaults() {
        return new ChunkingConfig(ChunkingStrategy.RECURSIVE, 512, 50);
    }

    // Getters
    public ChunkingStrategy strategy() { return strategy; }
    public int chunkSize() { return chunkSize; }
    public int chunkOverlap() { return chunkOverlap; }
}