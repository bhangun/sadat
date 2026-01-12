package tech.kayys.wayang.resources;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.dto.AgentCreatedEvent;
import tech.kayys.wayang.dto.AgentTaskRequest;
import tech.kayys.wayang.dto.GuardedAgentResponse;
import tech.kayys.wayang.guardrails.dto.BiasPolicy;
import tech.kayys.wayang.guardrails.dto.ContentModerationPolicy;
import tech.kayys.wayang.guardrails.dto.GuardrailAction;
import tech.kayys.wayang.guardrails.dto.GuardrailPolicy;
import tech.kayys.wayang.guardrails.dto.PIIPolicy;
import tech.kayys.wayang.guardrails.dto.RateLimitConfig;
import tech.kayys.wayang.guardrails.dto.ToxicityPolicy;
import tech.kayys.wayang.guardrails.service.GuardrailEngine;
import tech.kayys.wayang.project.domain.AIAgent;
import tech.kayys.wayang.project.dto.AgentTask;
import tech.kayys.wayang.project.dto.CreateAgentRequest;
import tech.kayys.wayang.project.dto.Guardrail;
import tech.kayys.wayang.project.service.ControlPlaneService;
import tech.kayys.wayang.security.service.AuthenticatedUser;
import tech.kayys.wayang.security.service.IketSecurityService;
import tech.kayys.wayang.websocket.dto.AgentEvent;
import tech.kayys.wayang.websocket.service.WebSocketEventBroadcaster;

@Path("/api/v1/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AI Agents", description = "AI agent management with guardrails")
@SecurityRequirement(name = "bearer-jwt")
public class AgentResource {

        @Inject
        ControlPlaneService controlPlaneService;

        @Inject
        IketSecurityService iketSecurity;

        @Inject
        GuardrailEngine guardrailEngine;

        @Inject
        WebSocketEventBroadcaster wsEventBroadcaster;

        @POST
        @Operation(summary = "Create AI agent with guardrails")
        @RolesAllowed({ "admin", "ai_engineer" })
        public Uni<RestResponse<AIAgent>> createAgent(
                        @QueryParam("projectId") UUID projectId,
                        @Valid CreateAgentRequest request) {

                AuthenticatedUser user = iketSecurity.getCurrentUser();

                return controlPlaneService.createAgent(projectId, request)
                                .flatMap(agent -> wsEventBroadcaster.broadcastToTenant(
                                                user.tenantId(),
                                                new AgentCreatedEvent(
                                                                agent.agentId,
                                                                agent.agentName,
                                                                agent.agentType,
                                                                Instant.now()))
                                                .map(v -> RestResponse.status(
                                                                RestResponse.Status.CREATED, agent)));
        }

        @POST
        @Path("/{agentId}/execute")
        @Operation(summary = "Execute agent task with guardrails")
        @RolesAllowed({ "admin", "ai_engineer", "user" })
        public Uni<RestResponse<GuardedAgentResponse>> executeTask(
                        @PathParam("agentId") UUID agentId,
                        @Valid AgentTaskRequest taskRequest) {

                AuthenticatedUser user = iketSecurity.getCurrentUser();

                // Get agent to retrieve guardrail policy
                return AIAgent.<AIAgent>findById(agentId)
                                .<RestResponse<GuardedAgentResponse>>flatMap(agent -> {
                                        if (agent == null) {
                                                return Uni.createFrom().item(
                                                                RestResponse.<GuardedAgentResponse>notFound());
                                        }

                                        // Check input against guardrails
                                        GuardrailPolicy policy = buildGuardrailPolicy(agent.guardrails);

                                        return guardrailEngine.checkInput(
                                                        taskRequest.instruction(),
                                                        policy,
                                                        user.userId(),
                                                        user.tenantId())
                                                        .<RestResponse<GuardedAgentResponse>>flatMap(inputCheck -> {
                                                                if (!inputCheck.passed()) {
                                                                        return Uni.createFrom().item(
                                                                                        RestResponse.ok(new GuardedAgentResponse(
                                                                                                        null,
                                                                                                        false,
                                                                                                        "Input blocked by guardrails",
                                                                                                        inputCheck.violations(),
                                                                                                        null)));
                                                                }

                                                                // Execute agent task
                                                                AgentTask task = new AgentTask(
                                                                                UUID.randomUUID().toString(),
                                                                                taskRequest.instruction(),
                                                                                taskRequest.context(),
                                                                                List.of());

                                                                return controlPlaneService
                                                                                .executeAgentTask(agentId, task)
                                                                                .<RestResponse<GuardedAgentResponse>>flatMap(
                                                                                                result -> {
                                                                                                        // Check output
                                                                                                        // against
                                                                                                        // guardrails
                                                                                                        return guardrailEngine
                                                                                                                        .checkOutput(
                                                                                                                                        result.response(),
                                                                                                                                        policy,
                                                                                                                                        user.userId(),
                                                                                                                                        user.tenantId())
                                                                                                                        .<GuardedAgentResponse>flatMap(
                                                                                                                                        outputCheck -> {
                                                                                                                                                String finalResponse = result
                                                                                                                                                                .response();

                                                                                                                                                // Sanitize
                                                                                                                                                // if
                                                                                                                                                // needed
                                                                                                                                                if (outputCheck.action() == GuardrailAction.REDACT) {
                                                                                                                                                        return guardrailEngine
                                                                                                                                                                        .sanitizeOutput(
                                                                                                                                                                                        result.response(),
                                                                                                                                                                                        policy)
                                                                                                                                                                        .map(sanitized -> new GuardedAgentResponse(
                                                                                                                                                                                        result.taskId(),
                                                                                                                                                                                        result.success(),
                                                                                                                                                                                        sanitized.content(),
                                                                                                                                                                                        outputCheck.violations(),
                                                                                                                                                                                        sanitized.modifications()));
                                                                                                                                                }

                                                                                                                                                return Uni.createFrom()
                                                                                                                                                                .item(
                                                                                                                                                                                new GuardedAgentResponse(
                                                                                                                                                                                                result.taskId(),
                                                                                                                                                                                                result.success(),
                                                                                                                                                                                                finalResponse,
                                                                                                                                                                                                outputCheck.violations(),
                                                                                                                                                                                                List.of()));
                                                                                                                                        })
                                                                                                                        .<RestResponse<GuardedAgentResponse>>flatMap(
                                                                                                                                        guardedResponse ->
                                                                                                        // Broadcast
                                                                                                        // agent event
                                                                                                        wsEventBroadcaster
                                                                                                                        .broadcastAgentEvent(
                                                                                                                                        user.tenantId(),
                                                                                                                                        agentId,
                                                                                                                                        new AgentEvent(
                                                                                                                                                        "task_completed",
                                                                                                                                                        agentId,
                                                                                                                                                        "completed",
                                                                                                                                                        guardedResponse.response(),
                                                                                                                                                        Map.of("task_id",
                                                                                                                                                                        result.taskId()),
                                                                                                                                                        Instant.now()))
                                                                                                                        .map(v -> RestResponse
                                                                                                                                        .ok(guardedResponse)));
                                                                                                });
                                                        });
                                });
        }

        private GuardrailPolicy buildGuardrailPolicy(List<Guardrail> guardrails) {
                if (guardrails == null || guardrails.isEmpty()) {
                        return defaultGuardrailPolicy();
                }

                boolean contentModEnabled = false;
                boolean piiEnabled = false;
                boolean toxicityEnabled = false;
                boolean biasEnabled = false;
                boolean promptInjectionEnabled = false;

                for (Guardrail g : guardrails) {
                        switch (g.type) {
                                case CONTENT_FILTER -> contentModEnabled = true;
                                case PII_DETECTION -> piiEnabled = true;
                                case TOXICITY_CHECK -> toxicityEnabled = true;
                                case BIAS_DETECTION -> biasEnabled = true;
                                default -> {
                                }
                        }
                }

                return new GuardrailPolicy(
                                contentModEnabled,
                                new ContentModerationPolicy(Map.of(), GuardrailAction.WARN),
                                piiEnabled,
                                new PIIPolicy(Set.of("ssn", "email", "phone"), GuardrailAction.REDACT),
                                toxicityEnabled,
                                new ToxicityPolicy(0.7, GuardrailAction.WARN),
                                biasEnabled,
                                new BiasPolicy(0.8, GuardrailAction.WARN),
                                true, // Always check prompt injection
                                true, // Always rate limit
                                new RateLimitConfig(60),
                                true, // Always check cost
                                false,
                                List.of());
        }

        private GuardrailPolicy defaultGuardrailPolicy() {
                return new GuardrailPolicy(
                                true,
                                new ContentModerationPolicy(Map.of(), GuardrailAction.WARN),
                                true,
                                new PIIPolicy(Set.of("ssn", "email", "phone"), GuardrailAction.REDACT),
                                true,
                                new ToxicityPolicy(0.7, GuardrailAction.WARN),
                                true,
                                new BiasPolicy(0.8, GuardrailAction.WARN),
                                true,
                                true,
                                new RateLimitConfig(60),
                                true,
                                false,
                                List.of());
        }
}
