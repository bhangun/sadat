
@Value
@Builder
public class ScoredChunk {
    Chunk chunk;
    double score;
    ScoreBreakdown breakdown;
}