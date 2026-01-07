package tech.kayys.wayang.billing.model;

public enum RiskLevel {
    LOW,      // < 20% probability
    MEDIUM,   // 20-50% probability
    HIGH,     // 50-80% probability
    CRITICAL  // > 80% probability
}
