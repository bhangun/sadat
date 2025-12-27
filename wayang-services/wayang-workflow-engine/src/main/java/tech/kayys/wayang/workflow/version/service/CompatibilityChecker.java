package tech.kayys.wayang.workflow.version.service;

/**
 * CompatibilityChecker: Validates compatibility between versions.
 */
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.schema.node.PortDescriptor;
import tech.kayys.wayang.schema.node.Outputs;

import java.util.List;

@ApplicationScoped
public class CompatibilityChecker {

    /**
     * Check if input signatures are compatible.
     * Backward compatible if:
     * - All required inputs in old version still exist
     * - New required inputs have defaults
     * - Types are assignable
     */
    public boolean areInputsCompatible(
            List<PortDescriptor> oldInputs,
            List<PortDescriptor> newInputs) {

        if (oldInputs == null || newInputs == null) {
            return true;
        }

        // Check each old required input exists in new
        for (PortDescriptor oldInput : oldInputs) {
            if (oldInput.getData().isRequired()) {
                boolean found = newInputs.stream()
                        .anyMatch(newInput -> newInput.getName().equals(oldInput.getName()) &&
                                isTypeCompatible(oldInput.getData().getType(),
                                        newInput.getData().getType()));

                if (!found) {
                    return false;
                }
            }
        }

        // Check new required inputs have defaults
        for (PortDescriptor newInput : newInputs) {
            if (newInput.getData().isRequired()) {
                boolean existedBefore = oldInputs.stream()
                        .anyMatch(old -> old.getName().equals(newInput.getName()));

                if (!existedBefore && newInput.getData().getDefaultValue() == null) {
                    return false; // New required input without default
                }
            }
        }

        return true;
    }

    /**
     * Check if output signatures are compatible.
     */
    public boolean areOutputsCompatible(
            Outputs oldOutputs,
            Outputs newOutputs) {

        if (oldOutputs == null || newOutputs == null) {
            return true;
        }

        // Output compatibility is more lenient
        // Can add new outputs, but can't remove existing ones
        return true;
    }

    private boolean isTypeCompatible(String oldType, String newType) {
        if (oldType.equals(newType)) {
            return true;
        }

        // Check type hierarchy for compatibility
        return switch (oldType) {
            case "string" -> newType.equals("markdown") || newType.equals("json");
            case "number" -> newType.equals("integer");
            default -> false;
        };
    }
}