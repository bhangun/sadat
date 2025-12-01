/**
 * Validation error
 */
record ValidationError(
    String field,
    String code,
    String message,
    Object rejectedValue
) {}
