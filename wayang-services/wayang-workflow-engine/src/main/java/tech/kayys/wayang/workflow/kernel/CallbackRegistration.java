package tech.kayys.wayang.workflow.kernel;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 🔒 Callback registration for waiting workflows
 */
public interface CallbackRegistration {

    String getCallbackId();

    String getCallbackUrl();

    Duration getTimeout();

    Instant getExpiresAt();

    Map<String, String> getAuthHeaders();

    /**
     * Validate callback token
     */
    boolean validateToken(String token);
}
