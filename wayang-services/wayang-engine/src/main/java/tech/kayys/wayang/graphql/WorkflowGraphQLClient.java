package tech.kayys.wayang.graphql;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.graphql.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.schema.CreateWorkflowInput;
import tech.kayys.wayang.schema.NodeDTO;
import tech.kayys.wayang.schema.NodeInput;
import tech.kayys.wayang.schema.NodeLockDTO;
import tech.kayys.wayang.schema.PageInput;
import tech.kayys.wayang.schema.UpdateWorkflowInput;
import tech.kayys.wayang.schema.WorkflowConnection;
import tech.kayys.wayang.schema.WorkflowDTO;
import tech.kayys.wayang.schema.WorkflowFilterInput;
import tech.kayys.wayang.service.SchemaProcessor.ValidationResult;

import java.util.List;

/**
 * GraphQL Client for Workflow Service
 */
@GraphQLClientApi(configKey = "workflow-service-graphql")
public interface WorkflowGraphQLClient {

    // Queries

    @Query("workflow")
    Uni<WorkflowDTO> getWorkflow(@Name("id") String id);

    @Query("workflows")
    Uni<WorkflowConnection> listWorkflows(
            @Name("filter") WorkflowFilterInput filter,
            @Name("page") PageInput page);

    @Query("nodeSchema")
    Uni<NodeSchemaDTO> getNodeSchema(@Name("id") String id);

    @Query("nodeSchemas")
    Uni<List<NodeSchemaDTO>> listNodeSchemas(
            @Name("filter") NodeSchemaFilterInput filter);

    @Query("validateWorkflow")
    Uni<ValidationResult> validateWorkflow(@Name("input") WorkflowInput input);

    @Query("compareWorkflows")
    Uni<WorkflowDiffDTO> compareWorkflows(
            @Name("baseId") String baseId,
            @Name("targetId") String targetId);

    // Mutations

    @Mutation("createWorkflow")
    Uni<WorkflowDTO> createWorkflow(@Name("input") CreateWorkflowInput input);

    @Mutation("updateWorkflow")
    Uni<WorkflowDTO> updateWorkflow(
            @Name("id") String id,
            @Name("input") UpdateWorkflowInput input);

    @Mutation("addNode")
    Uni<NodeDTO> addNode(
            @Name("workflowId") String workflowId,
            @Name("input") NodeInput input);

    @Mutation("lockNode")
    Uni<NodeLockDTO> lockNode(
            @Name("workflowId") String workflowId,
            @Name("nodeId") String nodeId);
}
