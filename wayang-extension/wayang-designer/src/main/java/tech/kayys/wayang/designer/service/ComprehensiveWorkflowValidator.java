
// Workflow Validator
@ApplicationScoped
public class ComprehensiveWorkflowValidator {
    @Inject SchemaRegistry schemaRegistry;
    @Inject PolicyEngine policyEngine;
    @Inject Linter linter;
    
    public ValidationResult validate(Workflow workflow) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        // Structural validation
        errors.addAll(validateStructure(workflow));
        
        // Node validation
        errors.addAll(validateNodes(workflow));
        
        // Edge validation
        errors.addAll(validateEdges(workflow));
        
        // Type compatibility validation
        errors.addAll(validateTypeCompatibility(workflow));
        
        // Policy validation
        errors.addAll(validatePolicies(workflow));
        
        // Lint workflow
        warnings.addAll(linter.lint(workflow));
        
        return ValidationResult.builder()
            .valid(errors.isEmpty())
            .errors(errors)
            .warnings(warnings)
            .build();
    }
    
    private List<ValidationError> validateNodes(Workflow workflow) {
        List<ValidationError> errors = new ArrayList<>();
        
        for (NodeInstance node : workflow.getDefinition().getNodes()) {
            // Get node descriptor
            Optional<NodeDescriptor> descriptorOpt = 
                schemaRegistry.getDescriptor(node.getDescriptorId());
            
            if (descriptorOpt.isEmpty()) {
                errors.add(new ValidationError(
                    "UNKNOWN_NODE_TYPE",
                    "Unknown node type: " + node.getDescriptorId(),
                    node.getNodeId()
                ));
                continue;
            }
            
            NodeDescriptor descriptor = descriptorOpt.get();
            
            // Validate configuration
            errors.addAll(validateNodeConfiguration(node, descriptor));
            
            // Validate input bindings
            errors.addAll(validateInputBindings(node, descriptor));
        }
        
        return errors;
    }
    
    private List<ValidationError> validateEdges(Workflow workflow) {
        List

        <ValidationError> errors = new ArrayList<>();
        
        Map<String, NodeInstance> nodeMap = workflow.getDefinition().getNodes()
            .stream()
            .collect(Collectors.toMap(NodeInstance::getNodeId, n -> n));
        
        for (Edge edge : workflow.getDefinition().getEdges()) {
            // Check source node exists
            if (!nodeMap.containsKey(edge.getSourceNodeId())) {
                errors.add(new ValidationError(
                    "INVALID_EDGE_SOURCE",
                    "Source node not found: " + edge.getSourceNodeId(),
                    edge.getId()
                ));
                continue;
            }
            
            // Check target node exists
            if (!nodeMap.containsKey(edge.getTargetNodeId())) {
                errors.add(new ValidationError(
                    "INVALID_EDGE_TARGET",
                    "Target node not found: " + edge.getTargetNodeId(),
                    edge.getId()
                ));
                continue;
            }
            
            // Validate port compatibility
            NodeInstance source = nodeMap.get(edge.getSourceNodeId());
            NodeInstance target = nodeMap.get(edge.getTargetNodeId());
            
            errors.addAll(validatePortCompatibility(edge, source, target));
        }
        
        return errors;
    }
}