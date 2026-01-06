package tech.kayys.wayang.guardrails.dto;

public record ValidationRule(
        String name,
        String description,
        java.util.function.Predicate<String> validator) {
    public boolean validate(String output) {
        return validator.test(output);
    }
}