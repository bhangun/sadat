package tech.kayys.wayang.graphql;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.graphql.Ignore;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Source;
import org.eclipse.microprofile.graphql.Type;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.spi.CDI;
import tech.kayys.wayang.domain.Workspace;
import tech.kayys.wayang.service.WorkflowService;

/**
 * WorkspaceQL - GraphQL type for Workspace
 */
@Type("Workspace")
public class WorkspaceQL {

    @NonNull
    public String id;

    @NonNull
    public String name;

    public String description;

    @NonNull
    public String tenantId;

    @NonNull
    public String ownerId;

    @NonNull
    public java.time.Instant createdAt;

    public java.time.Instant updatedAt;

    @NonNull
    public String status;

    @Ignore
    public java.util.Map<String, Object> metadata;

    /**
     * Nested query - get workflows in workspace
     */
    public Uni<List<WorkflowQL>> workflows(@Source WorkspaceQL workspace) {
        // Injected via DataFetchingEnvironment
        WorkflowService workflowService = CDI.current().select(WorkflowService.class).get();
        return workflowService.listWorkflows(UUID.fromString(workspace.id))
                .map(workflows -> workflows.stream()
                        .map(WorkflowQL::from)
                        .toList());
    }

    public static WorkspaceQL from(Workspace workspace) {
        WorkspaceQL ql = new WorkspaceQL();
        ql.id = workspace.id.toString();
        ql.name = workspace.name;
        ql.description = workspace.description;
        ql.tenantId = workspace.tenantId;
        ql.ownerId = workspace.ownerId;
        ql.createdAt = workspace.createdAt;
        ql.updatedAt = workspace.updatedAt;
        ql.status = workspace.status.name();
        ql.metadata = workspace.metadata;
        return ql;
    }
}
