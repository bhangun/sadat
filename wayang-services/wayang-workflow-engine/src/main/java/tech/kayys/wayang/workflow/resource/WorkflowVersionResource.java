package tech.kayys.wayang.workflow.resource;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.workflow.version.dto.CreateVersionRequest;
import tech.kayys.wayang.workflow.version.dto.DeprecateRequest;
import tech.kayys.wayang.workflow.version.dto.MigrationRequest;
import tech.kayys.wayang.workflow.version.dto.PublishOptions;
import tech.kayys.wayang.workflow.version.dto.PublishRequest;
import tech.kayys.wayang.workflow.version.dto.RollbackRequest;
import tech.kayys.wayang.workflow.version.dto.VersionListItem;
import tech.kayys.wayang.workflow.version.dto.VersionRequest;
import tech.kayys.wayang.workflow.version.dto.VersionResponse;
import tech.kayys.wayang.workflow.version.model.WorkflowVersion;
import tech.kayys.wayang.workflow.version.service.WorkflowVersionManager;

/**
 * WorkflowVersionResource: REST API for workflow versioning.
 * 
 * Endpoints:
 * - POST /api/v1/workflows/{id}/versions - Create version
 * - GET /api/v1/workflows/{id}/versions - List versions
 * - GET /api/v1/workflows/{id}/versions/{version} - Get version
 * - POST /api/v1/workflows/{id}/versions/{version}/publish - Publish
 * - POST /api/v1/workflows/{id}/versions/{version}/promote - Promote canary
 * - POST /api/v1/workflows/{id}/versions/{version}/rollback - Rollback
 * - POST /api/v1/workflows/{id}/versions/{version}/deprecate - Deprecate
 * - POST /api/v1/workflows/{id}/migrate - Migrate versions
 * - GET /api/v1/workflows/{id}/versions/compare - Compare versions
 */
@Path("/api/v1/workflows/{workflowId}/versions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowVersionResource {

        private static final Logger LOG = Logger.getLogger(WorkflowVersionResource.class);

        @Inject
        WorkflowVersionManager versionManager;

        /**
         * Create a new workflow version.
         */
        @POST
        public Uni<Response> createVersion(
                        @PathParam("workflowId") String workflowId,
                        CreateVersionRequest request) {

                LOG.infof("Creating version %s for workflow %s",
                                request.version(), workflowId);

                VersionRequest versionRequest = new VersionRequest(
                                workflowId,
                                request.version(),
                                request.previousVersion(),
                                request.createdBy());

                return versionManager.createVersion(versionRequest)
                                .map(version -> Response
                                                .status(Response.Status.CREATED)
                                                .entity(toVersionResponse(version))
                                                .build())
                                .onFailure().recoverWithItem(error -> {
                                        LOG.error("Failed to create version", error);
                                        return Response
                                                        .status(Response.Status.BAD_REQUEST)
                                                        .entity(Map.of("error", error.getMessage()))
                                                        .build();
                                });
        }

        /**
         * List all versions for a workflow.
         */
        @GET
        public Uni<Response> listVersions(@PathParam("workflowId") String workflowId) {
                return versionManager.getVersionHistory(workflowId)
                                .map(versions -> {
                                        List<VersionListItem> items = versions.stream()
                                                        .map(v -> new VersionListItem(
                                                                        v.getVersionId(),
                                                                        v.getVersion(),
                                                                        v.getStatus().name(),
                                                                        v.getBreakingChanges().size(),
                                                                        v.getCreatedAt()))
                                                        .toList();

                                        return Response.ok(items).build();
                                });
        }

        /**
         * Get specific version details.
         */
        @GET
        @Path("/{version}")
        public Uni<Response> getVersion(
                        @PathParam("workflowId") String workflowId,
                        @PathParam("version") String version) {

                // TODO: Implement version lookup
                return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND).build());
        }

        /**
         * Publish a workflow version.
         */
        @POST
        @Path("/{versionId}/publish")
        public Uni<Response> publishVersion(
                        @PathParam("workflowId") String workflowId,
                        @PathParam("versionId") String versionId,
                        PublishRequest request) {

                PublishOptions options = new PublishOptions(
                                request.canaryDeployment(),
                                request.canaryPercentage(),
                                request.autoMigrate(),
                                request.publishedBy());

                return versionManager.publishVersion(versionId, options)
                                .map(version -> Response.ok(toVersionResponse(version)).build())
                                .onFailure().recoverWithItem(error -> Response.status(Response.Status.BAD_REQUEST)
                                                .entity(Map.of("error", error.getMessage()))
                                                .build());
        }

        /**
         * Promote canary to full deployment.
         */
        @POST
        @Path("/{versionId}/promote")
        public Uni<Response> promoteCanary(
                        @PathParam("workflowId") String workflowId,
                        @PathParam("versionId") String versionId) {

                return versionManager.promoteCanary(versionId)
                                .map(version -> Response.ok(toVersionResponse(version)).build());
        }

        /**
         * Rollback canary deployment.
         */
        @POST
        @Path("/{versionId}/rollback")
        public Uni<Response> rollbackCanary(
                        @PathParam("workflowId") String workflowId,
                        @PathParam("versionId") String versionId,
                        RollbackRequest request) {

                return versionManager.rollbackCanary(versionId, request.reason())
                                .map(v -> Response.noContent().build());
        }

        /**
         * Deprecate a version.
         */
        @POST
        @Path("/{versionId}/deprecate")
        public Uni<Response> deprecateVersion(
                        @PathParam("workflowId") String workflowId,
                        @PathParam("versionId") String versionId,
                        DeprecateRequest request) {

                return versionManager.deprecateVersion(
                                versionId,
                                request.reason(),
                                request.sunsetDate())
                                .map(version -> Response.ok(toVersionResponse(version)).build());
        }

        /**
         * Migrate workflow runs between versions.
         */
        @POST
        @Path("/migrate")
        public Uni<Response> migrateVersion(
                        @PathParam("workflowId") String workflowId,
                        MigrationRequest request) {

                return versionManager.migrateVersion(
                                workflowId,
                                request.fromVersion(),
                                request.toVersion())
                                .map(result -> Response.ok(result).build());
        }

        /**
         * Compare two versions.
         */
        @GET
        @Path("/compare")
        public Uni<Response> compareVersions(
                        @PathParam("workflowId") String workflowId,
                        @QueryParam("version1") String version1,
                        @QueryParam("version2") String version2) {

                return versionManager.compareVersions(workflowId, version1, version2)
                                .map(diff -> Response.ok(diff).build());
        }

        private VersionResponse toVersionResponse(WorkflowVersion version) {
                return new VersionResponse(
                                version.getVersionId(),
                                version.getWorkflowId(),
                                version.getVersion(),
                                version.getPreviousVersion(),
                                version.getStatus().name(),
                                version.getBreakingChanges(),
                                version.getDeprecationWarnings(),
                                version.getCanaryPercentage(),
                                version.getCreatedAt(),
                                version.getPublishedAt());
        }
}