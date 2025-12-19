package tech.kayys.wayang.graphql;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.ConnectionDTO;
import tech.kayys.wayang.schema.ConnectionInput;
import tech.kayys.wayang.schema.CreateWorkflowInput;
import tech.kayys.wayang.schema.NodeDTO;
import tech.kayys.wayang.schema.NodeInput;
import tech.kayys.wayang.schema.NodeLockDTO;
import tech.kayys.wayang.schema.UpdateWorkflowInput;
import tech.kayys.wayang.schema.WorkflowDTO;
import tech.kayys.wayang.service.WorkflowCommandService;

/**
 * Workflow GraphQL API - Mutation operations
 */
@GraphQLApi
@ApplicationScoped
public class WorkflowMutationResolver {

    private static final Logger LOG = Logger.getLogger(WorkflowMutationResolver.class);

    @Inject
    WorkflowCommandService commandService;

    @Inject
    Context context;

    /**
     * Create new workflow
     */
    @Mutation("createWorkflow")
    @Description("Create a new workflow")
    public Uni<WorkflowDTO> createWorkflow(@Name("input") CreateWorkflowInput input) {
        String tenantId = extractTenantId();
        String userId = extractUserId();

        LOG.infof("GraphQL Mutation: createWorkflow(name=%s, tenant=%s)",
                input.getName(), tenantId);

        return commandService.create(input, tenantId, userId);
    }

    /**
     * Update workflow
     */
    @Mutation("updateWorkflow")
    @Description("Update workflow")
    public Uni<WorkflowDTO> updateWorkflow(
            @Name("id") String id,
            @Name("input") UpdateWorkflowInput input) {

        String tenantId = extractTenantId();
        String userId = extractUserId();

        LOG.infof("GraphQL Mutation: updateWorkflow(id=%s)", id);
        return commandService.update(id, input, tenantId, userId);
    }

    /**
     * Delete workflow
     */
    @Mutation("deleteWorkflow")
    @Description("Delete workflow")
    public Uni<Boolean> deleteWorkflow(@Name("id") String id) {
        String tenantId = extractTenantId();
        LOG.infof("GraphQL Mutation: deleteWorkflow(id=%s)", id);
        return commandService.delete(id, tenantId);
    }

    /**
     * Create version
     */
    @Mutation("createVersion")
    @Description("Create new workflow version")
    public Uni<WorkflowVersionDTO> createVersion(
            @Name("workflowId") String workflowId,
            @Name("input") CreateVersionInput input) {

        String tenantId = extractTenantId();
        String userId = extractUserId();
        return commandService.createVersion(workflowId, input, tenantId, userId);
    }

    /**
     * Publish version
     */
    @Mutation("publishVersion")
    @Description("Publish workflow version")
    public Uni<WorkflowVersionDTO> publishVersion(@Name("versionId") String versionId) {
        String tenantId = extractTenantId();
        String userId = extractUserId();
        return commandService.publishVersion(versionId, tenantId, userId);
    }

    /**
     * Add node to workflow
     */
    @Mutation("addNode")
    @Description("Add node to workflow")
    public Uni<NodeDTO> addNode(
            @Name("workflowId") String workflowId,
            @Name("input") NodeInput input) {

        String tenantId = extractTenantId();
        String userId = extractUserId();
        return commandService.addNode(workflowId, input, tenantId, userId);
    }

    /**
     * Update node
     */
    @Mutation("updateNode")
    @Description("Update node in workflow")
    public Uni<NodeDTO> updateNode(
            @Name("workflowId") String workflowId,
            @Name("nodeId") String nodeId,
            @Name("input") NodeInput input) {

        String tenantId = extractTenantId();
        String userId = extractUserId();
        return commandService.updateNode(workflowId, nodeId, input, tenantId, userId);
    }

    /**
     * Delete node
     */
    @Mutation("deleteNode")
    @Description("Delete node from workflow")
    public Uni<Boolean> deleteNode(
            @Name("workflowId") String workflowId,
            @Name("nodeId") String nodeId) {

        String tenantId = extractTenantId();
        return commandService.deleteNode(workflowId, nodeId, tenantId);
    }

    /**
     * Add connection
     */
    @Mutation("addConnection")
    @Description("Add connection between nodes")
    public Uni<ConnectionDTO> addConnection(
            @Name("workflowId") String workflowId,
            @Name("input") ConnectionInput input) {

        String tenantId = extractTenantId();
        String userId = extractUserId();
        return commandService.addConnection(workflowId, input, tenantId, userId);
    }

    /**
     * Lock node for editing
     */
    @Mutation("lockNode")
    @Description("Lock node for exclusive editing")
    public Uni<NodeLockDTO> lockNode(
            @Name("workflowId") String workflowId,
            @Name("nodeId") String nodeId) {

        String tenantId = extractTenantId();
        String userId = extractUserId();
        return commandService.lockNode(workflowId, nodeId, userId, tenantId);
    }

    /**
     * Unlock node
     */
    @Mutation("unlockNode")
    @Description("Unlock node")
    public Uni<Boolean> unlockNode(
            @Name("workflowId") String workflowId,
            @Name("nodeId") String nodeId) {

        String tenantId = extractTenantId();
        String userId = extractUserId();
        return commandService.unlockNode(workflowId, nodeId, userId, tenantId);
    }

    private String extractTenantId() {
        return context.getArgument("tenantId")
                .orElse(context.getHeader("X-Tenant-ID")
                        .orElse("default"));
    }

    private String extractUserId() {
        return context.getArgument("userId")
                .orElse(context.getHeader("X-User-ID")
                        .orElse("anonymous"));
    }
}
