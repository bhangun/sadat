package tech.kayys.wayang.resource;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.domain.WorkflowDraft;
import tech.kayys.wayang.domain.WorkflowLock;
import tech.kayys.wayang.model.LogicDefinition;
import tech.kayys.wayang.model.RuntimeConfig;
import tech.kayys.wayang.model.UIDefinition;
import tech.kayys.wayang.model.ValidationResult;
import tech.kayys.wayang.schema.ExecutionRequest;
import tech.kayys.wayang.schema.ExecutionStatus;
import tech.kayys.wayang.schema.PublishRequest;
import tech.kayys.wayang.schema.CodeGenRequest;
import tech.kayys.wayang.schema.WorkflowImportForm;
import tech.kayys.wayang.schema.ArtifactUploadForm;
import tech.kayys.wayang.service.DraftService;
import tech.kayys.wayang.service.LockService;
import tech.kayys.wayang.service.ValidationService;
import tech.kayys.wayang.service.WorkflowService;
import tech.kayys.wayang.service.WorkflowExecutionService;
import tech.kayys.wayang.service.WorkflowPublishService;
import tech.kayys.wayang.service.WorkflowImportExportService;
import tech.kayys.wayang.tenant.TenantContext;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.ResponseStatus;

/**
 * WorkflowResource - REST API for workflow management
 */
@Path("/api/v1/workspaces/{workspaceId}/workflows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Workflows", description = "Workflow management operations")
@RolesAllowed({ "admin", "designer" })
public class WorkflowResource {

        private static final Logger LOG = Logger.getLogger(WorkflowResource.class);

        @Inject
        WorkflowService workflowService;

        @Inject
        ValidationService validationService;

        @Inject
        LockService lockService;

        @Inject
        DraftService draftService;

        @Inject
        TenantContext tenantContext;

        @Inject
        WorkflowExecutionService executionService;

        @Inject
        WorkflowPublishService publishService;

        @Inject
        WorkflowImportExportService importExportService;

        /**
         * Execute workflow
         */
        @POST
        @Path("/{id}/execute")
        @Operation(summary = "Execute workflow", description = "Start workflow execution")
        public Uni<Response> executeWorkflow(
                        @PathParam("id") String workflowId,
                        @HeaderParam("X-Tenant-ID") String tenantId,
                        ExecutionRequest request) {

                LOG.infof("REST: Execute workflow %s (tenant=%s)", workflowId, tenantId);

                return executionService.execute(workflowId, request, tenantId)
                                .map(execution -> Response.accepted()
                                                .entity(execution)
                                                .header("X-Execution-ID", execution.getId())
                                                .build());
        }

        /**
         * Get execution status
         */
        @GET
        @Path("/executions/{executionId}")
        @Operation(summary = "Get execution status")
        public Uni<ExecutionStatus> getExecutionStatus(
                        @PathParam("executionId") String executionId,
                        @HeaderParam("X-Tenant-ID") String tenantId) {

                return executionService.getStatus(executionId, tenantId);
        }

        /**
         * Cancel execution
         */
        @POST
        @Path("/executions/{executionId}/cancel")
        @Operation(summary = "Cancel workflow execution")
        public Uni<Response> cancelExecution(
                        @PathParam("executionId") String executionId,
                        @HeaderParam("X-Tenant-ID") String tenantId) {

                return executionService.cancel(executionId, tenantId)
                                .map(success -> Response.ok()
                                                .entity(Map.of("cancelled", success))
                                                .build());
        }

        /**
         * Publish workflow
         */
        @POST
        @Path("/{id}/publish")
        @Operation(summary = "Publish workflow", description = "Publish workflow to production")
        public Uni<Response> publishWorkflow(
                        @PathParam("id") String workflowId,
                        @HeaderParam("X-Tenant-ID") String tenantId,
                        @HeaderParam("X-User-ID") String userId,
                        PublishRequest request) {

                LOG.infof("REST: Publish workflow %s", workflowId);

                return publishService.publish(workflowId, request, tenantId, userId)
                                .map(result -> Response.ok()
                                                .entity(result)
                                                .build());
        }

        /**
         * Export workflow
         */
        @GET
        @Path("/{id}/export")
        @Produces("application/zip")
        @Operation(summary = "Export workflow", description = "Export workflow with dependencies")
        public Uni<Response> exportWorkflow(
                        @PathParam("id") String workflowId,
                        @HeaderParam("X-Tenant-ID") String tenantId,
                        @QueryParam("format") @DefaultValue("json") String format) {

                LOG.infof("REST: Export workflow %s (format=%s)", workflowId, format);

                return importExportService.export(workflowId, format, tenantId)
                                .map(exportFile -> Response.ok(exportFile)
                                                .header("Content-Disposition",
                                                                "attachment; filename=workflow-" + workflowId + ".zip")
                                                .build());
        }

        /**
         * Import workflow
         */
        @POST
        @Path("/import")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Operation(summary = "Import workflow", description = "Import workflow from file")
        public Uni<Response> importWorkflow(
                        @MultipartForm WorkflowImportForm form,
                        @HeaderParam("X-Tenant-ID") String tenantId,
                        @HeaderParam("X-User-ID") String userId) {

                LOG.infof("REST: Import workflow (tenant=%s, user=%s)", tenantId, userId);

                return importExportService.importWorkflow(form.file, tenantId, userId)
                                .map(workflow -> Response.created(
                                                URI.create("/api/v1/workflows/" + workflow.id))
                                                .entity(workflow)
                                                .build());
        }

        /**
         * Upload document/artifact
         */
        @POST
        @Path("/{id}/artifacts")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Operation(summary = "Upload artifact", description = "Upload document or artifact")
        public Uni<Response> uploadArtifact(
                        @PathParam("id") String workflowId,
                        @MultipartForm ArtifactUploadForm form,
                        @HeaderParam("X-Tenant-ID") String tenantId) {

                LOG.infof("REST: Upload artifact to workflow %s", workflowId);

                return importExportService.uploadArtifact(workflowId, form, tenantId)
                                .map(artifact -> Response.created(
                                                URI.create("/api/v1/workflows/" + workflowId + "/artifacts/"
                                                                + artifact.getId()))
                                                .entity(artifact)
                                                .build());
        }

        /**
         * Download artifact
         */
        @GET
        @Path("/{id}/artifacts/{artifactId}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        @Operation(summary = "Download artifact")
        public Uni<Response> downloadArtifact(
                        @PathParam("id") String workflowId,
                        @PathParam("artifactId") String artifactId,
                        @HeaderParam("X-Tenant-ID") String tenantId) {

                return importExportService.downloadArtifact(workflowId, artifactId, tenantId)
                                .map(file -> Response.ok(file)
                                                .header("Content-Disposition",
                                                                "attachment; filename=" + file.getName())
                                                .build());
        }

        /**
         * Generate standalone agent code
         */
        @POST
        @Path("/{id}/codegen")
        @Operation(summary = "Generate standalone code", description = "Generate portable agent code")
        public Uni<Response> generateCode(
                        @PathParam("id") String workflowId,
                        @HeaderParam("X-Tenant-ID") String tenantId,
                        CodeGenRequest request) {

                LOG.infof("REST: Generate code for workflow %s (target=%s)",
                                workflowId, request.getTarget());

                return executionService.generateCode(workflowId, request, tenantId)
                                .map(artifact -> Response.accepted()
                                                .entity(artifact)
                                                .header("X-CodeGen-Job-ID", artifact.getJobId())
                                                .build());
        }

        @GET
        @Operation(summary = "List workflows", description = "List all workflows in workspace")
        @APIResponse(responseCode = "200", description = "Success")
        public Uni<Response> listWorkflows(@PathParam("workspaceId") UUID workspaceId) {
                LOG.infof("Listing workflows for workspace: %s and tenant: %s", workspaceId,
                                tenantContext.getTenantId());
                return workflowService.listWorkflows(workspaceId)
                                .map(workflows -> Response.ok(WorkflowListResponse.fromWorkflows(workflows)).build());
        }

        @POST
        @Operation(summary = "Create workflow", description = "Create a new workflow")
        @APIResponse(responseCode = "201", description = "Created")
        @ResponseStatus(201)
        public Uni<Response> createWorkflow(
                        @PathParam("workspaceId") UUID workspaceId,
                        @Valid CreateWorkflowDTO request) {

                LOG.infof("Creating workflow '%s' in workspace: %s for tenant: %s", request.name, workspaceId,
                                tenantContext.getTenantId());
                WorkflowService.CreateWorkflowRequest serviceRequest = new WorkflowService.CreateWorkflowRequest();
                serviceRequest.name = request.name;
                serviceRequest.description = request.description;
                serviceRequest.version = request.version;
                serviceRequest.logic = request.logic;
                serviceRequest.ui = request.ui;
                serviceRequest.runtime = request.runtime;
                serviceRequest.metadata = request.metadata;

                return workflowService.createWorkflow(workspaceId, serviceRequest)
                                .map(workflow -> Response.created(
                                                UriBuilder.fromResource(WorkflowResource.class)
                                                                .path(workflow.id.toString())
                                                                .build(workspaceId))
                                                .entity(new WorkflowResponse(workflow))
                                                .build());
        }

        @GET
        @Path("/{workflowId}")
        @Operation(summary = "Get workflow", description = "Get workflow by ID")
        @APIResponse(responseCode = "200", description = "Success")
        @APIResponse(responseCode = "404", description = "Workflow not found")
        public Uni<Response> getWorkflow(
                        @PathParam("workspaceId") UUID workspaceId,
                        @PathParam("workflowId") UUID workflowId) {

                LOG.debugf("Getting workflow %s for tenant: %s", workflowId, tenantContext.getTenantId());
                return workflowService.getWorkflow(workflowId)
                                .map(workflow -> Response.ok(new WorkflowResponse(workflow)).build());
        }

        @PUT
        @Path("/{workflowId}/logic")
        @Operation(summary = "Update workflow logic", description = "Update workflow logic with optimistic locking")
        @APIResponse(responseCode = "200", description = "Success")
        @APIResponse(responseCode = "409", description = "Version conflict")
        public Uni<Response> updateLogic(
                        @PathParam("workspaceId") UUID workspaceId,
                        @PathParam("workflowId") UUID workflowId,
                        @Valid UpdateLogicDTO request) {

                return workflowService.updateLogic(workflowId, request.logic, request.expectedVersion)
                                .map(workflow -> Response.ok(new WorkflowResponse(workflow)).build());
        }

        @PUT
        @Path("/{workflowId}/ui")
        @Operation(summary = "Update workflow UI", description = "Update workflow UI definition")
        @APIResponse(responseCode = "200", description = "Success")
        public Uni<Response> updateUI(
                        @PathParam("workspaceId") UUID workspaceId,
                        @PathParam("workflowId") UUID workflowId,
                        @Valid UIDefinition ui) {

                return workflowService.updateUI(workflowId, ui)
                                .map(workflow -> Response.ok(new WorkflowResponse(workflow)).build());
        }

        @POST
        @Path("/{workflowId}/validate")
        @Operation(summary = "Validate workflow", description = "Validate workflow structure and logic")
        @APIResponse(responseCode = "200", description = "Validation complete")
        public Uni<Response> validateWorkflow(
                        @PathParam("workspaceId") UUID workspaceId,
                        @PathParam("workflowId") UUID workflowId) {

                return workflowService.validateWorkflow(workflowId)
                                .map(result -> Response.ok(result).build());
        }

        @DELETE
        @Path("/{workflowId}")
        @Operation(summary = "Delete workflow", description = "Delete workflow (soft delete)")
        @APIResponse(responseCode = "204", description = "Deleted")
        public Uni<Response> deleteWorkflow(
                        @PathParam("workspaceId") UUID workspaceId,
                        @PathParam("workflowId") UUID workflowId) {

                LOG.infof("Deleting workflow %s for tenant: %s", workflowId, tenantContext.getTenantId());
                return workflowService.deleteWorkflow(workflowId)
                                .replaceWith(Response.noContent().build());
        }

        // Lock management endpoints
        @POST
        @Path("/{workflowId}/lock")
        @Operation(summary = "Acquire lock", description = "Acquire exclusive lock on workflow")
        @APIResponse(responseCode = "200", description = "Lock acquired")
        @APIResponse(responseCode = "409", description = "Already locked")
        public Uni<Response> acquireLock(
                        @PathParam("workspaceId") UUID workspaceId,
                        @PathParam("workflowId") UUID workflowId,
                        @QueryParam("sessionId") @NotNull String sessionId) {

                LOG.infof("Acquiring lock for workflow %s, session %s, tenant %s", workflowId, sessionId,
                                tenantContext.getTenantId());
                return lockService.acquireLock(workflowId, sessionId)
                                .map(lock -> Response.ok(new LockResponse(lock)).build());
        }

        @PUT
        @Path("/{workflowId}/lock/{lockId}/heartbeat")
        @Operation(summary = "Renew lock", description = "Renew lock heartbeat")
        @APIResponse(responseCode = "200", description = "Lock renewed")
        public Uni<Response> renewLock(
                        @PathParam("workspaceId") UUID workspaceId,
                        @PathParam("workflowId") UUID workflowId,
                        @PathParam("lockId") UUID lockId) {

                return lockService.renewLock(lockId)
                                .map(lock -> Response.ok(new LockResponse(lock)).build());
        }

        @DELETE
        @Path("/{workflowId}/lock/{lockId}")
        @Operation(summary = "Release lock", description = "Release workflow lock")
        @APIResponse(responseCode = "204", description = "Lock released")
        public Uni<Response> releaseLock(
                        @PathParam("workspaceId") UUID workspaceId,
                        @PathParam("workflowId") UUID workflowId,
                        @PathParam("lockId") UUID lockId) {

                return lockService.releaseLock(lockId)
                                .replaceWith(Response.noContent().build());
        }

        @GET
        @Path("/{workflowId}/lock")
        @Operation(summary = "Check lock", description = "Check if workflow is locked")
        @APIResponse(responseCode = "200", description = "Lock status")
        public Uni<Response> checkLock(
                        @PathParam("workspaceId") UUID workspaceId,
                        @PathParam("workflowId") UUID workflowId) {

                return lockService.checkLock(workflowId)
                                .map(lock -> Response.ok(lock.map(LockResponse::new).orElse(null)).build());
        }

        // Draft management endpoints
        @POST
        @Path("/{workflowId}/drafts")
        @Operation(summary = "Save draft", description = "Save workflow draft")
        @APIResponse(responseCode = "201", description = "Draft saved")
        public Uni<Response> saveDraft(
                        @PathParam("workspaceId") UUID workspaceId,
                        @PathParam("workflowId") UUID workflowId,
                        @Valid WorkflowDraft.WorkflowSnapshot snapshot,
                        @QueryParam("autoSave") @DefaultValue("true") boolean autoSave) {

                return draftService.saveDraft(workflowId, snapshot, autoSave)
                                .map(draft -> Response.status(Response.Status.CREATED)
                                                .entity(new DraftResponse(draft))
                                                .build());
        }

        @GET
        @Path("/{workflowId}/drafts/latest")
        @Operation(summary = "Get latest draft", description = "Get latest draft for user")
        @APIResponse(responseCode = "200", description = "Success")
        @APIResponse(responseCode = "404", description = "No draft found")
        public Uni<Response> getLatestDraft(
                        @PathParam("workspaceId") UUID workspaceId,
                        @PathParam("workflowId") UUID workflowId) {

                return draftService.getLatestDraft(workflowId)
                                .map(draft -> draft.map(d -> Response.ok(new DraftResponse(d)).build())
                                                .orElse(Response.status(Response.Status.NOT_FOUND).build()));
        }

        // DTOs
        public static class CreateWorkflowDTO {
                @NotBlank(message = "Workflow name is required")
                public String name;
                public String description;
                public String version;
                public LogicDefinition logic;
                public UIDefinition ui;
                public RuntimeConfig runtime;
                public java.util.Map<String, Object> metadata;
        }

        public static class UpdateLogicDTO {
                @NotNull(message = "Logic definition is required")
                public LogicDefinition logic;
                @NotNull(message = "Expected version is required for optimistic locking")
                public Long expectedVersion;
        }

        public record WorkflowResponse(
                        UUID id,
                        String name,
                        String description,
                        String version,
                        Workflow.WorkflowStatus status,
                        LogicDefinition logic,
                        UIDefinition ui,
                        RuntimeConfig runtime,
                        ValidationResult validationResult,
                        java.time.Instant createdAt,
                        java.time.Instant updatedAt,
                        java.time.Instant publishedAt,
                        Long entityVersion) {
                public WorkflowResponse(Workflow workflow) {
                        this(
                                        workflow.id,
                                        workflow.name,
                                        workflow.description,
                                        workflow.version,
                                        workflow.status,
                                        workflow.logic,
                                        workflow.ui,
                                        workflow.runtime,
                                        workflow.validationResult,
                                        workflow.createdAt,
                                        workflow.updatedAt,
                                        workflow.publishedAt,
                                        workflow.entityVersion);
                }
        }

        public record WorkflowListResponse(List<WorkflowSummary> workflows) {
                public static WorkflowListResponse fromWorkflows(List<Workflow> workflows) {
                        return new WorkflowListResponse(workflows.stream().map(WorkflowSummary::new).toList());
                }
        }

        public record WorkflowSummary(
                        UUID id,
                        String name,
                        String description,
                        String version,
                        Workflow.WorkflowStatus status,
                        java.time.Instant createdAt,
                        java.time.Instant updatedAt) {
                public WorkflowSummary(Workflow workflow) {
                        this(
                                        workflow.id,
                                        workflow.name,
                                        workflow.description,
                                        workflow.version,
                                        workflow.status,
                                        workflow.createdAt,
                                        workflow.updatedAt);
                }
        }

        public record LockResponse(
                        UUID id,
                        UUID workflowId,
                        String userId,
                        String sessionId,
                        java.time.Instant acquiredAt,
                        java.time.Instant expiresAt,
                        WorkflowLock.LockType type) {
                public LockResponse(WorkflowLock lock) {
                        this(
                                        lock.id,
                                        lock.workflowId,
                                        lock.userId,
                                        lock.sessionId,
                                        lock.acquiredAt,
                                        lock.expiresAt,
                                        lock.type);
                }
        }

        public record DraftResponse(
                        UUID id,
                        UUID workflowId,
                        String userId,
                        WorkflowDraft.WorkflowSnapshot content,
                        java.time.Instant savedAt,
                        boolean autoSaved) {
                public DraftResponse(WorkflowDraft draft) {
                        this(
                                        draft.id,
                                        draft.workflowId,
                                        draft.userId,
                                        draft.content,
                                        draft.savedAt,
                                        draft.autoSaved);
                }
        }
}