package tech.kayys.wayang.graphql;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.dto.CreateWorkspaceInput;
import tech.kayys.wayang.dto.UpdateWorkspaceInput;
import tech.kayys.wayang.service.WorkspaceService;
import tech.kayys.wayang.tenant.TenantContext;

import org.eclipse.microprofile.graphql.*;

import java.util.List;
import java.util.UUID;

/**
 * WorkspaceGraphQLResource - GraphQL API for workspaces
 */
@GraphQLApi
public class WorkspaceGraphQLResource {

    @Inject
    WorkspaceService workspaceService;

    @Inject
    TenantContext tenantContext;

    @Inject
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * Query all workspaces
     */
    @Query("workspaces")
    @Description("Get all workspaces for current tenant")
    public Uni<List<WorkspaceQL>> workspaces() {
        return workspaceService.listWorkspaces()
                .map(workspaces -> workspaces.stream()
                        .map(WorkspaceQL::from)
                        .toList());
    }

    /**
     * Query workspace by ID
     */
    @Query("workspace")
    @Description("Get workspace by ID")
    public Uni<WorkspaceQL> workspace(@Name("id") String id) {
        return workspaceService.getWorkspace(UUID.fromString(id))
                .map(WorkspaceQL::from);
    }

    /**
     * Create workspace
     */
    @Mutation("createWorkspace")
    @Description("Create a new workspace")
    public Uni<WorkspaceQL> createWorkspace(@Name("input") CreateWorkspaceInput input) {
        try {
            WorkspaceService.CreateWorkspaceRequest request = new WorkspaceService.CreateWorkspaceRequest();
            request.name = input.name;
            request.description = input.description;
            if (input.metadata != null) {
                request.metadata = objectMapper.readValue(input.metadata, java.util.Map.class);
            }

            return workspaceService.createWorkspace(request)
                    .map(WorkspaceQL::from);
        } catch (Exception e) {
            throw new RuntimeException("Invalid metadata JSON", e);
        }
    }

    /**
     * Update workspace
     */
    @Mutation("updateWorkspace")
    @Description("Update workspace")
    public Uni<WorkspaceQL> updateWorkspace(
            @Name("id") String id,
            @Name("input") UpdateWorkspaceInput input) {

        try {
            WorkspaceService.UpdateWorkspaceRequest request = new WorkspaceService.UpdateWorkspaceRequest();
            request.name = input.name;
            request.description = input.description;
            if (input.metadata != null) {
                request.metadata = objectMapper.readValue(input.metadata, java.util.Map.class);
            }

            return workspaceService.updateWorkspace(UUID.fromString(id), request)
                    .map(WorkspaceQL::from);
        } catch (Exception e) {
            throw new RuntimeException("Invalid metadata JSON", e);
        }
    }

    /**
     * Delete workspace
     */
    @Mutation("deleteWorkspace")
    @Description("Delete workspace")
    public Uni<Boolean> deleteWorkspace(@Name("id") String id) {
        return workspaceService.deleteWorkspace(UUID.fromString(id))
                .map(v -> true);
    }
}