package tech.kayys.wayang.agent.dto;

public record ResourceRequirements(
        String cpu,
        String memory,
        int timeoutMs) {
}
