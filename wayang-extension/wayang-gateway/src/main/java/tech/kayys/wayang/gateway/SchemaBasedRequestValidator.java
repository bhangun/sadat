
// Request Validator
@ApplicationScoped
public class SchemaBasedRequestValidator {
    @Inject JsonSchemaValidator jsonSchemaValidator;
    
    public ValidationResult validate(Object request) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Convert to JSON
        JsonNode jsonNode = objectMapper.valueToTree(request);
        
        // Get schema for request type
        JsonSchema schema = getSchemaForType(request.getClass());
        
        // Validate
        Set<ValidationMessage> messages = jsonSchemaValidator.validate(jsonNode, schema);
        
        for (ValidationMessage message : messages) {
            errors.add(new ValidationError(
                message.getType(),
                message.getMessage()
            ));
        }
        
        return ValidationResult.builder()
            .valid(errors.isEmpty())
            .errors(errors)
            .build();
    }
}
