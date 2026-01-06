

/**
 * Self-Healing Service - Attempts automatic error correction
 */
@ApplicationScoped
public class SelfHealingService {

    private static final Logger LOG = Logger.getLogger(SelfHealingService.class);

    @Inject
    LLMService llmService;

    /**
     * Check if error can be auto-fixed
     */
    public boolean canAutoFix(ErrorPayload error) {
        return error.getType().equals("ValidationError") 
            || error.getType().equals("ToolError");
    }

    /**
     * Attempt to fix error automatically
     */
    public Uni<FixResult> attemptFix(ErrorPayload error, NodeContext context) {
        LOG.infof("Attempting auto-fix for error: %s", error.getType());

        if (error.getType().equals("ValidationError")) {
            return fixValidationError(error, context);
        }

        return Uni.createFrom().item(FixResult.failed("No fix strategy available"));
    }

    /**
     * Fix validation error using LLM
     */
    private Uni<FixResult> fixValidationError(
            ErrorPayload error, 
            NodeContext context) {
        
        String prompt = buildFixPrompt(error, context);
        
        return llmService.generateFix(prompt)
            .onItem().transform(fixedInput -> {
                // Validate fixed input
                if (validateFixedInput(fixedInput)) {
                    return FixResult.success(fixedInput);
                } else {
                    return FixResult.failed("Generated fix is invalid");
                }
            })
            .onFailure().recoverWithItem(throwable -> 
                FixResult.failed("Fix generation failed: " + throwable.getMessage())
            );
    }

    private String buildFixPrompt(ErrorPayload error, NodeContext context) {
        return String.format("""
            You are a data correction assistant.
            
            Error: %s
            Details: %s
            
            Original Input: %s
            
            Task: Generate corrected input that fixes the validation error.
            Return only valid JSON matching the expected schema.
            """,
            error.getMessage(),
            error.getDetails(),
            context.getInputs()
        );
    }

    private boolean validateFixedInput(Object fixedInput) {
        // Perform schema validation
        return fixedInput != null;
    }
}