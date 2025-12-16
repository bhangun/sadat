package tech.kayys.wayang.client;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * VersionServiceClient - Client for Version Service
 */
@RegisterRestClient(configKey = "version-service")
@Path("/api/v1/versions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VersionServiceClient {

        /**
         * Create immutable version of workflow
         */
        @POST
        @Path("/workflows")
        @Retry(maxRetries = 2)
        @Timeout(value = 10000)
        Uni<WorkflowVersion> createVersion(CreateVersionRequest request);

        /**
         * Get version by ID
         */
        @GET
        @Path("/workflows/{versionId}")
        @Timeout(value = 5000)
        Uni<WorkflowVersion> getVersion(@PathParam("versionId") String versionId);

        /**
         * List versions for workflow
         */
        @GET
        @Path("/workflows")
        @QueryParam("workflowId")
        @Timeout(value = 5000)
        Uni<List<WorkflowVersion>> listVersions(@QueryParam("workflowId") String workflowId);

        /**
         * Compare two versions
         */
        @POST
        @Path("/workflows/compare")
        @Timeout(value = 10000)
        Uni<VersionDiff> compareVersions(CompareRequest request);

        // DTOs
        record CreateVersionRequest(
                        String workflowId,
                        String name,
                        String version,
                        String tenantId,
                        Object content,
                        Map<String, Object> metadata) {
        }

        record WorkflowVersion(
                        String id,
                        String workflowId,
                        String version,
                        String name,
                        Instant createdAt,
                        String createdBy,
                        Object content,
                        Map<String, Object> metadata) {
        }

        record CompareRequest(
                        String versionId1,
                        String versionId2) {
        }

        record VersionDiff(
                        List<Change> changes,
                        DiffSummary summary) {
        }

        record Change(
                        String type, // ADDED, REMOVED, MODIFIED
                        String path,
                        Object oldValue,
                        Object newValue) {
        }

        record DiffSummary(
                        int additions,
                        int deletions,
                        int modifications) {
        }
}
