package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.service.ProvenanceService;
import tech.kayys.wayang.workflow.service.ReportType;
import tech.kayys.wayang.workflow.v1.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@GrpcService
public class ProvenanceGrpcService implements tech.kayys.wayang.workflow.v1.ProvenanceService {

    @Inject
    ProvenanceService provenanceService;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Uni<ProvenanceReport> getProvenanceReport(GetProvenanceReportRequest request) {
        ReportType type = ReportType.SUMMARY;
        if (request.getReportType() != null && !request.getReportType().isEmpty()) {
            try {
                type = ReportType.valueOf(request.getReportType());
            } catch (IllegalArgumentException e) {
                // Default
            }
        }

        return provenanceService.generateReport(request.getRunId(), type)
                .map(report -> {
                    String json = "{}";
                    try {
                        if (report != null) {
                            json = objectMapper.writeValueAsString(report);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    return ProvenanceReport.newBuilder()
                            .setRunId(report.getRunId())
                            .setGeneratedAt(
                                    report.getGeneratedAt() != null ? report.getGeneratedAt().toEpochMilli() : 0)
                            .setReportJson(json)
                            .build();
                });
    }
}
