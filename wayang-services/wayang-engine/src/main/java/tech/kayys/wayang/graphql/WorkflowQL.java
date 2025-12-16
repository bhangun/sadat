package tech.kayys.wayang.graphql;

import org.eclipse.microprofile.graphql.Ignore;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.model.LogicDefinition;
import tech.kayys.wayang.model.RuntimeConfig;
import tech.kayys.wayang.model.UIDefinition;
import tech.kayys.wayang.model.ValidationResult;

/**
 * WorkflowQL - GraphQL type for Workflow
 */
@Type("Workflow")
public class WorkflowQL {

    @NonNull
    public String id;

    @NonNull
    public String name;

    public String description;

    @NonNull
    public String version;

    @NonNull
    public String status;

    @Ignore
    @NonNull
    public LogicDefinition logic;

    @Ignore
    public UIDefinition ui;

    @Ignore
    public RuntimeConfig runtime;

    @Ignore
    public ValidationResult validationResult;

    @NonNull
    public java.time.Instant createdAt;

    public java.time.Instant updatedAt;

    public java.time.Instant publishedAt;

    @NonNull
    public Long entityVersion;

    /**
     * Computed field - node count
     */
    public int nodeCount() {
        return logic != null && logic.nodes != null ? logic.nodes.size() : 0;
    }

    /**
     * Computed field - connection count
     */
    public int connectionCount() {
        return logic != null && logic.connections != null ? logic.connections.size() : 0;
    }

    /**
     * Computed field - is valid
     */
    public boolean isValid() {
        return validationResult != null && validationResult.isValid();
    }

    /**
     * Computed field - can publish
     */
    public boolean canPublish() {
        return status.equals("VALID") && validationResult != null && validationResult.isValid();
    }

    public static WorkflowQL from(Workflow workflow) {
        WorkflowQL ql = new WorkflowQL();
        ql.id = workflow.id.toString();
        ql.name = workflow.name;
        ql.description = workflow.description;
        ql.version = workflow.version;
        ql.status = workflow.status.name();
        ql.logic = workflow.logic;
        ql.ui = workflow.ui;
        ql.runtime = workflow.runtime;
        ql.validationResult = workflow.validationResult;
        ql.createdAt = workflow.createdAt;
        ql.updatedAt = workflow.updatedAt;
        ql.publishedAt = workflow.publishedAt;
        ql.entityVersion = workflow.entityVersion;
        return ql;
    }
}
