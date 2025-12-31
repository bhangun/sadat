package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.workflow.service.ProvenanceService;
import tech.kayys.wayang.workflow.service.ReportType;
import tech.kayys.wayang.workflow.v1.GetProvenanceReportRequest;
import tech.kayys.wayang.workflow.v1.ProvenanceReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.wayang.workflow.security.annotations.ControlPlaneSecured;

@GrpcService
@ControlPlaneSecured
public class ProvenanceGrpcService implements tech.kayys.wayang.workflow.v1.ProvenanceService {

    private static final Logger LOG = Logger.getLogger(ProvenanceGrpcService.class);

    @Inject
    ProvenanceService provenanceService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SecurityIdentity securityIdentity;

    private String getTenantId() {
        if (securityIdentity.isAnonymous()) {
            // Ideally unreachable due to interceptor, but good safety
            // If ServiceAccount or UserJWT, Identity should be populated by Quarkus if
            // configured
            // BUT we rely on AuthInterceptor logic principally.
            // If we didn't populate SecurityIdentity manually, it might be anonymous
            // IF Quarkus OIDC didn't pick it up.
            // Note: User feedback said "Control Plane ... OIDC".
        }
        return securityIdentity.getPrincipal().getName();
    }

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
