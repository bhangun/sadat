package tech.kayys.wayang.service;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.schema.CreateWorkflowInput;
import tech.kayys.wayang.schema.NodeInput;
import tech.kayys.wayang.schema.UpdateWorkflowInput;

@ApplicationScoped
public class WorkflowValidator {
    public void validateCreate(CreateWorkflowInput input) {
    }

    public void validateUpdate(Workflow workflow, UpdateWorkflowInput input) {
    }

    public void validateNode(Workflow workflow, NodeInput input) {
    }
}
