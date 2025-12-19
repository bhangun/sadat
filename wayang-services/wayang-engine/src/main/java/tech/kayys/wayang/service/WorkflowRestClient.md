package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.MultipartForm;
import tech.kayys.wayang.schema.ExecutionRequest;
import tech.kayys.wayang.schema.ExecutionResponse;
import tech.kayys.wayang.schema.ExecutionStatus;
import tech.kayys.wayang.schema.PublishRequest;
import tech.kayys.wayang.schema.PublishResponse;
import tech.kayys.wayang.schema.WorkflowDTO;

/**
 * REST Client for Workflow Service operations
 */
@Path("/api/v1/workflows")
@RegisterRestClient(configKey = "workflow-service-rest")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface WorkflowRestClient {

        @POST
        @Path("/{id}/execute")
        Uni<ExecutionResponse> executeWorkflow(
                        @PathParam("id") String workflowId,
                        @HeaderParam("X-Tenant-ID") String tenantId,
                        ExecutionRequest request);

        @GET
        @Path("/executions/{executionId}")
        Uni<ExecutionStatus> getExecutionStatus(
                        @PathParam("executionId") String executionId,
                        @HeaderParam("X-Tenant-ID") String tenantId);

        @POST
        @Path("/{id}/publish")
        Uni<PublishResponse> publishWorkflow(
                        @PathParam("id") String workflowId,
                        @HeaderParam("X-Tenant-ID") String tenantId,
                        PublishRequest request);

        @GET
        @Path("/{id}/export")
        @Produces("application/zip")
        Uni<File> exportWorkflow(
                        @PathParam("id") String workflowId,
                        @HeaderParam("X-Tenant-ID") String tenantId,
                        @QueryParam("format") String format);

        @POST
        @Path("/import")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        Uni<WorkflowDTO> importWorkflow(
                        @MultipartForm WorkflowImportForm form,
                        @HeaderParam("X-Tenant-ID") String tenantId);
}
