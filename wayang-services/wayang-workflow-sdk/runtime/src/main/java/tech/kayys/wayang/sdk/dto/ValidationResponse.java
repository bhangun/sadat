package tech.kayys.wayang.sdk.dto;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Validation response
 */
public record ValidationResponse(
    boolean valid,
    List<ValidationError> errors,
    List<ValidationWarning> warnings
) {
    public record ValidationError(String code, String message, String location) {}
    public record ValidationWarning(String code, String message, String location) {}
}

