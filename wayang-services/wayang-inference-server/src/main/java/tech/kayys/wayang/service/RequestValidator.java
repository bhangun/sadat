package tech.kayys.wayang.service;

import java.util.ArrayList;
import java.util.List;

import tech.kayys.wayang.model.ChatRequest;

public class RequestValidator {
    
    public ValidationResult validate(ChatRequest request) {
        List<String> errors = new ArrayList<>();
        
        // Validate messages
        if (request.messages() == null || request.messages().isEmpty()) {
            errors.add("Messages cannot be empty");
        } else {
            for (int i = 0; i < request.messages().size(); i++) {
                var msg = request.messages().get(i);
                
                if (msg.role() == null || msg.role().isBlank()) {
                    errors.add("Message " + i + ": role is required");
                }
                
                if (!List.of("system", "user", "assistant").contains(msg.role())) {
                    errors.add("Message " + i + ": invalid role '" + msg.role() + "'");
                }
                
                if (msg.content() == null) {
                    errors.add("Message " + i + ": content is required");
                }
                
                if (msg.content() != null && msg.content().length() > 100000) {
                    errors.add("Message " + i + ": content too long (max 100k chars)");
                }
            }
        }
        
        // Validate parameters
        if (request.maxTokens() != null) {
            if (request.maxTokens() < 1 || request.maxTokens() > 8192) {
                errors.add("max_tokens must be between 1 and 8192");
            }
        }
        
        if (request.temperature() != null) {
            if (request.temperature() < 0 || request.temperature() > 2) {
                errors.add("temperature must be between 0 and 2");
            }
        }
        
        if (request.topP() != null) {
            if (request.topP() < 0 || request.topP() > 1) {
                errors.add("top_p must be between 0 and 1");
            }
        }
        
        if (request.topK() != null) {
            if (request.topK() < 0 || request.topK() > 1000) {
                errors.add("top_k must be between 0 and 1000");
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    public record ValidationResult(
        boolean valid,
        List<String> errors
    ) {}
}
