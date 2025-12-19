package tech.kayys.wayang.agent.schema;

public sealed interface AgentExtension
        permits RiskProfileExtension, VectorStoreConfig, CustomJsonExtension {
}
