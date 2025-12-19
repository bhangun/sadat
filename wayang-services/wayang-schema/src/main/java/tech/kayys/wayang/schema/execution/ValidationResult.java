package tech.kayys.wayang.schema.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public void addError(String error) {
        errors.add(error);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public void failure(String name, List<String> errors) {
        errors.addAll(errors);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!errors.isEmpty()) {
            sb.append("Errors:\n");
            for (String error : errors) {
                sb.append("  - ").append(error).append("\n");
            }
        }
        if (!warnings.isEmpty()) {
            sb.append("Warnings:\n");
            for (String warning : warnings) {
                sb.append("  - ").append(warning).append("\n");
            }
        }
        return sb.toString();
    }
}
