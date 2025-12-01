
public interface DesignerService {
    Workflow createWorkflow(CreateWorkflowRequest request);
    Workflow updateWorkflow(UUID workflowId, UpdateWorkflowRequest request);
    void deleteWorkflow(UUID workflowId);
    Workflow getWorkflow(UUID workflowId);
    List<Workflow> listWorkflows(WorkflowQuery query);
    ValidationResult validateWorkflow(UUID workflowId);
    Workflow publishWorkflow(UUID workflowId);
}
