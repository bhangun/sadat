package tech.kayys.wayang.resources;

import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.project.domain.WorkflowTemplate;
import tech.kayys.wayang.project.dto.CreateTemplateRequest;
import tech.kayys.wayang.project.dto.ExecutionResponse;
import tech.kayys.wayang.project.dto.PublishResponse;
import tech.kayys.wayang.project.service.ControlPlaneService;
import tech.kayys.wayang.security.service.IketSecurityService;

@Path("/api/v1/control-plane/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Control Plane - Templates", description = "Workflow template management")
public class TemplateResource {

        @Inject
        ControlPlaneService controlPlaneService;

        @Inject
        IketSecurityService iketSecurity;

        @POST
        @Operation(summary = "Create workflow template")
        public Uni<RestResponse<WorkflowTemplate>> createTemplate(
                        @QueryParam("projectId") UUID projectId,
                        @Valid CreateTemplateRequest request) {

                return controlPlaneService.createWorkflowTemplate(projectId, request)
                                .map(template -> RestResponse.status(
                                                RestResponse.Status.CREATED, template));
        }

        @POST
        @Path("/{templateId}/publish")
        @Operation(summary = "Publish workflow template")
        public Uni<RestResponse<PublishResponse>> publishTemplate(
                        @PathParam("templateId") UUID templateId) {

                return controlPlaneService.publishWorkflowTemplate(templateId)
                                .map(workflowDefId -> RestResponse.ok(
                                                new PublishResponse(
                                                                true,
                                                                workflowDefId,
                                                                "Template published successfully")));
        }

        @POST
        @Path("/{templateId}/execute")
        @Operation(summary = "Execute workflow template")
        public Uni<RestResponse<ExecutionResponse>> executeTemplate(
                        @PathParam("templateId") UUID templateId,
                        @Valid Map<String, Object> inputs) {

                return controlPlaneService.executeWorkflowTemplate(templateId, inputs)
                                .map(run -> RestResponse.ok(
                                                new ExecutionResponse(
                                                                run.getId().value(),
                                                                run.getStatus().name(),
                                                                run.getCreatedAt())));
        }
}
