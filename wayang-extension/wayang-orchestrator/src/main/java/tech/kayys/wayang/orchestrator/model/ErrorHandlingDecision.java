
record ErrorHandlingDecision(
    ErrorAction action,
    Integer maxRetries,
    Long delayMs
) {
    enum ErrorAction {
        RETRY, AUTO_FIX, HUMAN_REVIEW, ABORT
    }
    
    static ErrorHandlingDecision retry(int maxRetries) {
        return new ErrorHandlingDecision(ErrorAction.RETRY, maxRetries, 1000L);
    }
    
    static ErrorHandlingDecision autoFix() {
        return new ErrorHandlingDecision(ErrorAction.AUTO_FIX, null, null);
    }
    
    static ErrorHandlingDecision humanReview() {
        return new ErrorHandlingDecision(ErrorAction.HUMAN_REVIEW, null, null);
    }
    
    static ErrorHandlingDecision abort() {
        return new ErrorHandlingDecision(ErrorAction.ABORT, null, null);
    }
}