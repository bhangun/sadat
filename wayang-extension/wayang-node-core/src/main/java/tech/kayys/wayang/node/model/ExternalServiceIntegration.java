package tech.kayys.wayang.node.core.integration;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Base interface for external service integrations.
 * 
 * Provides common patterns for authentication, rate limiting,
 * and error handling across all external services.
 */
public interface ExternalServiceIntegration {
    
    /**
     * Get the service name
     */
    String getServiceName();
    
    /**
     * Check if the service is available
     */
    CompletionStage<Boolean> healthCheck();
    
    /**
     * Authenticate with the service
     */
    CompletionStage<AuthenticationResult> authenticate(Map<String, String> credentials);
    
    /**
     * Get current rate limit status
     */
    RateLimitStatus getRateLimitStatus();
    
    /**
     * Test connection with credentials
     */
    CompletionStage<ConnectionTestResult> testConnection(Map<String, String> credentials);
}