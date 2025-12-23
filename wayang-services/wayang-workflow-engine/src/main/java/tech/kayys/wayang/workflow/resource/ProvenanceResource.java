package tech.kayys.wayang.workflow.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import tech.kayys.wayang.workflow.service.ProvenanceService;
import tech.kayys.wayang.workflow.api.dto.ErrorResponse;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.service.WorkflowRunManager;

import java.util.Map;

import tech.kayys.wayang.workflow.service.ReportType;

/**
 * ProvenanceResource - REST API for audit and provenance queries
 */
@Path("/api/v1/provenance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Provenance & Audit", description = "Audit trail and provenance tracking")
public class ProvenanceResource {

    private static final Logger LOG = Logger.getLogger(ProvenanceResource.class);

    @Inject
    ProvenanceService provenanceService;

    @Inject
    WorkflowRunManager runManager;

    /**
     * Get provenance report for a run
     */
    @GET
    @Path("/runs/{runId}/report")
    @Operation(summary = "Get provenance report", description = "Generate comprehensive provenance report for a run")
    public Uni<Response> getProvenanceReport(@PathParam("runId") String runId) {
        // Need to get the run first to pass to service
        return runManager.getRun(runId)
                .onItem().transformToUni(run -> {
                    if (run == null) {
                        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND)
                                .entity(new ErrorResponse("NOT_FOUND", "Run not found: " + runId))
                                .build());
                    }

                    return provenanceService.generateReport(run.getRunId(), ReportType.SUMMARY)
                            .map(report -> Response.ok(report).build());
                })
                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("REPORT_FAILED", th.getMessage()))
                        .build());
    }
}
