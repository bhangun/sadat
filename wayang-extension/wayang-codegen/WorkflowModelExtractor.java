
// Model Extractor
@ApplicationScoped
public class WorkflowModelExtractor {
    @Inject WorkflowRepository workflowRepository;
    @Inject SchemaRegistry schemaRegistry;
    
    public WorkflowModel extract(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        List<NodeModel> nodes = workflow.getDefinition().getNodes().stream()
            .map(this::toNodeModel)
            .collect(Collectors.toList());
        
        return WorkflowModel.builder()
            .id(workflow.getId())
            .name(workflow.getName())
            .version(workflow.getVersion())
            .nodes(nodes)
            .edges(workflow.getDefinition().getEdges())
            .build();
    }
    
    private NodeModel toNodeModel(NodeInstance instance) {
        NodeDescriptor descriptor = schemaRegistry
            .getDescriptor(instance.getDescriptorId())
            .orElseThrow();
        
        return NodeModel.builder()
            .nodeId(instance.getNodeId())
            .className(toClassName(instance.getNodeType()))
            .descriptor(descriptor)
            .configuration(instance.getConfig())
            .build();
    }
}
