package tech.kayys.wayang.agent.dto;

import java.util.List;

public record AgentBuilderList(
        List<AgentBuilderSummary> agents,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}