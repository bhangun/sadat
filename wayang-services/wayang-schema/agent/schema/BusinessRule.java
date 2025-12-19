package tech.kayys.agent.schema;

/**
 * Business rule expressed in CEL (Common Expression Language) or similar.
 * Example: "request.amount < 10000 && user.country == 'US'"
 */
public record BusinessRule(
    String expression,
    String description
) {
    public BusinessRule {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("expression must not be blank");
        }
        description = (description == null) ? "" : description;
    }
}