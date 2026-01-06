package tech.kayys.wayang.plugin.dto;

import io.smallrye.mutiny.Uni;

public non-sealed interface AnalyticsAgent extends AgentPlugin {
    Uni<AnalyticsResult> analyze(DataSet data, AnalyticsContext context);
}