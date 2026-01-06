package tech.kayys.wayang.guardrails.dto;

import java.util.Map;

public record GuardrailCheckResult(
                boolean passed,
                String checkType,
                GuardrailSeverity severity,
                GuardrailAction action,
                String message,
                Map<String, Object> details) {
        public static GuardrailCheckResult passed(String checkType) {
                return new GuardrailCheckResult(
                                true, checkType, null, GuardrailAction.ALLOW, null, Map.of());
        }

        public static GuardrailCheckResult violation(
                        String checkType,
                        GuardrailSeverity severity,
                        GuardrailAction action,
                        String message,
                        Map<String, Object> details) {
                return new GuardrailCheckResult(
                                false, checkType, severity, action, message, details);
        }

        public static GuardrailCheckResult passedWithModification(
                        String checkType,
                        String message,
                        Map<String, Object> details) {
                return new GuardrailCheckResult(
                                true, checkType, GuardrailSeverity.INFO,
                                GuardrailAction.ALLOW, message, details);
        }
}