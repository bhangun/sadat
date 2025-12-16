package tech.kayys.wayang.graphql;

import org.eclipse.microprofile.graphql.Type;
import tech.kayys.wayang.model.ValidationResult;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ValidationResultQL - GraphQL type for ValidationResult
 */
@Type("ValidationResult")
public class ValidationResultQL {
    public boolean valid;
    public Instant validatedAt;
    public String validatorVersion;
    public List<ValidationErrorQL> errors;
    public List<ValidationWarningQL> warnings;

    @Type("ValidationError")
    public static class ValidationErrorQL {
        public String code;
        public String message;
        public String nodeId;
        public String path;
        public String severity;
    }

    @Type("ValidationWarning")
    public static class ValidationWarningQL {
        public String message;
        public String nodeId;
        public String suggestion;
    }

    public static ValidationResultQL from(ValidationResult result) {
        if (result == null) {
            return null;
        }

        ValidationResultQL ql = new ValidationResultQL();
        ql.valid = result.valid;
        ql.validatedAt = result.validatedAt;
        ql.validatorVersion = result.validatorVersion;

        if (result.errors != null) {
            ql.errors = result.errors.stream()
                    .map(e -> {
                        ValidationErrorQL eq = new ValidationErrorQL();
                        eq.code = e.code;
                        eq.message = e.message;
                        eq.nodeId = e.nodeId;
                        eq.path = e.path;
                        eq.severity = e.severity != null ? e.severity.name() : null;
                        return eq;
                    })
                    .collect(Collectors.toList());
        }

        if (result.warnings != null) {
            ql.warnings = result.warnings.stream()
                    .map(w -> {
                        ValidationWarningQL wq = new ValidationWarningQL();
                        wq.message = w.message;
                        wq.nodeId = w.nodeId;
                        wq.suggestion = w.suggestion;
                        return wq;
                    })
                    .collect(Collectors.toList());
        }

        return ql;
    }
}
