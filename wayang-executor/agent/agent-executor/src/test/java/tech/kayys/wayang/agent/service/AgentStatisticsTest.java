package tech.kayys.wayang.agent.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AgentStatisticsTest {

    @Test
    void testStatistics() {
        AgentStatistics stats = new AgentStatistics(10, 8, 1000, 150.0);

        assertEquals(80.0, stats.successRate());

        Map<String, Object> map = stats.toMap();
        assertEquals(10L, map.get("totalExecutions"));
        assertEquals(80.0, map.get("successRate"));
    }
}
