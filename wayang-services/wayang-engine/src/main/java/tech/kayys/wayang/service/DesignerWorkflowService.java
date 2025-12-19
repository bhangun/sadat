package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import org.jboss.logging.Logger;
import tech.kayys.wayang.graphql.WorkflowGraphQLClient;
import tech.kayys.wayang.schema.CreateWorkflowInput;
import tech.kayys.wayang.schema.ExecutionRequest;
import tech.kayys.wayang.schema.ExecutionResponse;
import tech.kayys.wayang.schema.PageInput;
import tech.kayys.wayang.schema.PublishRequest;
import tech.kayys.wayang.schema.PublishResponse;
import tech.kayys.wayang.schema.WorkflowConnection;
import tech.kayys.wayang.schema.WorkflowDTO;
import tech.kayys.wayang.schema.WorkflowFilterInput;

/**
 * Designer Service - Orchestrates GraphQL and REST clients
 */
@ApplicationScoped
public class DesignerWorkflowService {

    private static final Logger LOG = Logger.getLogger(DesignerWorkflowService.class);

    @Inject
    WorkflowGraphQLClient graphQLClient;

    @Inject
    WorkflowRestClient restClient;

    @Inject
    DesignerWebSocketClient wsClient;

    /**
     * Get workflow (GraphQL)
     */
    public Uni<WorkflowDTO> getWorkflow(String id, String tenantId) {
        LOG.infof("Designer: Fetching workflow %s via GraphQL", id);
        return graphQLClient.getWorkflow(id)
                .onFailure().retry().atMost(3);
    }

    /**
     * List workflows with filtering (GraphQL)
     */
    public Uni<WorkflowConnection> listWorkflows(WorkflowFilterInput filter,
            PageInput page) {
        LOG.infof("Designer: Listing workflows via GraphQL");
        return graphQLClient.listWorkflows(filter, page);
    }

    /**
     * Create workflow (GraphQL)
     */
    public Uni<WorkflowDTO> createWorkflow(CreateWorkflowInput input) {
        LOG.infof("Designer: Creating workflow via GraphQL");
        return graphQLClient.createWorkflow(input);
    }

    /**
     * Execute workflow (REST)
     */
    public Uni<ExecutionResponse> executeWorkflow(String workflowId,
            ExecutionRequest request,
            String tenantId) {
        LOG.infof("Designer: Executing workflow %s via REST", workflowId);
        return restClient.executeWorkflow(workflowId, tenantId, request);
    }

    /**
     * Publish workflow (REST)
     */
    public Uni<PublishResponse> publishWorkflow(String workflowId,
            PublishRequest request,
            String tenantId) {
        LOG.infof("Designer: Publishing workflow %s via REST", workflowId);
        return restClient.publishWorkflow(workflowId, tenantId, request);
    }

    /**
     * Export workflow (REST)
     */
    public Uni<File> exportWorkflow(String workflowId, String format,
            String tenantId) {
        LOG.infof("Designer: Exporting workflow %s via REST", workflowId);
        return restClient.exportWorkflow(workflowId, tenantId, format);
    }

    /**
     * Connect to collaboration WebSocket
     */
    public Uni<Void> connectCollaboration(String workflowId, String userId,
            String tenantId,
            CollaborationHandler handler) {
        LOG.infof("Designer: Connecting to collaboration WebSocket for workflow %s",
                workflowId);
        return wsClient.connect(workflowId, userId, tenantId, handler);
    }
}
