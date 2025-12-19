package tech.kayys.wayang.graphql;

import io.smallrye.graphql.api.Context;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.model.ValidationResult;
import tech.kayys.wayang.schema.ConnectionInput;
import tech.kayys.wayang.schema.NodeInput;
import tech.kayys.wayang.schema.PageInput;
import tech.kayys.wayang.schema.WorkflowConnection;
import tech.kayys.wayang.schema.WorkflowDTO;
import tech.kayys.wayang.schema.WorkflowFilterInput;
import tech.kayys.wayang.service.ValidationService;
import tech.kayys.wayang.service.WorkflowQueryService;

import org.eclipse.microprofile.graphql.*;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Workflow GraphQL API - Query operations
 */
@GraphQLApi
@ApplicationScoped
public class WorkflowQueryResolver {

    private static final Logger LOG = Logger.getLogger(WorkflowQueryResolver.class);

    @Inject
    WorkflowQueryService queryService;

    @Inject
    NodeSchemaService schemaService;

    @Inject
    ValidationService validationService;

    @Inject
    Context context;

    /**
     * Get single workflow by ID
     */
    @Query("workflow")
    @Description("Retrieve workflow by ID")
    public Uni<WorkflowDTO> getWorkflow(@Name("id") String id) {
        String tenantId = extractTenantId();
        LOG.infof("GraphQL Query: workflow(id=%s, tenant=%s)", id, tenantId);
        return queryService.findById(id, tenantId);
    }

    /**
     * List workflows with filtering and pagination
     */
    @Query("workflows")
    @Description("List workflows with filtering")
    public Uni<WorkflowConnection> listWorkflows(
            @Name("filter") WorkflowFilterInput filter,
            @Name("page") PageInput page) {

        String tenantId = extractTenantId();
        LOG.infof("GraphQL Query: workflows(tenant=%s, filter=%s)", tenantId, filter);
        return queryService.findAll(tenantId, filter, page);
    }

    /**
     * Get workflow versions
     */
    @Query("workflowVersions")
    @Description("Get all versions of a workflow")
    public Uni<List<WorkflowVersionDTO>> getWorkflowVersions(@Name("workflowId") String workflowId) {
        String tenantId = extractTenantId();
        return queryService.findVersions(workflowId, tenantId);
    }

    /**
     * Get node schema definition
     */
    @Query("nodeSchema")
    @Description("Get node schema by ID")
    public Uni<NodeSchemaDTO> getNodeSchema(@Name("id") String id) {
        return schemaService.findById(id);
    }

    /**
     * List available node schemas
     */
    @Query("nodeSchemas")
    @Description("List available node types")
    public Uni<List<NodeSchemaDTO>> listNodeSchemas(@Name("filter") NodeSchemaFilterInput filter) {
        return schemaService.findAll(filter);
    }

    /**
     * Get node categories for UI
     */
    @Query("nodeCategories")
    @Description("Get node categories for palette")
    public Uni<List<NodeCategory>> getNodeCategories() {
        return schemaService.getCategories();
    }

    /**
     * Validate workflow structure
     */
    @Query("validateWorkflow")
    @Description("Validate workflow definition")
    public Uni<ValidationResult> validateWorkflow(@Name("input") WorkflowInput input) {
        String tenantId = extractTenantId();
        return validationService.validateWorkflow(input, tenantId);
    }

    /**
     * Validate single node
     */
    @Query("validateNode")
    @Description("Validate node configuration")
    public Uni<ValidationResult> validateNode(@Name("input") NodeInput input) {
        return validationService.validateNode(input);
    }

    /**
     * Validate connection
     */
    @Query("validateConnection")
    @Description("Validate connection between nodes")
    public Uni<ValidationResult> validateConnection(@Name("input") ConnectionInput input) {
        String tenantId = extractTenantId();
        return validationService.validateConnection(input, tenantId);
    }

    /**
     * Get tool capabilities for node type
     */
    @Query("toolCapabilities")
    @Description("Get available tool capabilities for node type")
    public Uni<List<ToolCapability>> getToolCapabilities(@Name("nodeType") String nodeType) {
        return schemaService.getToolCapabilities(nodeType);
    }

    /**
     * Get agent capabilities
     */
    @Query("agentCapabilities")
    @Description("Get available agent capabilities")
    public Uni<List<AgentCapability>> getAgentCapabilities() {
        return schemaService.getAgentCapabilities();
    }

    /**
     * Compare two workflows
     */
    @Query("compareWorkflows")
    @Description("Compare two workflow versions")
    public Uni<WorkflowDiffDTO> compareWorkflows(
            @Name("baseId") String baseId,
            @Name("targetId") String targetId) {

        String tenantId = extractTenantId();
        return queryService.compareWorkflows(baseId, targetId, tenantId);
    }

    private String extractTenantId() {
        // Extract from JWT token or context
        java.util.Optional<String> argument = context.getArgument("tenantId");
        return argument.orElse("default");
    }
}
