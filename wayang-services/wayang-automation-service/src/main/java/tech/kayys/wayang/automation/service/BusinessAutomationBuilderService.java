package tech.kayys.wayang.automation.service;

import tech.kayys.wayang.automation.dto.ApprovalDecision;
import tech.kayys.wayang.automation.dto.ApprovalLevel;
import tech.kayys.wayang.automation.dto.ApprovalWorkflowRequest;
import tech.kayys.wayang.automation.dto.BusinessProcessRequest;
import tech.kayys.wayang.automation.dto.BusinessProcessResponse;
import tech.kayys.wayang.automation.dto.DocumentProcessingRequest;
import tech.kayys.wayang.automation.dto.ExtractionRule;
import tech.kayys.wayang.automation.dto.PendingApproval;
import tech.kayys.wayang.automation.dto.ProcessExecutionRequest;
import tech.kayys.wayang.automation.dto.ProcessExecutionResponse;
import tech.kayys.wayang.automation.dto.ProcessStep;
import tech.kayys.wayang.automation.dto.ProcessType;
import tech.kayys.wayang.automation.dto.RPAAction;
import tech.kayys.wayang.automation.dto.RPAWorkflowRequest;
import tech.kayys.wayang.schema.execution.WaitFor;
import tech.kayys.wayang.schema.node.EdgeDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.sdk.HITLClient;
import tech.kayys.wayang.sdk.WorkflowDefinitionClient;
import tech.kayys.wayang.sdk.WorkflowRunClient;
import tech.kayys.wayang.sdk.dto.TriggerWorkflowRequest;
import tech.kayys.wayang.sdk.dto.WorkflowDefinitionRequest;
import tech.kayys.wayang.sdk.dto.htil.TaskAction;
import tech.kayys.wayang.sdk.dto.htil.TaskCompletionRequest;
import tech.kayys.wayang.sdk.dto.htil.TaskPriority;
import tech.kayys.wayang.sdk.dto.htil.TaskType;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Business Automation Builder Service
 * 
 * Features:
 * - Business process workflows (BPMN-style)
 * - Approval workflows
 * - Document processing automation
 * - RPA (Robotic Process Automation)
 * - Decision tables
 * - Business rules engine
 * - Human task management
 */
@Path("/api/v1/business-automation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class BusinessAutomationBuilderService {

    private static final Logger log = LoggerFactory.getLogger(
            BusinessAutomationBuilderService.class);

    @Inject
    @RestClient
    WorkflowRunClient workflowClient;

    @Inject
    @RestClient
    WorkflowDefinitionClient workflowDefClient;

    @Inject
    @RestClient
    HITLClient hitlClient;

    @Inject
    BusinessProcessLibrary processLibrary;

    /**
     * Create business process automation
     */
    @POST
    @Path("/processes")
    public Uni<BusinessProcessResponse> createBusinessProcess(
            BusinessProcessRequest request) {
        log.info("Creating business process: {}, type: {}",
                request.name(), request.processType());

        return buildBusinessProcess(request)
                .onItem().transformToUni(workflow -> workflowDefClient.createWorkflow(toWorkflowRequest(workflow)))
                .map(created -> new BusinessProcessResponse(
                        created.id(),
                        created.name(),
                        request.processType(),
                        created.status()));
    }

    /**
     * Create approval workflow
     */
    @POST
    @Path("/approvals")
    public Uni<BusinessProcessResponse> createApprovalWorkflow(
            ApprovalWorkflowRequest request) {
        log.info("Creating approval workflow: {}", request.name());

        WorkflowDefinition workflow = buildApprovalWorkflow(request);

        return workflowDefClient.createWorkflow(toWorkflowRequest(workflow))
                .map(created -> new BusinessProcessResponse(
                        created.id(),
                        created.name(),
                        ProcessType.APPROVAL,
                        created.status()));
    }

    /**
     * Execute business process
     */
    @POST
    @Path("/processes/{processId}/execute")
    public Uni<ProcessExecutionResponse> executeProcess(
            @PathParam("processId") String processId,
            ProcessExecutionRequest request) {
        log.info("Executing process: {}", processId);

        TriggerWorkflowRequest triggerRequest = new TriggerWorkflowRequest(
                processId,
                request.tenantId(),
                request.initiatedBy(),
                request.processData());

        return workflowClient.triggerWorkflow(triggerRequest)
                .map(run -> new ProcessExecutionResponse(
                        run.runId(),
                        run.status().toString(),
                        run.currentNodeId(),
                        run.createdAt()));
    }

    /**
     * Get pending approvals for user
     */
    @GET
    @Path("/approvals/pending")
    public Uni<List<PendingApproval>> getPendingApprovals(
            @QueryParam("userId") String userId,
            @QueryParam("tenantId") String tenantId) {
        return hitlClient.getPendingTasks(userId, TaskPriority.NORMAL)
                .map(tasks -> tasks.stream()
                        .filter(task -> task.taskType() == TaskType.APPROVAL)
                        .map(task -> new PendingApproval(
                                task.taskId(),
                                task.runId(),
                                task.context(),
                                task.createdAt(),
                                task.dueAt()))
                        .toList());
    }

    /**
     * Approve/Reject task
     */
    @POST
    @Path("/approvals/{taskId}/decide")
    public Uni<Void> decideApproval(
            @PathParam("taskId") String taskId,
            ApprovalDecision decision) {
        log.info("Processing approval decision for task: {}, decision: {}",
                taskId, decision.approved());

        TaskCompletionRequest completionRequest = new TaskCompletionRequest(
                new TaskAction(
                        decision.approved() ? "approve" : "reject",
                        decision.approved() ? "Approve" : "Reject",
                        "Approval decision"),
                Map.of(
                        "approved", decision.approved(),
                        "reason", decision.reason(),
                        "comments", decision.comments()),
                decision.notes(),
                decision.decidedBy());

        return hitlClient.completeTask(taskId, completionRequest);
    }

    /**
     * Create document processing workflow
     */
    @POST
    @Path("/documents/process")
    public Uni<BusinessProcessResponse> createDocumentProcessor(
            DocumentProcessingRequest request) {
        log.info("Creating document processor: {}", request.name());

        WorkflowDefinition workflow = buildDocumentProcessingWorkflow(request);

        return workflowDefClient.createWorkflow(toWorkflowRequest(workflow))
                .map(created -> new BusinessProcessResponse(
                        created.id(),
                        created.name(),
                        ProcessType.DOCUMENT_PROCESSING,
                        created.status()));
    }

    /**
     * Create RPA workflow
     */
    @POST
    @Path("/rpa")
    public Uni<BusinessProcessResponse> createRPAWorkflow(
            RPAWorkflowRequest request) {
        log.info("Creating RPA workflow: {}", request.name());

        WorkflowDefinition workflow = buildRPAWorkflow(request);

        return workflowDefClient.createWorkflow(toWorkflowRequest(workflow))
                .map(created -> new BusinessProcessResponse(
                        created.id(),
                        created.name(),
                        ProcessType.RPA,
                        created.status()));
    }

    /**
     * Get business process templates
     */
    @GET
    @Path("/templates")
    public Uni<List<ProcessTemplate>> getTemplates(
            @QueryParam("industry") String industry,
            @QueryParam("type") ProcessType type) {
        return processLibrary.getTemplates(industry, type);
    }

    // ========================================================================
    // Workflow Builders
    // ========================================================================

    /**
     * Build business process workflow
     */
    private Uni<WorkflowDefinition> buildBusinessProcess(
            BusinessProcessRequest request) {
        return Uni.createFrom().item(() -> {
            WorkflowDefinition workflow = new WorkflowDefinition();
            workflow.setId(UUID.randomUUID().toString());
            workflow.setName(request.name());
            workflow.setDescription(request.description());
            workflow.setTenantId(request.tenantId());

            List<NodeDefinition> nodes = new ArrayList<>();
            List<EdgeDefinition> edges = new ArrayList<>();

            // Start event
            NodeDefinition startNode = createStartEventNode();
            nodes.add(startNode);
            String previousNodeId = startNode.getId();

            // Add process steps
            for (ProcessStep step : request.steps()) {
                NodeDefinition stepNode = createProcessStepNode(step);
                nodes.add(stepNode);
                edges.add(createEdge(previousNodeId, stepNode.getId()));

                // Add decision gateway if conditional
                if (step.isConditional()) {
                    NodeDefinition gateway = createGatewayNode(step.condition());
                    nodes.add(gateway);
                    edges.add(createEdge(stepNode.getId(), gateway.getId()));
                    previousNodeId = gateway.getId();
                } else {
                    previousNodeId = stepNode.getId();
                }
            }

            // End event
            NodeDefinition endNode = createEndEventNode();
            nodes.add(endNode);
            edges.add(createEdge(previousNodeId, endNode.getId()));

            workflow.setNodes(nodes);
            workflow.setEdges(edges);

            return workflow;
        });
    }

    /**
     * Build approval workflow
     */
    private WorkflowDefinition buildApprovalWorkflow(
            ApprovalWorkflowRequest request) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(UUID.randomUUID().toString());
        workflow.setName(request.name());
        workflow.setTenantId(request.tenantId());

        List<NodeDefinition> nodes = new ArrayList<>();
        List<EdgeDefinition> edges = new ArrayList<>();

        // Start
        NodeDefinition startNode = createStartEventNode();
        nodes.add(startNode);

        // Initialize approval request
        NodeDefinition initNode = createApprovalInitNode(request);
        nodes.add(initNode);
        edges.add(createEdge(startNode.getId(), initNode.getId()));

        // Multi-level approvals
        String previousNodeId = initNode.getId();
        for (ApprovalLevel level : request.approvalLevels()) {
            // Human task node for approval
            NodeDefinition approvalNode = createApprovalTaskNode(level);
            nodes.add(approvalNode);
            edges.add(createEdge(previousNodeId, approvalNode.getId()));

            // Decision gateway
            NodeDefinition decisionNode = createApprovalDecisionNode(level);
            nodes.add(decisionNode);
            edges.add(createEdge(approvalNode.getId(), decisionNode.getId()));

            // If rejected, go to rejection handler
            if (request.allowRejection()) {
                NodeDefinition rejectNode = createRejectionHandlerNode();
                nodes.add(rejectNode);
                edges.add(createConditionalEdge(
                        decisionNode.getId(),
                        rejectNode.getId(),
                        "approved == false"));
            }

            previousNodeId = decisionNode.getId();
        }

        // Final approval notification
        NodeDefinition notifyNode = createNotificationNode("Approved");
        nodes.add(notifyNode);
        edges.add(createEdge(previousNodeId, notifyNode.getId()));

        // End
        NodeDefinition endNode = createEndEventNode();
        nodes.add(endNode);
        edges.add(createEdge(notifyNode.getId(), endNode.getId()));

        workflow.setNodes(nodes);
        workflow.setEdges(edges);

        return workflow;
    }

    /**
     * Build document processing workflow
     */
    private WorkflowDefinition buildDocumentProcessingWorkflow(
            DocumentProcessingRequest request) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(UUID.randomUUID().toString());
        workflow.setName(request.name());
        workflow.setTenantId(request.tenantId());

        List<NodeDefinition> nodes = new ArrayList<>();
        List<EdgeDefinition> edges = new ArrayList<>();

        // Document upload trigger
        NodeDefinition triggerNode = createDocumentTriggerNode();
        nodes.add(triggerNode);

        // Document classification
        NodeDefinition classifyNode = createDocumentClassifierNode();
        nodes.add(classifyNode);
        edges.add(createEdge(triggerNode.getId(), classifyNode.getId()));

        // OCR/Text extraction
        NodeDefinition ocrNode = createOCRNode();
        nodes.add(ocrNode);
        edges.add(createEdge(classifyNode.getId(), ocrNode.getId()));

        // Data extraction
        NodeDefinition extractNode = createDataExtractionNode(request.extractionRules());
        nodes.add(extractNode);
        edges.add(createEdge(ocrNode.getId(), extractNode.getId()));

        // Validation
        NodeDefinition validateNode = createDataValidationNode();
        nodes.add(validateNode);
        edges.add(createEdge(extractNode.getId(), validateNode.getId()));

        // Human review (if validation fails)
        NodeDefinition reviewNode = createHumanReviewNode();
        nodes.add(reviewNode);
        edges.add(createConditionalEdge(
                validateNode.getId(),
                reviewNode.getId(),
                "confidence < 0.8"));

        // Store processed document
        NodeDefinition storeNode = createDocumentStorageNode();
        nodes.add(storeNode);
        edges.add(createEdge(validateNode.getId(), storeNode.getId()));
        edges.add(createEdge(reviewNode.getId(), storeNode.getId()));

        // End
        NodeDefinition endNode = createEndEventNode();
        nodes.add(endNode);
        edges.add(createEdge(storeNode.getId(), endNode.getId()));

        workflow.setNodes(nodes);
        workflow.setEdges(edges);

        return workflow;
    }

    /**
     * Build RPA workflow
     */
    private WorkflowDefinition buildRPAWorkflow(RPAWorkflowRequest request) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(UUID.randomUUID().toString());
        workflow.setName(request.name());
        workflow.setTenantId(request.tenantId());

        List<NodeDefinition> nodes = new ArrayList<>();
        List<EdgeDefinition> edges = new ArrayList<>();

        // Start
        NodeDefinition startNode = createStartEventNode();
        nodes.add(startNode);
        String previousNodeId = startNode.getId();

        // RPA actions
        for (RPAAction action : request.actions()) {
            NodeDefinition actionNode = createRPAActionNode(action);
            nodes.add(actionNode);
            edges.add(createEdge(previousNodeId, actionNode.getId()));
            previousNodeId = actionNode.getId();
        }

        // End
        NodeDefinition endNode = createEndEventNode();
        nodes.add(endNode);
        edges.add(createEdge(previousNodeId, endNode.getId()));

        workflow.setNodes(nodes);
        workflow.setEdges(edges);

        return workflow;
    }

    // ========================================================================
    // Node Creation Methods
    // ========================================================================

    private NodeDefinition createStartEventNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("start-event");
        node.setType("bpmn-start-event");
        node.setDisplayName("Start");
        return node;
    }

    private NodeDefinition createEndEventNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("end-event");
        node.setType("bpmn-end-event");
        node.setDisplayName("End");
        return node;
    }

    private NodeDefinition createProcessStepNode(ProcessStep step) {
        NodeDefinition node = new NodeDefinition();
        node.setId("step-" + UUID.randomUUID().toString());
        node.setType(step.type());
        node.setDisplayName(step.name());
        return node;
    }

    private NodeDefinition createGatewayNode(String condition) {
        NodeDefinition node = new NodeDefinition();
        node.setId("gateway-" + UUID.randomUUID().toString());
        node.setType("bpmn-exclusive-gateway");
        node.setDisplayName("Decision Gateway");
        return node;
    }

    private NodeDefinition createApprovalInitNode(ApprovalWorkflowRequest request) {
        NodeDefinition node = new NodeDefinition();
        node.setId("init-approval");
        node.setType("approval-initializer");
        node.setDisplayName("Initialize Approval");
        return node;
    }

    private NodeDefinition createApprovalTaskNode(ApprovalLevel level) {
        NodeDefinition node = new NodeDefinition();
        node.setId("approval-" + level.levelNumber());
        node.setType("human-task");
        node.setDisplayName("Approval Level " + level.levelNumber());

        WaitFor waitFor = new WaitFor();
        waitFor.setType("human");
        waitFor.setTask(new HumanTask(
                null,
                "Please review and approve",
                List.of("approve", "reject"),
                "approve"));
        node.setWaitFor(waitFor);

        return node;
    }

    private NodeDefinition createApprovalDecisionNode(ApprovalLevel level) {
        NodeDefinition node = new NodeDefinition();
        node.setId("decision-" + level.levelNumber());
        node.setType("decision");
        node.setDisplayName("Decision");
        return node;
    }

    private NodeDefinition createRejectionHandlerNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("rejection-handler");
        node.setType("notification");
        node.setDisplayName("Handle Rejection");
        return node;
    }

    private NodeDefinition createNotificationNode(String type) {
        NodeDefinition node = new NodeDefinition();
        node.setId("notify-" + type.toLowerCase());
        node.setType("notification");
        node.setDisplayName("Notify: " + type);
        return node;
    }

    private NodeDefinition createDocumentTriggerNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("doc-trigger");
        node.setType("document-trigger");
        node.setDisplayName("Document Uploaded");
        return node;
    }

    private NodeDefinition createDocumentClassifierNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("classify-doc");
        node.setType("document-classifier");
        node.setDisplayName("Classify Document");
        return node;
    }

    private NodeDefinition createOCRNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("ocr");
        node.setType("ocr-processor");
        node.setDisplayName("Extract Text (OCR)");
        return node;
    }

    private NodeDefinition createDataExtractionNode(List<ExtractionRule> rules) {
        NodeDefinition node = new NodeDefinition();
        node.setId("extract-data");
        node.setType("data-extractor");
        node.setDisplayName("Extract Data");
        return node;
    }

    private NodeDefinition createDataValidationNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("validate");
        node.setType("validator");
        node.setDisplayName("Validate Data");
        return node;
    }

    private NodeDefinition createHumanReviewNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("human-review");
        node.setType("human-task");
        node.setDisplayName("Human Review");
        return node;
    }

    private NodeDefinition createDocumentStorageNode() {
        NodeDefinition node = new NodeDefinition();
        node.setId("store-doc");
        node.setType("document-storage");
        node.setDisplayName("Store Document");
        return node;
    }

    private NodeDefinition createRPAActionNode(RPAAction action) {
        NodeDefinition node = new NodeDefinition();
        node.setId("rpa-" + UUID.randomUUID().toString());
        node.setType("rpa-action");
        node.setDisplayName(action.name());
        return node;
    }

    private EdgeDefinition createEdge(String from, String to) {
        EdgeDefinition edge = new EdgeDefinition();
        edge.setId(UUID.randomUUID().toString());
        edge.setFrom(from);
        edge.setTo(to);
        edge.setFromPort("success");
        edge.setToPort("input");
        return edge;
    }

    private EdgeDefinition createConditionalEdge(String from, String to, String condition) {
        EdgeDefinition edge = createEdge(from, to);
        edge.setCondition(condition);
        return edge;
    }

    private WorkflowDefinitionRequest toWorkflowRequest(WorkflowDefinition def) {
        return new WorkflowDefinitionRequest(
                def.getId(),
                def.getName(),
                def.getDescription(),
                def.getVersion(),
                def.getTenantId(),
                null,
                null,
                def.getMetadata());
    }
}
