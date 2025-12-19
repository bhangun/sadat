package tech.kayys.wayang.agent.dto;

import java.util.Map;

public record BenchmarkRequest(
        int iterations,
        Map<String, Object> testInputs,
        String testName) {
}