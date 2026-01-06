package tech.kayys.wayang.agent.dto;

import java.util.Map;

/**
 * Coordination Config
 */
public record CoordinationConfig(
    int maxRounds,
    long roundTimeoutMs,
    ConsensusStrategy consensusStrategy,
    Map<String, Object> customConfig
) {
    
    public static CoordinationConfig createDefault() {
        return new CoordinationConfig(
            5,
            60000L,
            ConsensusStrategy.MAJORITY,
            Map.of()
        );
    }
}

