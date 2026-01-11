package tech.kayys.wayang.billing.model;

public record MarketPricing(
    double marketAverage,
    double competitorAverage,
    double ourPosition
) {}
