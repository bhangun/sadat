package tech.kayys.wayang.project.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.project.domain.AIAgent;
import tech.kayys.wayang.project.domain.IntegrationPattern;
import tech.kayys.wayang.project.domain.WayangProject;
import tech.kayys.wayang.project.domain.WorkflowTemplate;
import tech.kayys.wayang.project.dto.AgentExecutionResult;
import tech.kayys.wayang.project.dto.AgentStatus;
import tech.kayys.wayang.project.dto.AgentTask;
import tech.kayys.wayang.project.dto.CreateAgentRequest;
import tech.kayys.wayang.project.dto.CreatePatternRequest;
import tech.kayys.wayang.project.dto.CreateProjectRequest;
import tech.kayys.wayang.project.dto.CreateTemplateRequest;
import tech.kayys.wayang.project.dto.IntegrationExecutionResult;
import tech.kayys.wayang.project.dto.ProjectType;
import tech.kayys.silat.engine.WorkflowRunManager;
import tech.kayys.silat.workflow.WorkflowDefinitionRegistry;
import tech.kayys.silat.model.WorkflowRun;

/**
 * Main Control Plane Service
 */
@ApplicationScoped
public class ControlPlaneService {

        private static final Logger LOG = LoggerFactory.getLogger(ControlPlaneService.class);

        @Inject
        WorkflowRunManager workflowRunManager;

        @Inject
        WorkflowDefinitionRegistry definitionRegistry;

        @Inject
        CanvasToWorkflowConverter canvasConverter;

        @Inject
        AgentOrchestrator agentOrchestrator;

        @Inject
        IntegrationPatternExecutor patternExecutor;

        // ==================== PROJECT MANAGEMENT ====================

        /**
         * Create new control plane project
         */
        public Uni<WayangProject> createProject(CreateProjectRequest request) {
                LOG.info("Creating project: {} for tenant: {}",
                                request.projectName(), request.tenantId());

                return Panache.withTransaction(() -> {
                        WayangProject project = new WayangProject();
                        project.tenantId = request.tenantId();
                        project.projectName = request.projectName();
                        project.description = request.description();
                        project.projectType = request.projectType();
                        project.createdAt = Instant.now();
                        project.updatedAt = Instant.now();
                        project.createdBy = request.createdBy();
                        project.metadata = request.metadata() != null ? request.metadata() : new HashMap<>();

                        return project.persist()
                                        .map(p -> (WayangProject) p);
                });
        }

        /**
         * Get project by ID
         */
        public Uni<WayangProject> getProject(UUID projectId, String tenantId) {
                return WayangProject.find(
                                "projectId = ?1 and tenantId = ?2 and isActive = true",
                                projectId, tenantId).firstResult();
        }

        /**
         * List projects for tenant
         */
        public Uni<List<WayangProject>> listProjects(
                        String tenantId,
                        ProjectType type) {

                String query = type != null ? "tenantId = ?1 and projectType = ?2 and isActive = true"
                                : "tenantId = ?1 and isActive = true";

                return type != null ? WayangProject.list(query, tenantId, type) : WayangProject.list(query, tenantId);
        }

        // ==================== WORKFLOW TEMPLATE MANAGEMENT ====================

        /**
         * Create workflow template from canvas
         */
        public Uni<WorkflowTemplate> createWorkflowTemplate(
                        UUID projectId,
                        CreateTemplateRequest request) {

                LOG.info("Creating workflow template: {} in project: {}",
                                request.templateName(), projectId);

                return Panache.withTransaction(() -> WayangProject.<WayangProject>findById(projectId)
                                .flatMap(project -> {
                                        if (project == null) {
                                                return Uni.createFrom().failure(
                                                                new IllegalArgumentException("Project not found"));
                                        }

                                        WorkflowTemplate template = new WorkflowTemplate();
                                        template.project = project;
                                        template.templateName = request.templateName();
                                        template.description = request.description();
                                        template.version = request.version() != null ? request.version() : "1.0.0";
                                        template.templateType = request.templateType();
                                        template.canvasDefinition = request.canvasDefinition();
                                        template.tags = request.tags();
                                        template.createdAt = Instant.now();
                                        template.updatedAt = Instant.now();

                                        return template.persist()
                                                        .map(t -> (WorkflowTemplate) t);
                                }));
        }

        /**
         * Publish workflow template (convert to Silat workflow)
         */
        public Uni<String> publishWorkflowTemplate(UUID templateId) {
                LOG.info("Publishing workflow template: {}", templateId);

                return WorkflowTemplate.<WorkflowTemplate>findById(templateId)
                                .flatMap(template -> {
                                        if (template == null) {
                                                return Uni.createFrom().failure(
                                                                new IllegalArgumentException("Template not found"));
                                        }

                                        // Convert canvas to workflow definition
                                        return canvasConverter.convert(
                                                        template.canvasDefinition,
                                                        template.templateName,
                                                        template.version)
                                                        .flatMap(workflowDef ->
                                        // Register with Silat
                                        definitionRegistry.register(
                                                        workflowDef,
                                                        tech.kayys.silat.model.TenantId.of(template.project.tenantId)))
                                                        .flatMap(workflowDef ->
                                        // Update template with workflow definition ID
                                        Panache.withTransaction(() -> {
                                                template.workflowDefinitionId = workflowDef.id().value();
                                                template.isPublished = true;
                                                template.updatedAt = Instant.now();
                                                return template.persist()
                                                                .map(t -> workflowDef.id().value());
                                        }));
                                });
        }

        /**
         * Execute workflow template
         */
        public Uni<WorkflowRun> executeWorkflowTemplate(
                        UUID templateId,
                        Map<String, Object> inputs) {

                return WorkflowTemplate.<WorkflowTemplate>findById(templateId)
                                .flatMap(template -> {
                                        if (template == null || !template.isPublished) {
                                                return Uni.createFrom().failure(
                                                                new IllegalArgumentException("Template not published"));
                                        }

                                        // Create run request
                                        tech.kayys.silat.model.CreateRunRequest silatRequest = tech.kayys.silat.model.CreateRunRequest
                                                        .builder()
                                                        .workflowId(template.workflowDefinitionId)
                                                        .workflowVersion(template.version)
                                                        .inputs(inputs)
                                                        .build();

                                        // Create and start workflow
                                        return workflowRunManager.createRun(
                                                        silatRequest,
                                                        tech.kayys.silat.model.TenantId.of(template.project.tenantId))
                                                        .flatMap(run -> workflowRunManager.startRun(
                                                                        run.getId(),
                                                                        tech.kayys.silat.model.TenantId.of(
                                                                                        template.project.tenantId)));
                                });
        }

        // ==================== AI AGENT MANAGEMENT ====================

        /**
         * Create AI agent
         */
        public Uni<AIAgent> createAgent(UUID projectId, CreateAgentRequest request) {
                LOG.info("Creating AI agent: {} in project: {}",
                                request.agentName(), projectId);

                return Panache.withTransaction(() -> WayangProject.<WayangProject>findById(projectId)
                                .flatMap(project -> {
                                        AIAgent agent = new AIAgent();
                                        agent.project = project;
                                        agent.agentName = request.agentName();
                                        agent.description = request.description();
                                        agent.agentType = request.agentType();
                                        agent.llmConfig = request.llmConfig();
                                        agent.capabilities = request.capabilities();
                                        agent.tools = request.tools();
                                        agent.memoryConfig = request.memoryConfig();
                                        agent.guardrails = request.guardrails();
                                        agent.status = AgentStatus.INACTIVE;
                                        agent.createdAt = Instant.now();

                                        return agent.persist()
                                                        .map(a -> (AIAgent) a);
                                }));
        }

        /**
         * Activate AI agent
         */
        public Uni<AIAgent> activateAgent(UUID agentId) {
                return agentOrchestrator.activateAgent(agentId);
        }

        /**
         * Execute agent task
         */
        public Uni<AgentExecutionResult> executeAgentTask(
                        UUID agentId,
                        AgentTask task) {

                return agentOrchestrator.executeTask(agentId, task);
        }

        // ==================== INTEGRATION PATTERN MANAGEMENT ====================

        /**
         * Create integration pattern
         */
        public Uni<IntegrationPattern> createIntegrationPattern(
                        UUID projectId,
                        CreatePatternRequest request) {

                LOG.info("Creating integration pattern: {} in project: {}",
                                request.patternName(), projectId);

                return Panache.withTransaction(() -> WayangProject.<WayangProject>findById(projectId)
                                .flatMap(project -> {
                                        IntegrationPattern pattern = new IntegrationPattern();
                                        pattern.project = project;
                                        pattern.patternName = request.patternName();
                                        pattern.description = request.description();
                                        pattern.patternType = request.patternType();
                                        pattern.sourceConfig = request.sourceConfig();
                                        pattern.targetConfig = request.targetConfig();
                                        pattern.transformation = request.transformation();
                                        pattern.errorHandling = request.errorHandling();
                                        pattern.createdAt = Instant.now();

                                        return pattern.persist()
                                                        .map(p -> (IntegrationPattern) p);
                                }));
        }

        /**
         * Execute integration pattern
         */
        public Uni<IntegrationExecutionResult> executeIntegrationPattern(
                        UUID patternId,
                        Map<String, Object> payload) {

                return patternExecutor.execute(patternId, payload);
        }
}