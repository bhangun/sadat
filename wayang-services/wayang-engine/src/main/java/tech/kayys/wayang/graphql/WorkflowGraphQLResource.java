package tech.kayys.wayang.graphql;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.dto.CreateWorkflowInput;
import tech.kayys.wayang.model.LogicDefinition;
import tech.kayys.wayang.model.RuntimeConfig;
import tech.kayys.wayang.model.UIDefinition;
import tech.kayys.wayang.service.ValidationService;
import tech.kayys.wayang.service.WorkflowService;

/**
 * WorkflowGraphQLResource - GraphQL API for workflows
 */
@GraphQLApi
public class WorkflowGraphQLResource {

        @Inject
        WorkflowService workflowService;

        @Inject
        ValidationService validationService;

        @Inject
        com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        /**
         * Query workflows in workspace
         */
        @Query("workflows")
        @Description("Get all workflows in workspace")
        public Uni<List<WorkflowQL>> workflows(@Name("workspaceId") String workspaceId) {
                return workflowService.listWorkflows(UUID.fromString(workspaceId))
                                .map(workflows -> workflows.stream()
                                                .map(WorkflowQL::from)
                                                .toList());
        }

        /**
         * Query workflow by ID
         */
        @Query("workflow")
        @Description("Get workflow by ID")
        public Uni<WorkflowQL> workflow(@Name("id") String id) {
                return workflowService.getWorkflow(UUID.fromString(id))
                                .map(WorkflowQL::from);
        }

        /**
         * Search workflows
         */
        @Query("searchWorkflows")
        @Description("Search workflows by name or description")
        public Uni<List<WorkflowQL>> searchWorkflows(
                        @Name("workspaceId") String workspaceId,
                        @Name("query") String query) {

                return workflowService.listWorkflows(UUID.fromString(workspaceId))
                                .map(workflows -> workflows.stream()
                                                .filter(w -> w.name.toLowerCase().contains(query.toLowerCase()) ||
                                                                (w.description != null && w.description.toLowerCase()
                                                                                .contains(query.toLowerCase())))
                                                .map(WorkflowQL::from)
                                                .toList());
        }

        /**
         * Create workflow
         */
        @Mutation("createWorkflow")
        @Description("Create a new workflow")
        public Uni<WorkflowQL> createWorkflow(
                        @Name("workspaceId") String workspaceId,
                        @Name("input") CreateWorkflowInput input) {

                try {
                        WorkflowService.CreateWorkflowRequest request = new WorkflowService.CreateWorkflowRequest();
                        request.name = input.name;
                        request.description = input.description;
                        request.version = input.version;

                        if (input.logic != null) {
                                request.logic = objectMapper.readValue(input.logic, LogicDefinition.class);
                        }
                        if (input.ui != null) {
                                request.ui = objectMapper.readValue(input.ui, UIDefinition.class);
                        }
                        if (input.runtime != null) {
                                request.runtime = objectMapper.readValue(input.runtime, RuntimeConfig.class);
                        }
                        if (input.metadata != null) {
                                request.metadata = objectMapper.readValue(input.metadata, java.util.Map.class);
                        }

                        return workflowService.createWorkflow(UUID.fromString(workspaceId), request)
                                        .map(WorkflowQL::from);
                } catch (Exception e) {
                        throw new RuntimeException("Invalid JSON input", e);
                }
        }

        /**
         * Update workflow logic
         */
        @Mutation("updateWorkflowLogic")
        @Description("Update workflow logic")
        public Uni<WorkflowQL> updateWorkflowLogic(
                        @Name("id") String id,
                        @Name("logic") String logicJson,
                        @Name("expectedVersion") Long expectedVersion) {

                try {
                        LogicDefinition logic = objectMapper.readValue(logicJson, LogicDefinition.class);
                        return workflowService.updateLogic(UUID.fromString(id), logic, expectedVersion)
                                        .map(WorkflowQL::from);
                } catch (Exception e) {
                        throw new RuntimeException("Invalid logic JSON", e);
                }
        }

        /**
         * Update workflow UI
         */
        @Mutation("updateWorkflowUI")
        @Description("Update workflow UI definition")
        public Uni<WorkflowQL> updateWorkflowUI(
                        @Name("id") String id,
                        @Name("ui") String uiJson) {

                try {
                        UIDefinition ui = objectMapper.readValue(uiJson, UIDefinition.class);
                        return workflowService.updateUI(UUID.fromString(id), ui)
                                        .map(WorkflowQL::from);
                } catch (Exception e) {
                        throw new RuntimeException("Invalid UI JSON", e);
                }
        }

        /**
         * Validate workflow
         */
        @Mutation("validateWorkflow")
        @Description("Validate workflow structure and logic")
        public Uni<ValidationResultQL> validateWorkflow(@Name("id") String id) {
                return workflowService.validateWorkflow(UUID.fromString(id))
                                .map(ValidationResultQL::from);
        }

        /**
         * Publish workflow
         */
        @Mutation("publishWorkflow")
        @Description("Publish workflow as immutable version")
        public Uni<WorkflowQL> publishWorkflow(@Name("id") String id) {
                return workflowService.publishWorkflow(UUID.fromString(id))
                                .map(WorkflowQL::from);
        }

        /**
         * Delete workflow
         */
        @Mutation("deleteWorkflow")
        @Description("Delete workflow")
        public Uni<Boolean> deleteWorkflow(@Name("id") String id) {
                return workflowService.deleteWorkflow(UUID.fromString(id))
                                .map(v -> true);
        }
}
