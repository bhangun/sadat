
/**
 * Validator Node - Validate data against schemas
 * Supports JSON Schema, CEL, and custom validators
 */
@ApplicationScoped
@NodeType("builtin.validator")
public class ValidatorNode extends AbstractNode {
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var data = context.getInput("data");
        var schema = config.getObject("schema", Map.class);
        var celRules = config.getList("celRules", String.class);
        
        return Uni.createFrom().item(() -> {
            var errors = new ArrayList<String>();
            
            // JSON Schema validation
            if (schema != null) {
                var schemaErrors = SchemaValidator.validate(data, schema);
                errors.addAll(schemaErrors);
            }
            
            // CEL rules validation
            if (celRules != null) {
                var celErrors = validateCelRules(data, celRules);
                errors.addAll(celErrors);
            }
            
            var isValid = errors.isEmpty();
            
            return ExecutionResult.success(Map.of(
                "valid", isValid,
                "errors", errors,
                "data", data
            ));
        });
    }
    
    private List<String> validateCelRules(Object data, List<String> rules) {
        var errors = new ArrayList<String>();
        var celEngine = CelEngine.create();
        
        for (var rule : rules) {
            try {
                var result = celEngine.evaluate(rule, Map.of("data", data));
                if (!Boolean.TRUE.equals(result)) {
                    errors.add("Rule failed: " + rule);
                }
            } catch (Exception e) {
                errors.add("Rule error: " + e.getMessage());
            }
        }
        
        return errors;
    }
}
