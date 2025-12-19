package tech.kayys.wayang.agent.dto;

public record Pricing(
        double inputCostPer1k,
        double outputCostPer1k,
        String currency,
        String model) {
}
