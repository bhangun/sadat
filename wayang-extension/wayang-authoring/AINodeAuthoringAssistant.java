
@ApplicationScoped
public class AINodeAuthoringAssistant implements NodeAuthoringAssistant {
    @Inject LLMRuntime llmRuntime;
    @Inject SchemaRegistry schemaRegistry;
    @Inject CodeGenerator codeGenerator;
    @Inject NodeValidator nodeValidator;
    
    @Override
    public NodeTemplate generateTemplate(NodeGenerationRequest request) {
        // Build prompt for template generation
        String prompt = buildTemplatePrompt(request);
        
        LLMRequest llmRequest = LLMRequest.builder()
            .prompt(Prompt.of(prompt))
            .type(LLMRequestType.COMPLETION)
            .maxTokens(2000)
            .build();
        
        LLMResponse response = llmRuntime.complete(llmRequest).join();
        
        // Parse response
        NodeTemplate template = parseTemplate(response.getContent());
        
        return template;
    }
    
    @Override
    public NodeDescriptor generateSchema(SchemaGenerationRequest request) {
        // Build prompt for schema generation
        String prompt = buildSchemaPrompt(request);
        
        LLMRequest llmRequest = LLMRequest.builder()
            .prompt(Prompt.of(prompt))
            .type(LLMRequestType.COMPLETION)
            .maxTokens(1500)
            .build();
        
        LLMResponse response = llmRuntime.complete(llmRequest).join();
        
        // Parse JSON schema
        NodeDescriptor descriptor = parseNodeDescriptor(response.getContent());
        
        // Validate schema
        ValidationResult validation = schemaRegistry.validateSchema(descriptor);
        if (!validation.isValid()) {
            throw new InvalidSchemaException(validation.getErrors());
        }
        
        return descriptor;
    }
    
    @Override
    public String generateCode(CodeGenerationRequest request) {
        // Build prompt for code generation
        String prompt = buildCodePrompt(request);
        
        LLMRequest llmRequest = LLMRequest.builder()
            .prompt(Prompt.of(prompt))
            .type(LLMRequestType.COMPLETION)
            .maxTokens(3000)
            .build();
        
        LLMResponse response = llmRuntime.complete(llmRequest).join();
        
        // Extract code
        String code = extractCode(response.getContent());
        
        // Validate syntax
        codeGenerator.validateSyntax(code, request.getLanguage());
        
        return code;
    }
    
    private String buildTemplatePrompt(NodeGenerationRequest request) {
        return String.format("""
            Generate a node template for the following requirements:
            
            Purpose: %s
            Category: %s
            Input Schema: %s
            Output Schema: %s
            Required Capabilities: %s
            
            Provide a JSON template following the NodeDescriptor schema.
            Include all necessary fields: id, name, version, inputs, outputs, properties, capabilities.
            """,
            request.getPurpose(),
            request.getCategory(),
            request.getInputSchema(),
            request.getOutputSchema(),
            request.getCapabilities()
        );
    }
    
    private String buildCodePrompt(CodeGenerationRequest request) {
        return String.format("""
            Generate %s code for a node with the following specification:
            
            Node Type: %s
            Description: %s
            Inputs: %s
            Outputs: %s
            Logic: %s
            
            The code should:
            1. Implement the Node interface
            2. Handle errors gracefully
            3. Include proper logging
            4. Follow best practices
            5. Be production-ready
            
            Generate only the code, no explanations.
            """,
            request.getLanguage(),
            request.getNodeType(),
            request.getDescription(),
            request.getInputs(),
            request.getOutputs(),
            request.getLogicDescription()
        );
    }
}
