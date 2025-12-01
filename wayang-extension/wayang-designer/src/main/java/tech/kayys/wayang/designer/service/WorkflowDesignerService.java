
@ApplicationScoped
public class WorkflowDesignerService implements DesignerService {
    @Inject WorkflowRepository workflowRepository;
    @Inject WorkflowValidator workflowValidator;
    @Inject SchemaRegistry schemaRegistry;
    @Inject VersioningService versioningService;
    @Inject AutosaveManager autosaveManager;
    @Inject LockManager lockManager;
    
    @Override
    @Transactional
    public Workflow createWorkflow(CreateWorkflowRequest request) {
        // Validate request
        validateCreateRequest(request);
        
        // Create workflow
        Workflow workflow = Workflow.builder()
            .id(UUID.randomUUID())
            .name(request.getName())
            .version("1.0.0")
            .definition(WorkflowDefinition.builder()
                .nodes(new ArrayList<>())
                .edges(new ArrayList<>())
                .globalVariables(request.getGlobalVariables())
                .build())
            .status(WorkflowStatus.DRAFT)
            .tenantId(request.getTenantId())
            .createdBy(request.getUserId())
            .build();
        
        // Persist
        workflow = workflowRepository.save(workflow);
        
        // Enable autosave
        autosaveManager.enableAutosave(workflow.getId());
        
        return workflow;
    }
    
    @Override
    @Transactional
    public Workflow updateWorkflow(UUID workflowId, UpdateWorkflowRequest request) {
        // Acquire lock
        Lock lock = lockManager.acquireLock(workflowId, request.getUserId());
        
        try {
            // Get workflow
            Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
            
            // Check if editable
            if (workflow.getStatus() == WorkflowStatus.PUBLISHED) {
                throw new WorkflowNotEditableException(workflowId);
            }
            
            // Apply updates
            if (request.getName() != null) {
                workflow.setName(request.getName());
            }
            
            if (request.getDefinition() != null) {
                workflow.setDefinition(request.getDefinition());
            }
            
            // Validate
            ValidationResult validation = workflowValidator.validate(workflow);
            if (!validation.isValid()) {
                workflow.setStatus(WorkflowStatus.INVALID);
            } else {
                workflow.setStatus(WorkflowStatus.VALID);
            }
            
            // Save
            workflow = workflowRepository.save(workflow);
            
            // Autosave
            autosaveManager.save(workflow);
            
            return workflow;
            
        } finally {
            lock.release();
        }
    }
    
    @Override
    @Transactional
    public Workflow publishWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        // Validate
        ValidationResult validation = workflowValidator.validate(workflow);
        if (!validation.isValid()) {
            throw new InvalidWorkflowException(validation.getErrors());
        }
        
        // Create version
        WorkflowVersion version = versioningService.createVersion(workflow);
        
        // Update status
        workflow.setStatus(WorkflowStatus.PUBLISHED);
        workflow.setVersion(version.getVersion());
        
        return workflowRepository.save(workflow);
    }
}
