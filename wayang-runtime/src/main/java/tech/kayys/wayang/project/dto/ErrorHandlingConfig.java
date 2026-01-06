package tech.kayys.wayang.project.dto;

import java.util.List;

/**
 * Error Handling Configuration
 */
public class ErrorHandlingConfig {
    public RetryStrategy retryStrategy;
    public DeadLetterConfig deadLetter;
    public List<ErrorHandler> handlers;
}
