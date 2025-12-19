package tech.kayys.wayang.service;

import tech.kayys.wayang.dto.bpmn.ConnectorSpec;
import tech.kayys.wayang.dto.bpmn.DataDestination;
import tech.kayys.wayang.dto.bpmn.DataSource;
import tech.kayys.wayang.dto.bpmn.DatabaseIntegrationRequest;
import tech.kayys.wayang.dto.bpmn.ETLPipelineRequest;
import tech.kayys.wayang.dto.bpmn.IntegrationExecutionRequest;
import tech.kayys.wayang.dto.bpmn.IntegrationExecutionResponse;
import tech.kayys.wayang.dto.bpmn.IntegrationType;
import tech.kayys.wayang.dto.bpmn.IntegrationWorkflowRequest;
import tech.kayys.wayang.dto.bpmn.IntegrationWorkflowResponse;
import tech.kayys.wayang.dto.bpmn.OpenAPIImportRequest;
import tech.kayys.wayang.dto.bpmn.OpenAPISpec;
import tech.kayys.wayang.dto.bpmn.OutputSpec;
import tech.kayys.wayang.dto.bpmn.TransformationRule;
import tech.kayys.wayang.dto.bpmn.TransformationSpec;
import tech.kayys.wayang.dto.bpmn.TriggerSpec;
import tech.kayys.wayang.dto.bpmn.ValidationRule;
import tech.kayys.wayang.dto.bpmn.WebhookAction;
import tech.kayys.wayang.dto.bpmn.WebhookRequest;
import tech.kayys.wayang.model.*;
import tech.kayys.wayang.model.RuntimeConfig.Trigger.TriggerType;
import tech.kayys.wayang.schema.node.EdgeDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.workflow.Trigger;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.sdk.WorkflowDefinitionClient;
import tech.kayys.wayang.sdk.WorkflowRunClient;
import tech.kayys.wayang.sdk.dto.TriggerWorkflowRequest;
import tech.kayys.wayang.sdk.dto.WorkflowDefinitionRequest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Integration Builder Service
 * 
 * Features:
 * - API integration designer
 * - Database connector workflows
 * - Message queue integrations
 * - File processing workflows
 * - ETL pipelines
 * - Webhook handlers
 * - Scheduled jobs
 */
@Path("/api/v1/integration-builder")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class IntegrationBuilderService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationBuilderService.class);

    @Inject
    @RestClient
    WorkflowRunClient workflowClient;

    @Inject
    @RestClient
    WorkflowDefinitionClient workflowDefClient;

    @Inject
    ConnectorRegistry connectorRegistry;

    @Inject
    IntegrationPatternLibrary patternLibrary;

    /**
     * Create integration workflow
     */
    @POST
    @Path("/integrations")
    public Uni<IntegrationWorkflowResponse> createIntegration(
            IntegrationWorkflowRequest request) {
        log.info("Creating integration: {}, type: {}",
                request.name(), request.integrationType());

        return buildIntegrationWorkflow(request)
                .onItem().transformToUni(workflow -> workflowDefClient.createWorkflow(toWorkflowRequest(workflow)))
                .map(created -> new IntegrationWorkflowResponse(
                        created.id(),
                        created.name(),
                        request.integrationType(),
                        created.status()));
    }

    /**
     * Execute integration workflow
     */
    @POST
    @Path("/integrations/{integrationId}/execute")
    public Uni<IntegrationExecutionResponse> executeIntegration(
            @PathParam("integrationId") String integrationId,
            IntegrationExecutionRequest request) {
        log.info("Executing integration: {}", integrationId);

        TriggerWorkflowRequest triggerRequest = new TriggerWorkflowRequest(
                integrationId,
                request.tenantId(),
                request.triggeredBy(),
                request.inputs(),
                Map.of("integrationType", request.integrationType()));

        return workflowClient.triggerWorkflow(triggerRequest)
                .map(run -> new IntegrationExecutionResponse(
                        run.runId(),
                        run.status().toString(),
                        run.createdAt()));
    }

    /**
     * Create API integration from OpenAPI spec
     */
    @POST
    @Path("/integrations/from-openapi")
    public Uni<IntegrationWorkflowResponse> createFromOpenAPI(
            OpenAPIImportRequest request) {
        log.info("Creating integration from OpenAPI spec: {}", request.apiName());

        return parseOpenAPISpec(request.openApiSpec())
                .onItem().transformToUni(spec -> {
                    // Generate integration nodes from endpoints
                    IntegrationWorkflowRequest integrationRequest = generateIntegrationFromSpec(spec, request);

                    return createIntegration(integrationRequest);
                });
    }

    /**
     * Create database integration
     */
    @POST
    @Path("/integrations/database")
    public Uni<IntegrationWorkflowResponse> createDatabaseIntegration(
            DatabaseIntegrationRequest request) {
        log.info("Creating database integration: {}", request.name());

        WorkflowDefinition workflow = buildDatabaseWorkflow(request);

        return workflowDefClient.createWorkflow(toWorkflowRequest(workflow))
                .map(created -> new IntegrationWorkflowResponse(
                        created.id(),
                        created.name(),
                        IntegrationType.DATABASE,
                        created.status()));
    }

    /**
     * Create ETL pipeline
     */
    @POST
    @Path("/integrations/etl")
    public Uni<IntegrationWorkflowResponse> createETLPipeline(
            ETLPipelineRequest request) {
        log.info("Creating ETL pipeline: {}", request.name());

        WorkflowDefinition workflow = buildETLWorkflow(request);

        return workflowDefClient.createWorkflow(toWorkflowRequest(workflow))
                .map(created -> new IntegrationWorkflowResponse(
                        created.id(),
                        created.name(),
                        IntegrationType.ETL,
                        created.status()));
    }

    /**
     * Create webhook handler
     */
    @POST
    @Path("/integrations/webhook")
    public Uni<IntegrationWorkflowResponse> createWebhook(
            WebhookRequest request) {
        log.info("Creating webhook: {}", request.name());

        WorkflowDefinition workflow = buildWebhookWorkflow(request);

        return workflowDefClient.createWorkflow(toWorkflowRequest(workflow))
                .map(created -> new IntegrationWorkflowResponse(
                        created.id(),
                        created.name(),
                        IntegrationType.WEBHOOK,
                        created.status()));
    }

    /**
     * List available connectors
     */
    @GET
    @Path("/connectors")
    public Uni<List<ConnectorInfo>> listConnectors(
            @QueryParam("category") String category) {
        return connectorRegistry.getConnectors(category);
    }

    /**
     * Get integration patterns (templates)
     */
    @GET
    @Path("/patterns")
    public Uni<List<IntegrationPattern>> getPatterns(
            @QueryParam("type") IntegrationType type) {
        return patternLibrary.getPatterns(type);
    }

    // ========================================================================
    // Workflow Builders
    // ========================================================================

    /**
     * Build integration workflow from request
     */
    private Uni<WorkflowDefinition> buildIntegrationWorkflow(
            IntegrationWorkflowRequest request) {
        return Uni.createFrom().item(() -> {
            WorkflowDefinition workflow = new WorkflowDefinition();
            workflow.setId(UUID.randomUUID().toString());
            workflow.setName(request.name());
            workflow.setDescription(request.description());
            workflow.setTenantId(request.tenantId());

            List<NodeDefinition> nodes = new ArrayList<>();
            List<EdgeDefinition> edges = new ArrayList<>();

            // 1. Add trigger node
            NodeDefinition triggerNode = createTriggerNode(request.trigger());
            nodes.add(triggerNode);

            // 2. Add connector nodes
            String previousNodeId = triggerNode.getId();
            for (ConnectorSpec connector : request.connectors()) {
                NodeDefinition connectorNode = createConnectorNode(connector);
                nodes.add(connectorNode);
                edges.add(createEdge(previousNodeId, connectorNode.getId()));
                previousNodeId = connectorNode.getId();
            }

            // 3. Add transformation nodes
            for (TransformationSpec transform : request.transformations()) {
                NodeDefinition transformNode = createTransformationNode(transform);
                nodes.add(transformNode);
                edges.add(createEdge(previousNodeId, transformNode.getId()));
                previousNodeId = transformNode.getId();
            }

            // 4. Add output node
            NodeDefinition outputNode = createOutputNode(request.output());
            nodes.add(outputNode);
            edges.add(createEdge(previousNodeId, outputNode.getId()));

            workflow.setNodes(nodes);
            workflow.setEdges(edges);

            // Set trigger
            if (request.trigger().type() == TriggerType.SCHEDULE) {
                Trigger trigger = new Trigger();
                trigger.setType("cron");
                trigger.setExpression(request.trigger().schedule());
                workflow.setTriggers(List.of(trigger));
            }

            return workflow;
        });
    }

    /**
     * Build database workflow (CRUD operations)
     */
    private WorkflowDefinition buildDatabaseWorkflow(
            DatabaseIntegrationRequest request) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(UUID.randomUUID().toString());
        workflow.setName(request.name());
        workflow.setTenantId(request.tenantId());

        List<NodeDefinition> nodes = new ArrayList<>();
        List<EdgeDefinition> edges = new ArrayList<>();

        // Start node
        NodeDefinition startNode = createStartNode();
        nodes.add(startNode);

        // Database connector node
        NodeDefinition dbNode = createDatabaseConnectorNode(request);
        nodes.add(dbNode);
        edges.add(createEdge(startNode.getId(), dbNode.getId()));

        // Query execution node
        NodeDefinition queryNode = createQueryExecutionNode(request.query());
        nodes.add(queryNode);
        edges.add(createEdge(dbNode.getId(), queryNode.getId()));

        // Result transformation node (optional)
        if (request.resultTransformation() != null) {
            NodeDefinition transformNode = createResultTransformNode(
                    request.resultTransformation());
            nodes.add(transformNode);
            edges.add(createEdge(queryNode.getId(), transformNode.getId()));
        }

        // End node
        NodeDefinition endNode = createEndNode();
        nodes.add(endNode);
        String lastNodeId = request.resultTransformation() != null
                ? nodes.get(nodes.size() - 2).getId()
                : queryNode.getId();
        edges.add(createEdge(lastNodeId, endNode.getId()));

        workflow.setNodes(nodes);
        workflow.setEdges(edges);

        return workflow;
    }

    /**
     * Build ETL workflow (Extract, Transform, Load)
     */
    private WorkflowDefinition buildETLWorkflow(ETLPipelineRequest request) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(UUID.randomUUID().toString());
        workflow.setName(request.name());
        workflow.setTenantId(request.tenantId());

        List<NodeDefinition> nodes = new ArrayList<>();
        List<EdgeDefinition> edges = new ArrayList<>();

        // Start node
        NodeDefinition startNode = createStartNode();
        nodes.add(startNode);
        String previousNodeId = startNode.getId();

        // EXTRACT phase
        NodeDefinition extractNode = createExtractNode(request.source());
        nodes.add(extractNode);
        edges.add(createEdge(previousNodeId, extractNode.getId()));
        previousNodeId = extractNode.getId();

        // TRANSFORM phase
        for (TransformationRule rule : request.transformations()) {
            NodeDefinition transformNode = createTransformNode(rule);
            nodes.add(transformNode);
            edges.add(createEdge(previousNodeId, transformNode.getId()));
            previousNodeId = transformNode.getId();
        }

        // Data validation node
        NodeDefinition validationNode = createValidationNode(request.validationRules());
        nodes.add(validationNode);
        edges.add(createEdge(previousNodeId, validationNode.getId()));

        // LOAD phase
        NodeDefinition loadNode = createLoadNode(request.destination());
        nodes.add(loadNode);
        edges.add(createEdge(validationNode.getId(), loadNode.getId(), "success"));

        // Error handling path
        NodeDefinition errorNode = createErrorHandlerNode();
        nodes.add(errorNode);
        edges.add(createEdge(validationNode.getId(), errorNode.getId(), "error"));

        // End node
        NodeDefinition endNode = createEndNode();
        nodes.add(endNode);
        edges.add(createEdge(loadNode.getId(), endNode.getId()));
        edges.add(createEdge(errorNode.getId(), endNode.getId()));

        workflow.setNodes(nodes);
        workflow.setEdges(edges);

        return workflow;
    }

    /**
     * Build webhook workflow
     */
    private WorkflowDefinition buildWebhookWorkflow(WebhookRequest request) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(UUID.randomUUID().toString());
        workflow.setName(request.name());
        workflow.setTenantId(request.tenantId());

        List<NodeDefinition> nodes = new ArrayList<>();
        List<EdgeDefinition> edges = new ArrayList<>();

        // Webhook trigger
        NodeDefinition triggerNode = createWebhookTriggerNode(request);
        nodes.add(triggerNode);

        // Payload validation
        NodeDefinition validationNode = createPayloadValidationNode(request.schema());
        nodes.add(validationNode);
        edges.add(createEdge(triggerNode.getId(), validationNode.getId()));

        // Process webhook data
        String previousNodeId = validationNode.getId();
        for (WebhookAction action : request.actions()) {
            NodeDefinition actionNode = createWebhookActionNode(action);
            nodes.add(actionNode);
            edges.add(createEdge(previousNodeId, actionNode.getId()));
            previousNodeId = actionNode.getId();
        }

        // Response node
        NodeDefinition responseNode = createResponseNode(request.responseTemplate());
        nodes.add(responseNode);
        edges.add(createEdge(previousNodeId, responseNode.getId()));

        workflow.setNodes(nodes);
        workflow.setEdges(edges);

        return workflow;
    }

    // ========================================================================
    // Node Creation Helpers
    // ========================================================================

    private NodeDefinition createTriggerNode(TriggerSpec spec) {
        NodeDefinition node = new NodeDefinition();
        node.setId("trigger-" + UUID.randomUUID().toString());
        node.setType("trigger");
        node.setDisplayName("Trigger: " + spec.type());

        Map<String, Object> config = new HashMap<>();
        config.put("triggerType", spec.type());
        config.put("schedule", spec.schedule());
        // Set properties

        return node;
    }

    private NodeDefinition createConnectorNode(ConnectorSpec spec) {
        NodeDefinition node = new NodeDefinition();
        node.setId("connector-" + UUID.randomUUID().toString());
        node.setType("connector");
        node.setDisplayName(spec.name());
        node.setConnectorRef(spec.connectorId());

        // Set connector configuration
        Map<String, Object> config = new HashMap<>(spec.configuration());
        // Convert to properties

        return node;
    }

    private NodeDefinition createTransformationNode(TransformationSpec spec) {
        NodeDefinition node = new NodeDefinition();
        node.setId("transform-" + UUID.randomUUID().toString());
        node.setType("transformation");
        node.setDisplayName(spec.name());

        Map<String, Object> config = new HashMap<>();
        config.put("transformationType", spec.type());
        config.put("expression", spec.expression());
        config.put("mapping", spec.mapping());

        return node;
    }

    private NodeDefinition createOutputNode(OutputSpec spec) {
        NodeDefinition node = new NodeDefinition();
        node.setId("output-" + UUID.randomUUID().toString());
        node.setType("output");
        node.setDisplayName("Output: " + spec.destination());

        return node;
    }

    private NodeDefinition createDatabaseConnectorNode(DatabaseIntegrationRequest request) {
        NodeDefinition node = new NodeDefinition();
        node.setId("db-connector-" + UUID.randomUUID().toString());
        node.setType("connector");
        node.setDisplayName("Database: " + request.databaseType());

        Map<String, Object> config = new HashMap<>();
        config.put("dbType", request.databaseType());
        config.put("connectionString", request.connectionString());
        config.put("credentials", request.credentialsRef());

        return node;
    }

    private NodeDefinition createQueryExecutionNode(String query) {
        NodeDefinition node = new NodeDefinition();
        node.setId("query-" + UUID.randomUUID().toString());
        node.setType("database-query");
        node.setDisplayName("Execute Query");

        Map<String, Object> config = new HashMap<>();
        config.put("query", query);

        return node;
    }

    private NodeDefinition createExtractNode(DataSource source) {
        NodeDefinition node = new NodeDefinition();
        node.setId("extract-" + UUID.randomUUID().toString());
        node.setType("data-extractor");
        node.setDisplayName("Extract from " + source.type());

        return node;
    }

    private NodeDefinition createTransformNode(TransformationRule rule) {
        NodeDefinition node = new NodeDefinition();
        node.setId("transform-" + UUID.randomUUID().toString());
        node.setType("data-transformer");
        node.setDisplayName("Transform: " + rule.name());

        return node;
    }

    private NodeDefinition createValidationNode(List<ValidationRule> rules) {
        NodeDefinition node = new NodeDefinition();
        node.setId("validate-" + UUID.randomUUID().toString());
        node.setType("data-validator");
        node.setDisplayName("Validate Data");

        return node;
    }

    private NodeDefinition createLoadNode(DataDestination destination) {
        NodeDefinition node = new NodeDefinition();
        node.setId("load-" + UUID.randomUUID().toString());
        node.setType("data-loader");
        node.setDisplayName("Load to " + destination.type());

        return node;
    }

    private NodeDefinition createWebhookTriggerNode(WebhookRequest request) {
        NodeDefinition node = new NodeDefinition();
        node.setId("webhook-trigger");
        node.setType("webhook-trigger");
        node.setDisplayName("Webhook: " + request.path());

        return node;
    }

    private NodeDefinition createPayloadValidationNode(String schema) {
        NodeDefinition node = new NodeDefinition();
        node.setId("payload-validation");
        node.setType("json-validator");
        node.setDisplayName("Validate Payload");

        return node;
    }

    private NodeDefinition createWebhookActionNode(WebhookAction action) {
        NodeDefinition node = new NodeDefinition();
        node.setId("action-" + UUID.randomUUID().toString());
        node.setType(action.type());
        node.setDisplayName("Action: " + action.name());

        return node;
    }

    private NodeDefinition createResponseNode(String responseTemplate) {
        NodeDefinition node = new NodeDefinition();
        node.setId("response");
        node.setType("http-response");
        node.setDisplayName("Send Response");

        return node;
    }

    private NodeDefinition createResultTransformNode(String transformation) {
        NodeDefinition node = new NodeDefinition();
        node.setId("result-transform");
        node.setType("transformation");
        node.setDisplayName("Transform Results");

        return node;
    }

    private NodeDefinition createErrorHandlerNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("error-handler");
        node.setType("error-handler");
        node.setDisplayName("Handle Errors");

        return node;
    }

    private NodeDefinition createStartNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("start");
        node.setType("start");
        return node;
    }

    private NodeDefinition createEndNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("end");
        node.setType("end");
        return node;
    }

    private EdgeDefinition createEdge(String from, String to) {
        return createEdge(from, to, "success");
    }

    private EdgeDefinition createEdge(String from, String to, String fromPort) {
        EdgeDefinition edge = new EdgeDefinition();
        edge.setId(UUID.randomUUID().toString());
        edge.setFrom(from);
        edge.setTo(to);
        edge.setFromPort(fromPort);
        edge.setToPort("input");
        return edge;
    }

    private Uni<OpenAPISpec> parseOpenAPISpec(String spec) {
        // Parse OpenAPI YAML/JSON
        return Uni.createFrom().item(new OpenAPISpec(/* parsed */));
    }

    private IntegrationWorkflowRequest generateIntegrationFromSpec(
            OpenAPISpec spec,
            OpenAPIImportRequest request) {
        // Generate integration request from OpenAPI spec
        return new IntegrationWorkflowRequest(/* generated */);
    }

    private WorkflowDefinitionRequest toWorkflowRequest(WorkflowDefinition def) {
        return new WorkflowDefinitionRequest(
                def.getId(),
                def.getName(),
                def.getDescription(),
                def.getVersion(),
                def.getTenantId(),
                null, // nodes
                null, // edges
                def.getMetadata());
    }
}

// This is Part 1. Continuing with Business Automation Builder and DTOs...