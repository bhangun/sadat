package tech.kayys.wayang.automation.dto;

public record BusinessProcessResponse(
                String id,
                String name,
                ProcessType processType,
                String status) {
}
