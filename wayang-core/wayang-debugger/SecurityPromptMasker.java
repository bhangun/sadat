
// Prompt Masker for Security
@ApplicationScoped
public class SecurityPromptMasker {
    @Inject PIIDetector piiDetector;
    
    public TraceEntry maskSensitiveData(TraceEntry entry) {
        String message = entry.getMessage();
        
        // Detect and mask PII
        PIIResult piiResult = piiDetector.detect(message);
        if (piiResult.hasPII()) {
            for (PIIMatch match : piiResult.getMatches()) {
                message = message.replace(
                    match.getText(),
                    "[" + match.getType() + "_REDACTED]"
                );
            }
        }
        
        // Mask API keys and tokens
        message = maskApiKeys(message);
        
        // Mask passwords
        message = maskPasswords(message);
        
        return entry.withMessage(message);
    }
    
    public ReasoningStep maskReasoningStep(ReasoningStep step) {
        String thought = step.getThought();
        
        // Apply masking
        thought = maskSensitiveData(thought);
        
        return step.withThought(thought);
    }
    
    private String maskApiKeys(String text) {
        // Pattern for common API key formats
        return text.replaceAll(
            "\\b[A-Za-z0-9]{32,}\\b",
            "[API_KEY_REDACTED]"
        );
    }
    
    private String maskPasswords(String text) {
        // Pattern for password-like strings
        return text.replaceAll(
            "(?i)password[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']",
            "password: [PASSWORD_REDACTED]"
        );
    }
    
    private String maskSensitiveData(String text) {
        text = maskApiKeys(text);
        text = maskPasswords(text);
        
        // Mask email addresses
        text = text.replaceAll(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b",
            "[EMAIL_REDACTED]"
        );
        
        return text;
    }
}
