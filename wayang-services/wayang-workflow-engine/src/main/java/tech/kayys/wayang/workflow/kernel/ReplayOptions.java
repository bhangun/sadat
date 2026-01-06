package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Options for replaying workflow executions.
 * Enables debugging, testing, and recovery scenarios.
 */
@Data
@Builder(toBuilder = true)
public class ReplayOptions {

    @Builder.Default
    private final boolean fromBeginning = false;

    @Builder.Default
    private final boolean skipCompleted = false;

    @Builder.Default
    private final boolean forceReplay = false;

    @Builder.Default
    private final boolean dryRun = false;

    private final Instant fromTimestamp;
    private final Instant toTimestamp;

    @Builder.Default
    private final Map<String, Object> overrides = Map.of();

    @Builder.Default
    private final Map<String, Object> metadata = Map.of();

    @Builder.Default
    private final String replayStrategy = "exact"; // "exact", "recreate", "resume"

    @Builder.Default
    private final boolean preserveTiming = false;

    @Builder.Default
    private final boolean recordDifferences = true;

    @JsonCreator
    public ReplayOptions(
            @JsonProperty("fromBeginning") boolean fromBeginning,
            @JsonProperty("skipCompleted") boolean skipCompleted,
            @JsonProperty("forceReplay") boolean forceReplay,
            @JsonProperty("dryRun") boolean dryRun,
            @JsonProperty("fromTimestamp") Instant fromTimestamp,
            @JsonProperty("toTimestamp") Instant toTimestamp,
            @JsonProperty("overrides") Map<String, Object> overrides,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("replayStrategy") String replayStrategy,
            @JsonProperty("preserveTiming") boolean preserveTiming,
            @JsonProperty("recordDifferences") boolean recordDifferences) {

        this.fromBeginning = fromBeginning;
        this.skipCompleted = skipCompleted;
        this.forceReplay = forceReplay;
        this.dryRun = dryRun;
        this.fromTimestamp = fromTimestamp;
        this.toTimestamp = toTimestamp;
        this.overrides = overrides != null ? Map.copyOf(overrides) : Map.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.replayStrategy = replayStrategy != null ? replayStrategy : "exact";
        this.preserveTiming = preserveTiming;
        this.recordDifferences = recordDifferences;
    }

    public static ReplayOptions debug() {
        return ReplayOptions.builder()
                .dryRun(true)
                .recordDifferences(true)
                .replayStrategy("exact")
                .build();
    }

    public static ReplayOptions recovery() {
        return ReplayOptions.builder()
                .fromBeginning(true)
                .forceReplay(true)
                .skipCompleted(true)
                .replayStrategy("recreate")
                .build();
    }

    public static ReplayOptions test() {
        return ReplayOptions.builder()
                .dryRun(true)
                .recordDifferences(true)
                .replayStrategy("exact")
                .metadata(Map.of("purpose", "testing"))
                .build();
    }

    public boolean hasTimeRange() {
        return fromTimestamp != null || toTimestamp != null;
    }

    public boolean hasOverrides() {
        return !overrides.isEmpty();
    }
}