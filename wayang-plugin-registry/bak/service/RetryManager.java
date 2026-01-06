

/**
 * Retry Manager - Handles retry logic with exponential backoff
 */
@ApplicationScoped
public class RetryManager {

    private static final Logger LOG = Logger.getLogger(RetryManager.class);

    /**
     * Calculate backoff delay based on retry policy
     */
    public long calculateBackoff(ErrorPayload error) {
        // Get retry policy from error or use default
        int attempt = error.getAttempt();
        long initialDelay = 500; // ms
        
        // Exponential backoff with jitter
        long delay = (long) (initialDelay * Math.pow(2, attempt));
        
        // Add jitter (±25%)
        double jitter = 0.75 + (Math.random() * 0.5);
        delay = (long) (delay * jitter);
        
        // Cap at max delay
        return Math.min(delay, 30000);
    }

    /**
     * Schedule retry execution
     */
    public Uni<Void> scheduleRetry(ErrorPayload error, long delayMs) {
        LOG.infof("Scheduling retry in %d ms for node: %s", 
            delayMs, error.getOriginNode());
        
        return Uni.createFrom().item(() -> null)
            .onItem().delayIt().by(Duration.ofMillis(delayMs));
    }
}