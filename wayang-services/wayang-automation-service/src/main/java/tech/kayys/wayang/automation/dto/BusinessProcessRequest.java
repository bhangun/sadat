package tech.kayys.wayang.automation.dto;

import java.util.List;

public record BusinessProcessRequest(
        String name,
        String description,
        String tenantId,
        ProcessType processType,
        List<ProcessStep> steps) {
}
